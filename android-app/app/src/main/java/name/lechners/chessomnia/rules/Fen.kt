package name.lechners.chessomnia.rules

/** Forsyth-Edwards notation. Carries persistence, the tests and the repetition key. */
object Fen {

    const val START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    fun parse(fen: String): Position {
        val parts = fen.trim().split(Regex("\\s+"))
        require(parts.size >= 4) { "FEN braucht mindestens 4 Felder: $fen" }

        val board = arrayOfNulls<Piece>(128)
        var rank = 7
        var file = 0
        for (c in parts[0]) {
            when {
                c == '/' -> {
                    require(file == 8) { "Reihe ${rank + 1} hat $file statt 8 Felder: $fen" }
                    rank--; file = 0
                    require(rank >= 0) { "Zu viele Reihen: $fen" }
                }
                c.isDigit() -> {
                    file += c - '0'
                    require(file <= 8) { "Reihe ${rank + 1} zu lang: $fen" }
                }
                else -> {
                    val piece = Piece.fromFenChar(c)
                        ?: throw IllegalArgumentException("Unbekanntes Zeichen '$c' in: $fen")
                    require(file < 8) { "Reihe ${rank + 1} zu lang: $fen" }
                    board[rank * 16 + file] = piece
                    file++
                }
            }
        }
        require(rank == 0 && file == 8) { "Stellungsteil unvollstaendig: $fen" }

        val side = when (parts[1]) {
            "w" -> Side.WHITE
            "b" -> Side.BLACK
            else -> throw IllegalArgumentException("Ungueltige Zugfarbe '${parts[1]}': $fen")
        }
        val rights = CastlingRights.fromFen(parts[2])
        val ep = if (parts[3] == "-") null else Square.parse(parts[3])
        val halfmove = parts.getOrNull(4)?.toIntOrNull() ?: 0
        val fullmove = parts.getOrNull(5)?.toIntOrNull() ?: 1

        return Position(board, side, rights, ep, halfmove, fullmove)
    }

    /** The position part only - the basis of the repetition key. */
    fun piecePlacement(pos: Position): String {
        val sb = StringBuilder(72)
        for (rank in 7 downTo 0) {
            var empty = 0
            for (file in 0..7) {
                val p = pos.board[rank * 16 + file]
                if (p == null) {
                    empty++
                } else {
                    if (empty > 0) { sb.append(empty); empty = 0 }
                    sb.append(p.fenChar)
                }
            }
            if (empty > 0) sb.append(empty)
            if (rank > 0) sb.append('/')
        }
        return sb.toString()
    }

    fun serialize(pos: Position): String = buildString {
        append(piecePlacement(pos))
        append(' ').append(if (pos.sideToMove == Side.WHITE) 'w' else 'b')
        append(' ').append(CastlingRights.toFen(pos.castlingRights))
        append(' ').append(pos.epTarget?.algebraic ?: "-")
        append(' ').append(pos.halfmoveClock)
        append(' ').append(pos.fullmoveNumber)
    }
}
