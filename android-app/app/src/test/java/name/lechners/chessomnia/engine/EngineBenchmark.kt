package name.lechners.chessomnia.engine

import name.lechners.chessomnia.rules.Fen
import org.junit.Assume
import org.junit.Test

/**
 * How long the opponent takes to answer, and how deep it gets in that time.
 *
 * Not a correctness test and not part of the normal build - the numbers depend entirely
 * on the machine. Run it when a change is meant to affect speed:
 *
 * ```
 * ./gradlew testDebugUnitTest --tests "*EngineBenchmark*" -DengineBench=1
 * ```
 *
 * What matters for the app is the wall clock per move, not the node rate: a tablet
 * player waiting for the board to answer notices a second, and nothing else.
 */
class EngineBenchmark {

    private val positions = listOf(
        "opening" to Fen.START,
        "middlegame" to
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
        "endgame" to "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
    )

    @Test
    fun measureEachLevel() {
        Assume.assumeNotNull(System.getProperty("engineBench"))

        // Warm the JIT up first. Without this the level that happens to run first
        // measures the interpreter and reads three times too slow, which is exactly the
        // sort of number that gets quoted later.
        repeat(3) {
            for ((_, fen) in positions) Engine(Level.CLUB).chooseMove(Fen.parse(fen))
        }

        println("level      position     ms   depth       nodes    knps")
        for (level in Level.entries) {
            for ((name, fen) in positions) {
                val engine = Engine(level)
                val position = Fen.parse(fen)
                val started = System.currentTimeMillis()
                engine.chooseMove(position)
                val elapsed = System.currentTimeMillis() - started
                val knps = if (elapsed > 0) engine.nodes / elapsed else 0
                println(
                    "%-10s %-10s %5d %7d %11d %7d".format(
                        level.name, name, elapsed, engine.depthReached, engine.nodes, knps,
                    )
                )
            }
        }
    }
}
