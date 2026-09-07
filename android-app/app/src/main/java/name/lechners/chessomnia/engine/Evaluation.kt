package name.lechners.chessomnia.engine

import name.lechners.chessomnia.rules.PieceType
import name.lechners.chessomnia.rules.Position
import name.lechners.chessomnia.rules.Side

/**
 * How good a position is, in centipawns, from the point of view of the side to move.
 *
 * The engine never shows this number. It exists only so the search can compare two
 * positions; there is no evaluation bar and no "you are losing" hint anywhere in the UI.
 *
 * Two scores are accumulated side by side, one for the middlegame and one for the
 * endgame, and blended by how much material is still on the board (see [phaseOf]).
 * Without that blend a king that belongs in the corner during the middlegame would still
 * be told to hide there in a king-and-pawn ending, where it has to march instead.
 */
object Evaluation {

    const val PAWN = 100
    const val KNIGHT = 320
    const val BISHOP = 330
    const val ROOK = 500
    const val QUEEN = 900

    /** A draw. Its own constant so the search reads clearly. */
    const val DRAW = 0

    fun valueOf(type: PieceType): Int = when (type) {
        PieceType.PAWN -> PAWN
        PieceType.KNIGHT -> KNIGHT
        PieceType.BISHOP -> BISHOP
        PieceType.ROOK -> ROOK
        PieceType.QUEEN -> QUEEN
        PieceType.KING -> 0
    }

    // ── Piece-square tables ─────────────────────────────────────────────────────
    //
    // Written the way a board is printed: the first row is rank 8, the last is rank 1,
    // and files run a..h from left to right. That makes them readable, at the price of
    // one mirror step in `pstIndex`.
    //
    // The values are chosen for this app rather than copied from anywhere: they encode
    // the handful of principles a beginner is taught - knights belong in the centre,
    // rooks on the seventh, the king behind its pawns while the queens are on - and
    // nothing more. A stronger table would not make the opponent more pleasant to play.

