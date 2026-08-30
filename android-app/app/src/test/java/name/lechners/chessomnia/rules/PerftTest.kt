package name.lechners.chessomnia.rules

import org.junit.Assert.assertEquals
import org.junit.Assume
import org.junit.Test

/**
 * The actual correctness proof of the rule engine.
 *
 * Between them the six positions cover: loss of castling rights through a rook capture,
 * en passant including the rank pin, promotion with and without a capture, double check,
 * and castling out of and through check.
 */
class PerftTest {

    /** Runs on every build - well under a second in total. */
    @Test
    fun perftToDepth3() {
        for (case in PerftPositions.ALL) {
            val pos = Fen.parse(case.fen)
            for (depth in 1..minOf(3, case.expected.size)) {
                assertEquals(
                    "${case.name}, Tiefe $depth",
                    case.expected[depth - 1],
                    Perft.count(pos, depth),
                )
            }
        }
    }

    /** Only on request: ./gradlew test -DperftDeep=1 */
    @Test
    fun perftDeep() {
        Assume.assumeNotNull(System.getProperty("perftDeep"))
        for (case in PerftPositions.ALL) {
            val pos = Fen.parse(case.fen)
            for (depth in 4..case.expected.size) {
                assertEquals(
                    "${case.name}, Tiefe $depth",
                    case.expected[depth - 1],
                    Perft.count(pos, depth),
                )
            }
        }
    }
}
