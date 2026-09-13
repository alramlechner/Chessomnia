package name.lechners.chessomnia.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.util.lerp
import name.lechners.chessomnia.rules.Move
import name.lechners.chessomnia.rules.MoveKind
import name.lechners.chessomnia.rules.Piece
import name.lechners.chessomnia.rules.Side
import name.lechners.chessomnia.rules.Square

/**
 * How long the device's piece takes to cross the board.
 *
 * Long enough to be followed, short enough not to be waited for: the device already owes
 * a minimum thinking time before this even starts.
 */
const val SLIDE_MS = 300

/**
 * The board - a single Canvas instead of 64 composables.
 *
 * The reason is not speed but that there is exactly ONE source of geometry: hit testing,
 * markers, pieces and coordinates all compute against the same [BoardGeometry]. With 64
 * children the hit test would live in the child and the square-spanning overlays (the
 * check glow, the castling hint) in the parent - and those two drift apart over time.
 */
@Composable
fun ChessBoard(
    board: Array<Piece?>,
    bottomSide: Side,
    selected: Square?,
    hints: List<Move>,
    lastMove: Move?,
    checkedKing: Square?,
    showCoordinates: Boolean,
    onSquareTap: (Square) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * A move to show arriving rather than already arrived. See [MoveAnimation] for why
     * only some moves get one.
     */
    animation: MoveAnimation? = null,
    /**
     * Draws the empty board: no pieces, no hints, no highlights. Used by the pause
     * curtain, where the point is that the position cannot be studied while the clock
     * is stopped. The squares and the coordinates stay - they carry no information
     * about the game.
     */
    piecesHidden: Boolean = false,
) {
    val painters = rememberPiecePainters()
    val textMeasurer = rememberTextMeasurer()
    val tap = rememberUpdatedState(onSquareTap)
    // The gesture detector is built only once; without this indirection it would keep
    // computing with the old orientation after a side swap.
    val bottom = rememberUpdatedState(bottomSide)

    // What is in the air right now, and how far along it is. Null between moves, which is
    // almost always: then everything below draws the board exactly as it did before.
    var flight by remember { mutableStateOf<MoveAnimation?>(null) }
    val progress = remember { Animatable(1f) }
    // Initialised from the key that is already there, so the first composition shows the
    // position as it stands instead of replaying the move that produced it. That is the
    // difference between a move landing and a screen coming back.
    var seenKey by remember { mutableStateOf(animation?.key) }

    LaunchedEffect(animation?.key) {
        val next = animation
        if (next == null || next.key == seenKey) {
            flight = null
            return@LaunchedEffect
        }
        seenKey = next.key
        flight = next
        try {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(SLIDE_MS, easing = FastOutSlowInEasing))
        } finally {
            // Also on cancellation - moving while the device's piece is still travelling
            // would otherwise leave that piece frozen in mid-board for good.
            flight = null
        }
    }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                val geo = BoardGeometry(size.width.toFloat(), bottom.value)
                geo.squareAt(offset.x, offset.y)?.let { tap.value(it) }
            }
        }
    ) {
        val geo = BoardGeometry(size.minDimension, bottomSide)

        drawSquares(geo)

        if (!piecesHidden) {
            lastMove?.let {
                fillSquare(geo, it.from, BoardColors.lastMove)
                fillSquare(geo, it.to, BoardColors.lastMove)
            }
            checkedKing?.let { drawCheckGlow(geo, it) }
            selected?.let { fillSquare(geo, it, BoardColors.selection) }

            // Castling and en passant touch squares that are neither origin nor target.
            // Making exactly those visible is the actual learning effect.
            for (m in hints) drawSecondarySquares(geo, m)

            val moving = flight
            val travelled = progress.value
            val slides = moving?.let { slidesOf(it.move, board) } ?: emptyList()

            // The travelling pieces are drawn last, on their way; their target squares are
            // therefore left empty here rather than showing them already arrived.
            drawPieces(geo, board, painters, bottomSide, skip = slides.mapTo(HashSet()) { it.to.index })

            // A piece that is being taken stays on its square for the whole slide, under
            // the piece coming to take it. It is not removed here at all: when the slide
            // ends, CapturedPieceFlight picks it up and floats it out to the edge.
            val victim = moving?.captured
            val victimSquare = moving?.capturedSquare
            if (victim != null && victimSquare != null) {
                painters[victim]?.let {
                    drawPieceAt(
                        geo, victim, it,
                        geo.originXOf(victimSquare), geo.originYOf(victimSquare), bottomSide,
                    )
                }
            }

            for (slide in slides) {
                painters[slide.piece]?.let { drawSlide(geo, slide, it, bottomSide, travelled) }
            }

            for (m in hints) drawMoveHint(geo, m, board)
        }

        if (showCoordinates) drawCoordinates(geo, textMeasurer)
    }
}

