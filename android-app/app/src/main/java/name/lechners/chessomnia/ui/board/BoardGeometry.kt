package name.lechners.chessomnia.ui.board

import name.lechners.chessomnia.rules.Side
import name.lechners.chessomnia.rules.Square

/**
 * Converts between screen pixels and squares - the ONLY place where the board's
 * orientation is decided.
 *
 * [bottomSide] is the colour of the player sitting at the lower table edge. The tablet
 * lies flat between the two; swapping colours therefore turns the board instead of
 * making somebody turn the device. There is deliberately NO automatic rotation after
 * each move - one player sees the board from the other side, exactly as at a real board.
 *
 * Deliberately a plain class with no Compose dependency: [squareAt] is the most
 * error-prone function in the whole UI and belongs under a unit test.
 */
class BoardGeometry(
    val boardSizePx: Float,
    val bottomSide: Side = Side.WHITE,
) {
    val squareSizePx: Float get() = boardSizePx / 8f

    private val flipped: Boolean get() = bottomSide == Side.BLACK

    /** Column on screen, 0 = leftmost. */
    private fun columnOf(square: Square): Int =
        if (flipped) 7 - square.file else square.file

    /** Row on screen, 0 = topmost. */
    private fun rowOf(square: Square): Int =
        if (flipped) square.rank else 7 - square.rank

    fun originXOf(square: Square): Float = columnOf(square) * squareSizePx

    fun originYOf(square: Square): Float = rowOf(square) * squareSizePx

    fun centerXOf(square: Square): Float = originXOf(square) + squareSizePx / 2f

    fun centerYOf(square: Square): Float = originYOf(square) + squareSizePx / 2f

    /** The square at a screen column/row (0,0 = top left). */
    fun squareAtCell(column: Int, row: Int): Square =
        if (flipped) Square.of(7 - column, row) else Square.of(column, 7 - row)

    /** The square at a screen position, or null outside the board. */
    fun squareAt(x: Float, y: Float): Square? {
        if (x < 0f || y < 0f || x >= boardSizePx || y >= boardSizePx) return null
        val col = (x / squareSizePx).toInt().coerceIn(0, 7)
        val row = (y / squareSizePx).toInt().coerceIn(0, 7)
        return if (flipped) Square.of(7 - col, row) else Square.of(col, 7 - row)
    }
}