    private val PAWN_MG = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0,
        60, 60, 60, 60, 60, 60, 60, 60,
        20, 20, 30, 40, 40, 30, 20, 20,
        8, 8, 15, 28, 28, 15, 8, 8,
        2, 2, 6, 22, 22, 6, 2, 2,
        2, -2, -6, 4, 4, -6, -2, 2,
        4, 8, 8, -18, -18, 8, 8, 4,
        0, 0, 0, 0, 0, 0, 0, 0,
    )

    /** In the endgame a pawn's only virtue is how far it has come. */
    private val PAWN_EG = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0,
        90, 90, 90, 90, 90, 90, 90, 90,
        55, 55, 55, 55, 55, 55, 55, 55,
        32, 32, 32, 32, 32, 32, 32, 32,
        18, 18, 18, 18, 18, 18, 18, 18,
        8, 8, 8, 8, 8, 8, 8, 8,
        4, 4, 4, 4, 4, 4, 4, 4,
        0, 0, 0, 0, 0, 0, 0, 0,
    )

    private val KNIGHT_PST = intArrayOf(
        -60, -40, -30, -30, -30, -30, -40, -60,
        -40, -20, 0, 4, 4, 0, -20, -40,
        -30, 4, 16, 20, 20, 16, 4, -30,
        -30, 2, 20, 26, 26, 20, 2, -30,
        -30, 0, 20, 26, 26, 20, 0, -30,
        -30, 4, 16, 20, 20, 16, 4, -30,
        -40, -20, 0, 2, 2, 0, -20, -40,
        -60, -40, -30, -30, -30, -30, -40, -60,
    )

    private val BISHOP_PST = intArrayOf(
        -24, -12, -12, -12, -12, -12, -12, -24,
        -12, 0, 0, 0, 0, 0, 0, -12,
        -12, 0, 8, 12, 12, 8, 0, -12,
        -12, 6, 6, 12, 12, 6, 6, -12,
        -12, 0, 12, 12, 12, 12, 0, -12,
        -12, 12, 12, 12, 12, 12, 12, -12,
        -12, 6, 0, 0, 0, 0, 6, -12,
        -24, -12, -12, -12, -12, -12, -12, -24,
    )

    private val ROOK_PST = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0,
        10, 14, 14, 14, 14, 14, 14, 10,
        -4, 0, 0, 0, 0, 0, 0, -4,
        -4, 0, 0, 0, 0, 0, 0, -4,
        -4, 0, 0, 0, 0, 0, 0, -4,
        -4, 0, 0, 0, 0, 0, 0, -4,
        -4, 0, 0, 0, 0, 0, 0, -4,
        0, 0, 4, 8, 8, 4, 0, 0,
    )

    private val QUEEN_PST = intArrayOf(
        -20, -10, -10, -4, -4, -10, -10, -20,
        -10, 0, 0, 0, 0, 0, 0, -10,
        -10, 0, 4, 4, 4, 4, 0, -10,
        -4, 0, 4, 4, 4, 4, 0, -4,
        0, 0, 4, 4, 4, 4, 0, -4,
        -10, 4, 4, 4, 4, 4, 0, -10,
        -10, 0, 4, 0, 0, 0, 0, -10,
        -20, -10, -10, -4, -4, -10, -10, -20,
    )

    /** Middlegame: stay home, and castling is worth something. */
    private val KING_MG = intArrayOf(
        -40, -50, -50, -60, -60, -50, -50, -40,
        -40, -50, -50, -60, -60, -50, -50, -40,
        -40, -50, -50, -60, -60, -50, -50, -40,
        -40, -50, -50, -60, -60, -50, -50, -40,
        -30, -40, -40, -50, -50, -40, -40, -30,
        -20, -30, -30, -40, -40, -30, -30, -20,
        16, 16, -8, -12, -12, -8, 16, 16,
        16, 24, 8, 0, 0, 8, 28, 16,
    )

    /** Endgame: the king is a piece again and belongs in the middle. */
    private val KING_EG = intArrayOf(
        -50, -30, -20, -20, -20, -20, -30, -50,
        -30, -15, 0, 0, 0, 0, -15, -30,
        -20, 0, 20, 25, 25, 20, 0, -20,
        -20, 5, 25, 32, 32, 25, 5, -20,
        -20, 0, 25, 32, 32, 25, 0, -20,
        -20, 5, 20, 25, 25, 20, 5, -20,
        -30, -15, 0, 5, 5, 0, -15, -30,
        -50, -30, -20, -20, -20, -20, -30, -50,
    )

    /** Bonus for a passed pawn, by how far it has advanced (index = rank from own side). */
    private val PASSED_MG = intArrayOf(0, 4, 8, 16, 32, 60, 90, 0)
    private val PASSED_EG = intArrayOf(0, 10, 20, 36, 60, 100, 150, 0)

    private const val BISHOP_PAIR_MG = 30
    private const val BISHOP_PAIR_EG = 45
    private const val DOUBLED_PAWN = -12
    private const val ISOLATED_PAWN = -15
    private const val ROOK_OPEN_FILE = 15
    private const val ROOK_SEMI_OPEN_FILE = 8
    private const val DRIVE_TO_EDGE = 16
    private const val DRIVE_KINGS_TOGETHER = 10

    // Phase weights. Queens dominate: while they are on, it is a middlegame.
    private const val PHASE_MAX = 24

    /**
     * The score in centipawns, positive when the side to move stands better.
     *
     * ⚠️ Relative to the side to move, not to White. The search is a negamax, so every
     * value it compares has to be from the mover's point of view; an absolute score
     * would need a sign flip at every single node and would eventually be forgotten at
     * one of them.
     */
    fun evaluate(pos: Position): Int {
        val board = pos.board

        var mg = 0
        var eg = 0
        var phase = 0

        // Ranks occupied by pawns, one bitmask per file per side. Exact, and it makes
        // the doubled/isolated/passed tests plain bit arithmetic.
        val pawnMask = IntArray(16)
        val bishops = IntArray(2)
        val kingSquare = IntArray(2) { -1 }
        val material = IntArray(2)

        for (rank in 0..7) {
            for (file in 0..7) {
                val sq = rank * 16 + file
                val piece = board[sq] ?: continue
                val side = piece.side
                val sign = if (side == Side.WHITE) 1 else -1
                val pst = pstIndex(side, file, rank)

                material[side.ordinal] += valueOf(piece.type)

                when (piece.type) {
                    PieceType.PAWN -> {
                        pawnMask[side.ordinal * 8 + file] =
                            pawnMask[side.ordinal * 8 + file] or (1 shl rank)
                        mg += sign * (PAWN + PAWN_MG[pst])
                        eg += sign * (PAWN + PAWN_EG[pst])
                    }
                    PieceType.KNIGHT -> {
                        phase += 1
                        mg += sign * (KNIGHT + KNIGHT_PST[pst])
                        eg += sign * (KNIGHT + KNIGHT_PST[pst])
                    }
                    PieceType.BISHOP -> {
                        phase += 1
                        bishops[side.ordinal]++
                        mg += sign * (BISHOP + BISHOP_PST[pst])
                        eg += sign * (BISHOP + BISHOP_PST[pst])
                    }
                    PieceType.ROOK -> {
                        phase += 2
                        mg += sign * (ROOK + ROOK_PST[pst])
                        eg += sign * (ROOK + ROOK_PST[pst])
                    }
                    PieceType.QUEEN -> {
                        phase += 4
                        mg += sign * (QUEEN + QUEEN_PST[pst])
                        eg += sign * (QUEEN + QUEEN_PST[pst])
                    }
                    PieceType.KING -> {
                        kingSquare[side.ordinal] = sq
                        mg += sign * KING_MG[pst]
                        eg += sign * KING_EG[pst]
                    }
                }
            }
        }

        // ── Pawn structure ──────────────────────────────────────────────────────
        for (sideOrdinal in 0..1) {
            val sign = if (sideOrdinal == 0) 1 else -1
            val ours = sideOrdinal * 8
            val theirs = (1 - sideOrdinal) * 8
            for (file in 0..7) {
                val mask = pawnMask[ours + file]
                if (mask == 0) continue

                val count = Integer.bitCount(mask)
                if (count > 1) {
                    mg += sign * DOUBLED_PAWN * (count - 1)
                    eg += sign * DOUBLED_PAWN * (count - 1)
                }

                val left = if (file > 0) pawnMask[ours + file - 1] else 0
                val right = if (file < 7) pawnMask[ours + file + 1] else 0
                if (left == 0 && right == 0) {
                    mg += sign * ISOLATED_PAWN * count
                    eg += sign * ISOLATED_PAWN * count
                }

                // Passed: no enemy pawn on this or an adjacent file ahead of it.
                var bits = mask
                while (bits != 0) {
                    val rank = Integer.numberOfTrailingZeros(bits)
                    bits = bits and (bits - 1)
                    val ahead = if (sideOrdinal == 0) {
                        // White: ranks strictly greater than `rank`.
                        (-1 shl (rank + 1)) and 0xFF
                    } else {
                        // Black: ranks strictly smaller.
                        (1 shl rank) - 1
                    }
                    val blockers = (pawnMask[theirs + file] or
                        (if (file > 0) pawnMask[theirs + file - 1] else 0) or
                        (if (file < 7) pawnMask[theirs + file + 1] else 0)) and ahead
                    if (blockers == 0) {
                        val advanced = if (sideOrdinal == 0) rank else 7 - rank
                        mg += sign * PASSED_MG[advanced]
                        eg += sign * PASSED_EG[advanced]
                    }
                }
            }
        }

        // ── Rooks on open files ─────────────────────────────────────────────────
        for (rank in 0..7) {
            for (file in 0..7) {
                val piece = board[rank * 16 + file] ?: continue
                if (piece.type != PieceType.ROOK) continue
                val side = piece.side
                val sign = if (side == Side.WHITE) 1 else -1
                val own = pawnMask[side.ordinal * 8 + file]
                val enemy = pawnMask[(1 - side.ordinal) * 8 + file]
                val bonus = when {
                    own == 0 && enemy == 0 -> ROOK_OPEN_FILE
                    own == 0 -> ROOK_SEMI_OPEN_FILE
                    else -> 0
                }
                mg += sign * bonus
                eg += sign * bonus
            }
        }

        // ── Bishop pair ─────────────────────────────────────────────────────────
        if (bishops[0] >= 2) { mg += BISHOP_PAIR_MG; eg += BISHOP_PAIR_EG }
        if (bishops[1] >= 2) { mg -= BISHOP_PAIR_MG; eg -= BISHOP_PAIR_EG }

        // ── Driving a bare king to the edge ─────────────────────────────────────
        //
        // Without this the engine wins the queen, then shuffles: every move looks equal
        // to material-plus-table, so nothing pulls the enemy king towards the rail. It
        // is the difference between an opponent that mates and one that runs into the
        // fifty-move rule with a queen extra.
        if (kingSquare[0] >= 0 && kingSquare[1] >= 0) {
            if (material[1] == 0 && material[0] >= ROOK) {
                val drive = mateDrive(kingSquare[0], kingSquare[1])
                mg += drive; eg += drive
            } else if (material[0] == 0 && material[1] >= ROOK) {
                val drive = mateDrive(kingSquare[1], kingSquare[0])
                mg -= drive; eg -= drive
            }
        }

        val p = phase.coerceAtMost(PHASE_MAX)
        val blended = (mg * p + eg * (PHASE_MAX - p)) / PHASE_MAX
        return if (pos.sideToMove == Side.WHITE) blended else -blended
    }

    /** How far into the middlegame the position is: [PHASE_MAX] at the start, 0 bare. */
    fun phaseOf(pos: Position): Int {
        var phase = 0
        for (rank in 0..7) {
            for (file in 0..7) {
                val piece = pos.board[rank * 16 + file] ?: continue
                phase += when (piece.type) {
                    PieceType.KNIGHT, PieceType.BISHOP -> 1
                    PieceType.ROOK -> 2
                    PieceType.QUEEN -> 4
                    else -> 0
                }
            }
        }
        return phase.coerceAtMost(PHASE_MAX)
    }

    /**
     * One side is down to a bare king and the other has enough left to mate.
     *
     * The engine treats this regime specially: see [mateDrive] for the evaluation side
     * and `Engine.chooseMove` for why the difficulty setting steps aside here.
     */
    fun isBareKingEndgame(pos: Position): Boolean {
        val material = IntArray(2)
        for (rank in 0..7) {
            for (file in 0..7) {
                val piece = pos.board[rank * 16 + file] ?: continue
                material[piece.side.ordinal] += valueOf(piece.type)
            }
        }
        return (material[0] == 0 && material[1] >= ROOK) ||
            (material[1] == 0 && material[0] >= ROOK)
    }

    /**
     * Weak king towards the edge, strong king towards the weak one.
     *
     * ⚠️ The weights are large on purpose. They are the only gradient that exists once
     * the board is down to a bare king - material and tables say the same thing about
     * every move there - and they have to stand out clearly against the difficulty
     * setting's tolerance, or the engine wanders instead of mating. They cost nothing
     * elsewhere: the term is only added when [isBareKingEndgame] holds, and by then
     * there is no material decision left to distort.
     */
    private fun mateDrive(strongKing: Int, weakKing: Int): Int {
        val wf = weakKing and 7
        val wr = weakKing shr 4
        val sf = strongKing and 7
        val sr = strongKing shr 4
        val fromCentre = kotlin.math.abs(2 * wf - 7) + kotlin.math.abs(2 * wr - 7)
        val between = kotlin.math.abs(wf - sf) + kotlin.math.abs(wr - sr)
        return (fromCentre - 2) * DRIVE_TO_EDGE + (14 - between) * DRIVE_KINGS_TOGETHER
    }

    /**
     * Index into a table that is written with rank 8 first. Black reads the same table
     * mirrored, which is what makes one table serve both sides.
     */
    private fun pstIndex(side: Side, file: Int, rank: Int): Int =
        if (side == Side.WHITE) (7 - rank) * 8 + file else rank * 8 + file
}