@Composable
private fun rememberPiecePainters(): Map<Piece, VectorPainter> {
    // A fixed order across every composition - the precondition for
    // rememberVectorPainter staying stable inside a loop.
    val entries = Piece.entries
    val painters = ArrayList<VectorPainter>(entries.size)
    for (piece in entries) {
        painters += rememberVectorPainter(ImageVector.vectorResource(PieceAssets.drawableOf(piece)))
    }
    return entries.indices.associate { entries[it] to painters[it] }
}

// ── Layers ──────────────────────────────────────────────────────────────────────

private fun DrawScope.drawSquares(geo: BoardGeometry) {
    val s = geo.squareSizePx
    for (rank in 0..7) {
        for (file in 0..7) {
            val sq = Square.of(file, rank)
            drawRect(
                color = if (sq.isLightSquare) BoardColors.light else BoardColors.dark,
                topLeft = Offset(geo.originXOf(sq), geo.originYOf(sq)),
                size = Size(s, s),
            )
        }
    }
}

private fun DrawScope.fillSquare(geo: BoardGeometry, sq: Square, color: Color) {
    val s = geo.squareSizePx
    drawRect(color, Offset(geo.originXOf(sq), geo.originYOf(sq)), Size(s, s))
}

private fun DrawScope.drawCheckGlow(geo: BoardGeometry, sq: Square) {
    val s = geo.squareSizePx
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(BoardColors.check, Color.Transparent),
            center = Offset(geo.centerXOf(sq), geo.centerYOf(sq)),
            radius = s * 0.62f,
        ),
        radius = s * 0.62f,
        center = Offset(geo.centerXOf(sq), geo.centerYOf(sq)),
    )
}

/** Squares a special move moves or clears as well: the rook target and the en-passant victim. */
private fun DrawScope.drawSecondarySquares(geo: BoardGeometry, move: Move) {
    when (move.kind) {
        MoveKind.CASTLE_KINGSIDE -> fillSquare(geo, Square(move.to.index - 1), BoardColors.lastMove)
        MoveKind.CASTLE_QUEENSIDE -> fillSquare(geo, Square(move.to.index + 1), BoardColors.lastMove)
        MoveKind.EN_PASSANT -> {
            val victim = move.to.index - if (move.to.rank > move.from.rank) 16 else -16
            fillSquare(geo, Square(victim), BoardColors.opponentHint)
        }
        else -> Unit
    }
}

/**
 * The pieces of the player sitting at the TOP are drawn rotated by 180 degrees -
 * otherwise they would see their own material upside down. At a real board the
 * three-dimensional shape of the pieces solves this; on a flat display it has to be
 * drawn.
 */
private fun DrawScope.drawPieces(
    geo: BoardGeometry,
    board: Array<Piece?>,
    painters: Map<Piece, VectorPainter>,
    bottomSide: Side,
    /** Squares whose piece is drawn elsewhere this frame, because it is in flight. */
    skip: Set<Int> = emptySet(),
) {
    for (rank in 0..7) {
        for (file in 0..7) {
            val sq = Square.of(file, rank)
            if (sq.index in skip) continue
            val piece = board[sq.index] ?: continue
            val painter = painters[piece] ?: continue
            drawPieceAt(geo, piece, painter, geo.originXOf(sq), geo.originYOf(sq), bottomSide)
        }
    }
}

/** One piece at a free position, not necessarily on a square - a slide ends between two. */
private fun DrawScope.drawPieceAt(
    geo: BoardGeometry,
    piece: Piece,
    painter: VectorPainter,
    x: Float,
    y: Float,
    bottomSide: Side,
    alpha: Float = 1f,
) {
    val s = geo.squareSizePx
    if (piece.side == bottomSide) {
        translate(left = x, top = y) { with(painter) { draw(Size(s, s), alpha = alpha) } }
    } else {
        rotate(degrees = 180f, pivot = Offset(x + s / 2f, y + s / 2f)) {
            translate(left = x, top = y) { with(painter) { draw(Size(s, s), alpha = alpha) } }
        }
    }
}

