package name.lechners.chessomnia.data

import name.lechners.chessomnia.rules.Piece
import name.lechners.chessomnia.rules.Side
import name.lechners.chessomnia.rules.Square

/** What belongs in the report about the app and the device. */
data class AppInfo(
    val versionName: String,
    val versionCode: Long,
    val device: String,
    val androidRelease: String,
    val sdkInt: Int,
)

/** A snapshot of the game for the bug report. */
data class BugReportData(
    val app: AppInfo,
    val timestamp: String,
    val startFen: String,
    val moves: List<String>,
    val currentFen: String,
    val status: String,
    val sideToMove: Side,
    val inCheck: Boolean,
    /**
     * The decisive field. For the report "the mate was not detected" this states in
     * black and white which moves were still available - a non-empty list means it was
     * not mate.
     */
    val legalMoves: List<String>,
    val halfmoveClock: Int,
    val fullmoveNumber: Int,
    val board: Array<Piece?>,
    val whiteThinkMs: Long,
    val blackThinkMs: Long,
    val clockEnabled: Boolean,
) {
    // An array inside a data class: equals/hashCode are not needed here, but the ones
    // the compiler generates would be wrong. So override them explicitly.
    override fun equals(other: Any?) = this === other
    override fun hashCode() = System.identityHashCode(this)
}

/**
 * Builds the bug report as plain text.
 *
 * Deliberately plain text and a pure function: it has to be pushable into any app via
 * the share sheet, and that also makes it testable without Android.
 *
 * The text is always English, independent of the UI language. It is addressed to the
 * developer, and a translated version would need a Context, which would cost both the
 * pure function and its testability.
 */
object BugReport {

    fun compose(description: String, d: BugReportData): String = buildString {
        appendLine("CHESSOMNIA BUG REPORT")
        appendLine("=====================")
        appendLine()
        appendLine("Description:")
        appendLine(description.ifBlank { "(none given)" })
        appendLine()
        appendLine("Time:    ${d.timestamp}")
        appendLine("App:     ${d.app.versionName} (${d.app.versionCode})")
        appendLine("Device:  ${d.app.device}, Android ${d.app.androidRelease} (API ${d.app.sdkInt})")
        appendLine()
        appendLine("POSITION")
        appendLine("--------")
        appendLine(asciiBoard(d.board))
        appendLine("FEN:             ${d.currentFen}")
        appendLine("To move:         ${sideName(d.sideToMove)}")
        appendLine("In check:        ${if (d.inCheck) "yes" else "no"}")
        appendLine("App status:      ${d.status}")
        appendLine("Halfmove clock:  ${d.halfmoveClock} (draw at 100)")
        appendLine("Move number:     ${d.fullmoveNumber}")
        if (d.clockEnabled) {
            appendLine("Thinking time:   White ${formatMs(d.whiteThinkMs)}, Black ${formatMs(d.blackThinkMs)}")
        }
        appendLine()
        appendLine("LEGAL MOVES IN THIS POSITION (${d.legalMoves.size})")
        appendLine("-----------------------------------------")
        appendLine(if (d.legalMoves.isEmpty()) "(none - checkmate or stalemate)" else d.legalMoves.joinToString(" "))
        appendLine()
        appendLine("GAME (${d.moves.size} halfmoves)")
        appendLine("--------------------")
        appendLine("Starting position: ${d.startFen}")
        appendLine(numberedMoves(d.moves).ifBlank { "(no move played yet)" })
    }

    /** Moves numbered in pairs, so the game stays readable as a whole. */
    fun numberedMoves(moves: List<String>): String {
        val sb = StringBuilder()
        var i = 0
        var number = 1
        while (i < moves.size) {
            sb.append(number).append(". ").append(moves[i])
            if (i + 1 < moves.size) sb.append(' ').append(moves[i + 1])
            sb.append(if (number % 5 == 0) "\n" else "   ")
            i += 2
            number++
        }
        return sb.toString().trimEnd()
    }

    /** The board from rank 8 down to 1 - the way White sees it. */
    fun asciiBoard(board: Array<Piece?>): String = buildString {
        appendLine("  +------------------------+")
        for (rank in 7 downTo 0) {
            append(rank + 1).append(" |")
            for (file in 0..7) {
                val piece = board[Square.of(file, rank).index]
                append(' ').append(piece?.fenChar ?: '.').append(' ')
            }
            appendLine("|")
        }
        appendLine("  +------------------------+")
        append("    a  b  c  d  e  f  g  h")
    }

    // The report goes to the developer, not to the player: it stays English and
    // independent of the chosen language. Otherwise it would depend on a Context and
    // would no longer be checkable as a pure function.
    private fun sideName(side: Side) = if (side == Side.WHITE) "White" else "Black"

    private fun formatMs(ms: Long): String {
        val total = ms.coerceAtLeast(0L) / 1000
        return "%d:%02d:%02d".format(total / 3600, (total % 3600) / 60, total % 60)
    }
}
