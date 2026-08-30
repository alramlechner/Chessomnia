# Contributing

Thanks for looking. A few things worth knowing before you spend time on a change.

## Scope

Chessomnia is a **replacement for a physical chess board**, not a chess program. The
following will be declined regardless of how well they are implemented:

- a computer opponent or any engine
- position evaluation, an evaluation bar, best-move hints
- opening books, endgame tablebases
- online or networked play

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