/** A piece on its way, [travelled] of the distance done. */
private fun DrawScope.drawSlide(
    geo: BoardGeometry,
    slide: Slide,
    painter: VectorPainter,
    bottomSide: Side,
    travelled: Float,
) = drawPieceAt(
    geo, slide.piece, painter,
    lerp(geo.originXOf(slide.from), geo.originXOf(slide.to), travelled),
    lerp(geo.originYOf(slide.from), geo.originYOf(slide.to), travelled),
    bottomSide,
)

private data class Slide(val piece: Piece, val from: Square, val to: Square)

/**
 * The pieces one move puts in motion: its own, and for castling the rook as well — a king
 * gliding while its rook jumps would look like two different rules.
 *
 * Read from the board AFTER the move, so a promotion carries the piece that now stands
 * there rather than the pawn that set off.
 */
private fun slidesOf(move: Move, board: Array<Piece?>): List<Slide> {
    val mover = board[move.to.index] ?: return emptyList()
    val main = Slide(mover, move.from, move.to)

    // Same file arithmetic as drawSecondarySquares: the rook ends up beside the king.
    val rookFrom: Square
    val rookTo: Square
    when (move.kind) {
        MoveKind.CASTLE_KINGSIDE -> {
            rookFrom = Square(move.to.index + 1)
            rookTo = Square(move.to.index - 1)
        }
        MoveKind.CASTLE_QUEENSIDE -> {
            rookFrom = Square(move.to.index - 2)
            rookTo = Square(move.to.index + 1)
        }
        else -> return listOf(main)
    }
    val rook = board[rookTo.index] ?: return listOf(main)
    return listOf(main, Slide(rook, rookFrom, rookTo))
}

/**
 * A dot for a quiet move, a ring for a capture. Special moves get their own colour so
 * that castling and en passant do not disappear among the ordinary dots - they are the
 * reason this display exists at all.
 */
private fun DrawScope.drawMoveHint(
    geo: BoardGeometry,
    move: Move,
    board: Array<Piece?>,
) {
    val s = geo.squareSizePx
    val center = Offset(geo.centerXOf(move.to), geo.centerYOf(move.to))
    val special = move.isCastle || move.kind == MoveKind.EN_PASSANT || move.kind == MoveKind.PROMOTION
    val color = if (special) BoardColors.specialHint else BoardColors.moveHint
    val isCapture = board[move.to.index] != null || move.kind == MoveKind.EN_PASSANT

    if (isCapture) {
        drawCircle(color, radius = s * 0.42f, center = center, style = Stroke(width = s * 0.08f))
    } else {
        drawCircle(color, radius = s * 0.16f, center = center)
    }
    // Mark a promotion with an additional inner ring: a choice is waiting there.
    if (move.kind == MoveKind.PROMOTION) {
        drawCircle(color, radius = s * 0.26f, center = center, style = Stroke(width = s * 0.05f))
    }
}

private fun DrawScope.drawCoordinates(geo: BoardGeometry, measurer: TextMeasurer) {
    val s = geo.squareSizePx
    val style = TextStyle(color = BoardColors.coordinate, fontSize = (s * 0.16f).toSp())
    // Over screen columns and rows, not over files and ranks: with the sides swapped,
    // rank 1 is at the top and the h-file on the left.
    for (column in 0..7) {
        val sq = geo.squareAtCell(column, 7)
        val layout = measurer.measure(('a' + sq.file).toString(), style)
        drawText(
            layout,
            topLeft = Offset(
                geo.originXOf(sq) + s - layout.size.width - s * 0.06f,
                geo.originYOf(sq) + s - layout.size.height - s * 0.02f,
            ),
        )
    }
    for (row in 0..7) {
        val sq = geo.squareAtCell(0, row)
        val layout = measurer.measure((sq.rank + 1).toString(), style)
        drawText(
            layout,
            topLeft = Offset(geo.originXOf(sq) + s * 0.06f, geo.originYOf(sq) + s * 0.03f),
        )
    }
}
