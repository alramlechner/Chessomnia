# Contributing

Thanks for looking. A few things worth knowing before you spend time on a change.

## Scope

Chessomnia is a **replacement for a physical chess board** that can also play against you.
The following will be declined regardless of how well they are implemented:

- position evaluation shown to the player, an evaluation bar, best-move hints
- endgame tablebases
- online or networked play
- accounts, advertising, or anything that is not free to use

**Making the opponent stronger is also out of scope.** The ceiling is deliberately a good
club amateur: this is a board for the family table, and an opponent nobody can beat is not
a feature. Transposition tables, deeper search and a bundled strong engine have all been
considered and set aside — see `ARCHITECTURE.md`, *The opponent*.

That boundary is the product, not an oversight. Everything else is open for discussion.

## Bug reports

The most useful report comes from the app itself: **Settings → Report a problem**. It
produces a plain-text dump of the position, every move played, and — crucially — every
move that is legal in the current position.

That last field settles the most common report by far. "The app did not notice the
checkmate" is answered immediately by the legal-move list: if it is not empty, it was not
mate. Please paste the whole report into the issue.

## Working on the rules engine

`rules/` is pure Kotlin with **no Android imports**, so it runs as a plain JVM unit test.
Keep it that way — it is the reason the engine can be tested exhaustively.

Before submitting a change there:

```bash
cd android-app
./gradlew test -DperftDeep=1
```

That runs perft to depth 5 against all six standard positions (12.4 million nodes) plus
the 4,576-position cross-check against python-chess. Both must pass exactly. A change that
makes perft disagree is wrong, even if it looks right.

If you add a rule case, add it to the reference corpus rather than hand-writing the
expectation. Hand-written chess test data is unreliable — see the note in the README.

## Working on the opponent

`engine/` is pure Kotlin with no Android imports too, for the same reason: whether an
opponent actually mates can only be settled by playing games out.

⚠️ **A change that makes the engine play better is not automatically an improvement.**
Check `GamePlayTest` — it plays whole games and insists that queen-and-king and
rook-and-king are converted to mate. Those are the tests that catch the classic failure of
a small engine: the won endgame it never finishes, shuffling until the fifty-move rule
ends it in a draw. A mate-in-one test would never notice.

Speed work should quote before-and-after numbers from the benchmark:

```bash
./gradlew testDebugUnitTest --tests "*EngineBenchmark*" -DengineBench=1
```

## Translations

The UI lives in `app/src/main/res/values/strings.xml` (English, the default) with
translations in `values-<lang>/`. To add a language, copy the default file, translate the
values, and keep the keys and their order identical — a missing key silently falls back to
English and shows up as a stray English line mid-sentence.

The bug report text is deliberately **not** translated: it is addressed to the developer,
and keeping it a pure function without an Android `Context` is what makes it testable.

## Style

Match the surrounding code. Comments explain *why*, not *what* — the existing ones are
worth reading before you add your own.
