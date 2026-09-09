# Chessomnia — Architecture

A chess board for Android tablets. **It replaces the physical board** — two people play
each other on one device.

Since the engine package it can also be **one person against the device**. What stays out
is everything that would turn the board into an analysis tool: no evaluation bar, no
"best move" hint, no opening explorer. The opponent plays its move and says nothing about
the human's position.

Its second purpose is learning: tap a piece and every legal move for it is marked,
including the special moves — castling, en passant, promotion — that beginners overlook.

---

## Product decisions

| Point | Decision |
|---|---|
| What the app promises | **Free · ad-free · registration-free · no tracking.** The four load-bearing ones — see below |
| Setup | **The tablet lies flat on the table and the players sit opposite each other** — as at a real board |
| Board orientation | **Fixed, White always at the bottom.** No auto-rotation, no flip flag |
| Learning aids | Mark legal moves · check/mate/stalemate · take back a move |
| Opponent | Optional. Four strengths, the top one a good club amateur — **not** as strong as it could be, the bottom one beatable by a nine-year-old |
| Deliberately absent | Move list / SAN notation, PGN export, any form of engine *analysis* shown to the player |
| Clock | **Counts upward, never expires.** Fully switchable |
| Pieces | Classic Staunton as VectorDrawables |
| Extras | Start a new game from the board · the game survives an app restart · hints and takebacks switchable · the screen stays on |

### The four promises, and which one is structural

Free, ad-free, registration-free and untracked are what the store copy leads with, so they
have to survive every future change rather than be re-argued each time. Three of them are
kept by simply not adding something: no billing, no ad SDK, no account.

⚠️ The fourth is different in kind, and it is the one to protect. **"No tracking" is not a
policy here, it is a property of the manifest:** the release build requests no Android
permission that grants it any capability — no `INTERNET` above all — so the app is
*incapable* of sending anything anywhere, whatever any future code might try. That is
worth far more than a promise, and it is also fragile: a single dependency that declares
`INTERNET` in its own manifest would merge it in and quietly turn a property back into a
promise. `RELEASING.md` step 4 greps the merged manifest for exactly this reason, and it
is not an optional step.

The engine matters here too. An opponent is the obvious thing to have called out to a
server, and it deliberately is not: the search runs on the device, adds no dependency and
no permission, which is why adding it did not cost any of the four.

### An opponent, but not an analysis tool

"No computer opponent" used to be part of how the app described itself. That is no longer
true and no longer the positioning — a chess computer is a fine thing for a board to have
when nobody else is at the table. What did *not* change is the line next to it: no
evaluation bar, no best-move hint, no opening explorer. The opponent plays its move and
says nothing about the human's position. Those were always two separate decisions; only
one of them was reversed.

### What the setup means for the layout

The tablet lies flat between the players. Almost the entire layout follows from that:

- **One player sees the board upside down** — exactly as at a real board. That is not a
  shortcoming; it is the reason there is no auto-rotation after each move.
- **The upper player's pieces are drawn rotated 180°** so that they read their own
  material the right way up. At a real board the three-dimensional shape does this; on a
  flat display it has to be drawn.
- **"Swap colours"** turns the board 180° instead of making somebody pick the tablet up
  (`Settings.boardBottomSide`, a button in both panels). This is the only reason
  `BoardGeometry` knows about a side swap at all — the doubled coordinate test matrix is
  covered exhaustively in `BoardGeometryTest`.
- **Captured pieces** stand between board and panel on the side of whoever took them, as
  they would beside the board. With the *net* material advantage; a plain sum would
  mislead, since trading queens would read "+9".
- **Everything except the board exists twice**, once at each player's edge: clock, status
  line and action buttons. Black's panel is rotated 180°. A single panel would always be
  upside down for one of the two.
- **Panels belong at the top and bottom, never at the sides.** The players face each
  other, they do not sit side by side. That gives *one* layout instead of separate
  portrait and landscape variants: the board sits square in the middle, the panels lie
  along the two edges where somebody is sitting.
- **Dialogs turn to face whoever acted** (promotion, confirmations). A dialog in a fixed
  orientation would always be unreadable for one of the two.
  ⚠️ What matters is the **seat, not the colour**: `isSeatedAtTop(side, bottomSide)` and
  `rotationFor(...)` in `SeatOrientation.kt`. Testing for `== BLACK` has been wrong since
  "swap colours" existed — it turned the promotion dialog the wrong way up for *both*
  players after a swap. Every prompt therefore carries its own `initiator`.
- **The end of the game does not put a window over the board.** Result and reason appear
  in the two panels at the table edges. After a mate you want to see *why* — the
  highlighted king and the marked last move are the answer, and nothing may cover them.

---

## Technical frame

| | |
|---|---|
| Language / UI | Kotlin 2.2.21, Jetpack Compose (BOM 2026.06.01), Material 3 |
| AGP / Gradle | 8.13.2 / 8.14.5 |
| SDK | `minSdk 30` (Android 11), `compileSdk`/`targetSdk` 36 |
| JDK | **Java 17 required** |
| Dependencies | Compose, Navigation, Lifecycle ViewModel, kotlinx.serialization, JUnit. **No Room, no WorkManager, no Play Services** |

⚠️ **The AndroidX versions are pinned deliberately.** Newer releases (core-ktx ≥ 1.18,
compose-ui ≥ 1.12, activity ≥ 1.12) already require `compileSdk 37` and AGP ≥ 9.1 and
break the build. Raise the toolchain first; do not bump individual libraries.

### Minimum SDK

`minSdk 30` is a comfortable floor, not a hard one. The hard floor is **26**, for two
reasons: `java.time` in `GameViewModel`, and a launcher icon that exists only as
`mipmap-anydpi-v26`. Going below 26 would need core library desugaring and PNG icon
fallbacks.

⚠️ One call would have to change first: `PackageInfo.longVersionCode` in `ChessomniaApp`
only exists from API 28 and would crash on Android 8. Use `PackageInfoCompat` if the floor
is ever lowered — and note that `lint { checkReleaseBuilds = false }` means the build
will *not* warn about it.

### Screen orientation

The app does **not** pin an orientation. From `targetSdk 36` Android ignores
`screenOrientation` on displays of 600dp and wider — that is, on tablets, the target
device — so a setting for it could only have looked broken.

That costs nothing, because the layout was built for both orientations anyway: a `Column`
over the full area, the board in a weighted `BoxWithConstraints`, panels above and below.
In landscape the board simply gets smaller and the panels get wider.

⚠️ **The board's side is `min(maxWidth, maxHeight)`, stated explicitly.** It used to be
`fillMaxHeight().aspectRatio(1f)`, which derives the side from the height alone. On a
tablet in landscape the height is the smaller axis, so that happened to be right; on a
phone in portrait it is the larger one, and the board came out 608dp wide on a 344dp
screen — about 57 % of it was visible. Do not go back to deriving it from one axis.

⚠️ `ChessomniaPrefs.migrate()` has a `settings_version` chain: v3 removed the orphaned
orientation key, v4 the orphaned takeback-confirmation key. **Every future change to a
default needs the same treatment** — otherwise it only takes effect for fresh installs,
since anyone who has touched the settings already has the old value stored.

---

## The rule engine

`name.lechners.chessomnia.rules` — pure Kotlin, **no Android imports**, so it runs as an
ordinary JVM unit test (no Robolectric).

- **Board: 0x88.** `Array<Piece?>(128)`, `square = rank * 16 + file`. The off-board check
  is a single `(sq and 0x88) == 0`. Bitboards would be overkill — their benefit is search
  speed, which is never needed here — and a plain 8×8 array would need its own range
  checks in about eight places, which is exactly where off-board bugs hide.
- **Legality by make/unmake**, not analytically: make the move, test
  `isSquareAttacked(king)`, unmake it. That handles without any special case: pins, a king
  retreating along the checking line, and **en-passant pins along a rank** — the last of
  which analytical pin detection notoriously gets wrong.
- **Castling rights also expire when a rook is captured on its home square.** The single
  most common implementation bug; the "Kiwipete" perft position exposes it.
- **Queenside castling:** b1/b8 must be *empty* but may be *attacked*.
- **Promotion:** the generator emits four moves (Q/R/B/N). Consequence for the UI: if the
  move list contains more than one entry for a target square, it is by definition a
  promotion — no special case needed.
- **Repetition:** a normalised FEN string (first four fields) in a `HashMap<String, Int>`,
  no Zobrist hashing. At fewer than 300 positions per game that is about 18 KB, in
  exchange for a zero collision risk. The en-passant field enters the key only when an
  en-passant capture is actually available.
- **Insufficient material:** K–K, K+B–K, K+N–K, and K+B–K+B with same-coloured bishops.
  Deliberately **not** K+N+N–K — mate is possible there, merely not forcible.

## The opponent

`name.lechners.chessomnia.engine` — pure Kotlin, **no Android imports**, same rule as
`rules/` and for the same reason: whether an opponent actually mates can only be settled
by playing games out, and that has to run as an ordinary JVM test.

### How it reaches the board

`GameViewModel` owns an `OpponentConfig?` — the level and the colour the device plays, or
null between two people. Everything else follows from it:

- **It belongs to the game, not to the settings.** `GameSnapshot` stores it, so a restart
  cannot quietly turn a game against the device into a two-player one that simply stops
  answering. `Settings` only remembers what to preselect next time.
- **The search runs on `Dispatchers.Default`**, over a FEN and a *copy* of the repetition
  history. The game keeps being edited on the main thread; handing over the live objects
  would be a data race.
- ⚠️ **A cancelled search can still be in flight.** `engineGeneration` counts every search
  started or abandoned, and a result whose generation is stale is dropped. Without it a
  move found just before a takeback would land on the restored board.
- **Taking back undoes more than one halfmove**: it keeps going until it is the human's
  turn. Undoing only the device's reply would hand the board straight back to it.
- **`maybeStartEngine()` is called from every path that can make it the device's turn** —
  a move, a new game, a takeback, and returning to the board. It checks everything itself,
  so calling it too often is free; missing one call means a board that never answers.
- **A minimum thinking time of 350 ms.** The lower levels answer in about a millisecond,
  and a reply in the same frame as one's own move reads as a glitch rather than as an
  opponent.

### It adds code and changes nothing

`rules/` is untouched. The search walks the very same `makeMove`/`unmakeMove` that perft
hammers, so a move the engine plays is legal by the identical code the board itself uses.
Adding an opponent cannot make the app misjudge a mate.

One deliberate departure from the UI path: the search calls `pseudoLegalMoves` and does
the check test itself. `MoveGenerator.legalMoves` filters by making and unmaking every
move — the search would then make and unmake it a second time, doubling the cost of every
node. Same functions, same rules, half the work.

### Search

Iterative deepening, negamax with alpha-beta, and a quiescence search that plays out the
captures before evaluating. Move ordering is MVV-LVA for captures, then killers, then a
history table. No transposition table and no Zobrist hashing: that would mean either
mirroring `makeMove`'s logic incrementally — a second source of truth for the rules — or
hashing from scratch at every node. Neither is worth it at the strength being aimed for.

Two consequences of that choice, both deliberate:

- **Repetition is only tested at the root**, where the game's actual `RepetitionTracker`
  is available. That is enough to stop the visible failure — a winning engine shuffling
  into a threefold draw — without a hash inside the search.
- **Every root move is searched with a full window**, so all root scores are exact and
  comparable. That is what lets the difficulty setting pick a deliberately imperfect move
  without a second search pass. It costs depth, which a family-strength opponent can
  spare.

### Evaluation

Material, piece-square tables blended between a middlegame and an endgame set, pawn
structure (doubled, isolated, passed), the bishop pair, and rooks on open files. They
encode the handful of principles a beginner is taught and nothing more.

⚠️ **The piece-square tables are generated, not hand-tuned.** `tools/generate_pst.py`
derives every one of them from a principle written down in that file — a knight's actual
mobility on an empty board, a pawn's distance from promotion, a king's distance from the
centre — and `--check` verifies that what stands in `Evaluation.kt` still matches the
derivation. Do not edit the numbers by hand.

The reason is licensing as much as tidiness, and it is worth stating plainly because an
earlier version of this file claimed the opposite. Piece-square tables are the
most-copied artefact in computer chess, and the tables that shipped up to version 1.3.0
*were* derived from the best-known set — Michniewski's *Simplified Evaluation Function* —
by rescaling it. The queen table gave it away: it matched cell for cell under a single
value substitution, reproducing even the two asymmetries generally taken for typos in the
original. That set is published on a CC BY-SA 3.0 wiki, and share-alike is not something
an Apache-2.0 project can satisfy. Generating the tables removes the question rather than
arguing it; see NOTICE for what remains and why it is fine.

The piece values (100 / 320 / 330 / 500 / 900) are deliberately **not** regenerated. They
are the standard set every engine and every textbook uses, they are five short numbers
dictated by the game rather than by anyone's expression, and changing them for the sake of
appearances would make the engine worse for no gain.

⚠️ **The bare-king term is load-bearing.** Once one side is down to a lone king, material
and tables say the same thing about every move, and nothing points anywhere. Without a
term that walks the weak king to the edge and brings the strong king up, the engine wins
the queen and then shuffles until the fifty-move rule. `GamePlayTest` plays those endings
out precisely because a mate-in-one test would never catch it.

### Strength is a window, not a depth

⚠️ Difficulty is **not** set by search depth alone. A depth-two engine sees two moves
perfectly and then hangs a rook for no reason, which reads as broken rather than as
beatable. The knob is `Level.windowCp`: the search scores every root move exactly, and the
engine picks at random among those within that many centipawns of the best, capped at
`maxCandidates`. A human settles for the second-best idea; it does not choose uniformly
among thirty legal moves. Quiescence stays on at every level — it is what stops the engine
from giving pieces away in a way that looks like a bug.

Two situations override the window entirely:

- **A forced mate**, given or received, is always played best-first. Losing a mate in one
  to a dice roll makes an opponent feel random rather than weak.
- **A bare-king endgame** is converted at full strength. The tolerance would swamp the
  mating gradient, and a beginner should lose to a mate — being handed a draw because the
  opponent could not finish reads as a broken app, not a weak one.

  ⚠️ "Full strength" has to mean the **depth** as well, not only the move choice. Until
  this was fixed, `BEGINNER` drew king and queen against a bare king *every single time*:
  at depth two it followed the mating gradient, ran out of sight one move before the net
  closed and shuffled the queen until the position repeated. `Engine` therefore searches
  a bare-king ending to at least `MATING_DEPTH` (four) whatever the level says. The
  ending is nearly branchless, so the extra plies cost a few milliseconds. A difficulty
  setting has no business deciding whether the app can finish a game it has already won.

### What a window around the best move cannot do

⚠️ A window around *the best move* has a floor below which it cannot go, however wide it
is opened, and that floor was still far too strong for a nine-year-old. A free queen is
worth nine pawns more than every alternative, so no tolerance ever reaches the second
move: `BEGINNER` took the hanging queen in **100 %** of tries. A child leaves a piece
hanging several times a game, so the opponent never had to play well — it only had to
accept the presents, and it won every game by a distance.

`Level.LEARNING` therefore measures the same tolerance from a different reference: not
from the best move found, but from the **standing evaluation** — what the position is
worth before anybody does anything clever. The consequence is exactly the one wanted:

- The free queen no longer stands out. Taking it is one of thirty moves that do not make
  the engine's own position worse, so it is picked about as often as any other: **3 %** of
  tries in the same position where `BEGINNER` is at 100 %.
- Handing over a piece is still excluded, because *that* does make its position worse.
  The window is one minor piece (`Evaluation.KNIGHT`, 320 cp), written as that rather
  than as a number: it will let a pawn go, trade a knight for a pawn and now and then
  drop a knight, but a rook for nothing is out of reach of the dice. Careless, not
  suicidal.

`minOf(best, standing)` is the reference, and the minimum is load-bearing. When every move
is bad — a trapped queen, a threat that must be answered — the standing evaluation sits
above all of them and would leave no candidate at all; falling back to the best score
there means the engine still defends as well as it can see.

Measured over twelve games against a deliberately blundering stand-in opponent,
`BEGINNER` finishes about **15 pawns** ahead, mates all twelve and is never mated;
`LEARNING` finishes about **4 pawns** ahead, mates seven and is mated twice. The
stand-in is a crude model of a child rather than a child, so those figures calibrate
rather than prove. What is asserted in the suite is the relative claim:
`GamePlayTest.theWeakestLevelIsMateriallyWeakerThanTheNextOneUp` plays the two levels
against each other, because a level that merely *said* it was weaker would be worth
nothing.

### The clock is a ceiling, not a promise

The first iteration ignores the time budget. However slow the device, a search that has
not finished depth one holds nothing but zeroes and would make the engine play a random
legal move. One slow move is a far better failure than a nonsensical one.

### Measured

On a Raspberry Pi 5 — comparable to a mid-range tablet — after JIT warm-up, roughly
450–750 thousand nodes per second:

| Level | Opening | Middlegame (Kiwipete) | Endgame |
|---|---|---|---|
| `LEARNING` | depth 2, 1 ms | depth 1, 200 ms | depth 2, 1 ms |
| `BEGINNER` | depth 2, 1 ms | depth 1, 200 ms | depth 2, 1 ms |
| `CASUAL` | depth 4, 20 ms | depth 2, 500 ms | depth 4, 23 ms |
| `CLUB` | depth 6, 666 ms | depth 4, 1200 ms | depth 6, 454 ms |

Kiwipete is a deliberately sharp test position, not a typical middlegame; it is the worst
case rather than the average. Reproduce with:

```bash
./gradlew testDebugUnitTest --tests "*EngineBenchmark*" -DengineBench=1
```

## The clock — counts upward, never ends the game

The clock measures **how long each player has thought across all of their moves**. There
is no base time, no expiry and no win on time: at a home board a clock should inform, not
adjudicate. `GameStatus` accordingly has **no** time-based outcome.

A monotonic base rather than a counter: elapsed time is always *computed* as
`accumulated + (now − startedAt)`, where `now` is `SystemClock.elapsedRealtime()`. Doze,
app switching and rotation are therefore correct for free; the 200 ms tick only triggers a
redraw and has nothing to verify.

`ClockState` does not know `SystemClock` — every method takes `nowMs`. That is precisely
why the clock is fully testable against an invented timeline (`ClockStateTest`).

The clock belongs to the **game** (`ChessGame.clock`), not to the UI: only that way does a
takeback restore position and thinking time in one step. `MoveRecord` carries the clock
reading from *before* the move.

It does not start by itself — "start clock" or the first move sets it going. It switches
only as a consequence of a move; there is no tapping the clock, because the tablet *is*
the board. It runs while the board is visible: resumed when the game screen is entered,
paused when it is left, so that no time accrues in the menu. Auto-resume is harmless
precisely because nothing can expire.

**A takeback also returns the thinking time** spent on the move being taken back — that is
how a takeback works among friends. Afterwards the clock is **paused**, so nobody loses
time during the ensuing discussion.

**Compose detail with real effect:** the clock has its *own* `StateFlow`, and `ClockView`
collects it itself. If the time lived in the board state, the 64-square canvas would
redraw five times a second for no reason.

## Persistence

`ChessomniaPrefs` on SharedPreferences, **not** DataStore: this is read *synchronously* at
startup, so the ViewModel can restore the position in its constructor and the very first
frame already shows the game. DataStore is flow-based and would force an empty board or a
loading state for one frame, without gaining anything at this data volume. `apply()`
writes off-thread, so there is no ANR risk when saving after every move.

What is stored is **the starting position plus the move list**, not a position snapshot:
loading replays the moves, which makes the undo stack and the repetition history correct
by construction.

⚠️ **Resignation and an agreed draw do not follow from the move list** and are stored
separately as a `result` code (`GameSnapshot.encodeResult`). Mate, stalemate, the
fifty-move rule, repetition and dead material all re-emerge from the replay and are
therefore deliberately *not* in the snapshot. Any new outcome that cannot be replayed has
to be added here.

The clock always comes back **paused**: `elapsedRealtime()` resets when the device
reboots.

`GameSnapshot.v` distinguishes the meaning of the stored clock: **v1** stored the
*remaining* time of a counting-down clock, **v2** the *accumulated* thinking time. v1
readings are discarded on load rather than misread as thinking time, and old `TIMEOUT_*`
results are silently dropped — the game then simply continues.

## Only the player to move may select

Tapping a piece belonging to the player who is **not** to move is ignored entirely — an
existing selection even stays in place. An earlier version showed the opponent's moves
dimmed ("why can't I go there?"); that was deliberately removed. The order in
`onSquareTap` matters: the check comes **after** the search for valid capture targets,
otherwise no enemy piece could ever be taken.

## The menu does not end the game

Switching to the main menu is **not** the end of a game. It lives on in `GameViewModel`
and is additionally persisted; the menu offers **"Resume game"** and **"Start a new
game"** separately, the latter with a confirmation while a game is running.

The game screen has a **new-game button** of its own, also behind a confirmation that
names how many moves are about to be lost.

⚠️ This reverses an earlier decision, which held that there should be no such button
because "ending and restarting should be explicit steps". What changed is the reasoning
around it: with resigning and offering a draw gone (see below), starting again is the
*only* way to end a game, and routing that through the menu made the most ordinary
action the most awkward one. It is still a deliberate step - it just no longer requires
leaving the board first.

The clock is **paused automatically** when the game screen is left (`DisposableEffect` in
`GameScreen`), otherwise thinking time would accrue in the menu.

### No resigning, no draw offers

Both were removed. The app records nothing - no move list, no history, no rating - so how
a game ends makes no difference to it; whoever wants to stop starts a new one.

`GameStatus.Resigned` and `GameStatus.AgreedDraw` nevertheless remain in the rule model,
together with their codes in `GameSnapshot`. A game saved by an older version can still
carry them and has to load. `ChessGame.restoreResult()` is the only way in, and
`ChessGameTest` covers exactly that path.

### The pause curtain

Pausing the clock with the button covers the board: the pieces disappear and a large
pause symbol takes their place. Tapping it - or pressing back - resumes. The point is
that the position cannot be studied while the clock is stopped.

The symbol is deliberately a symbol and not a word: it has no orientation and therefore
reads the same from both sides of the table. The word "Paused" is there too, but twice,
once for each seat - the same reasoning that gives every panel a twin.

⚠️ The curtain is tied to an **explicit** pause (`GameViewModel.pauseByUser`), not to the
clock merely being stopped. The clock also stops after a takeback and when the screen is
left, and covering the board then would be wrong: a takeback is followed by precisely the
discussion in which both players want to look at the position.

## Settings

⚠️ A setting has to *do* something. "Confirm takeback" did not: it was written to the
preferences and shown in the UI, but no code ever read it, so the confirmation appeared
either way. It is now **"Allow taking moves back"**, which hides the undo button when
switched off — a switch that offers or withholds the feature rather than one that only
governs a prompt.

The old value is deliberately **not** carried over by migration v4: switching a prompt off
is not the same as giving up the feature, so translating one into the other would put
words in the user's mouth. Everyone starts with takebacks allowed, which is the old
effective behaviour anyway.

## Back navigation

`BackHandler` in two places, and the order matters. `GameScreen` claims back while
something is on top of the board - the curtain, a confirmation, the promotion choice -
and closes that first. `MainActivity` claims it for everything except the home screen and
walks one step up: licences to settings, everything else to home.

Without these the activity handles back itself and simply quits, which is what the app
used to do from every screen.

---

## Reporting a problem

`BugReportButton` (in the main menu and in the settings) asks for a description and pushes
the finished report into any app via `Intent.ACTION_SEND`. Deliberately **no** transport of
its own — no upload, no mail: the report is plain text, and where it goes is decided by the
user in the share sheet.

The report contains everything needed to reproduce: starting position, **all moves in long
algebraic notation**, the current FEN, an ASCII board, status, halfmove clock, thinking
time, app version and device model.

⚠️ The most important field is **"legal moves in this position"**. For the classic report
"the mate was not detected" it states in black and white which moves were still available —
a non-empty list means it was not mate. Without that field every such report would be
guesswork. `BugReportTest` covers exactly this case with a position that looks like mate
but whose king can escape.

`BugReport.compose()` is a pure function with no Android dependency and therefore fully
unit-testable; the report is built when it is shared, not when the dialog opens. Its text
is always English, independent of the UI language — it is addressed to the developer, and a
translated version would need a `Context`, which would cost both the pure function and its
testability.

---

## How rule correctness is ensured

Three independent layers — deliberately, because a hand-written test only checks a
hand-written engine against its author's own assumptions:

1. **Perft** (`PerftTest`) against the six published standard positions. Depth 3 on every
   build, depth 4–5 behind `-DperftDeep=1` (12.4 million nodes). This exhausts move
   generation.
2. **Cross-check against python-chess** (`ReferenceCorpusTest`). `reference_positions.txt`
   holds 4,576 positions with their full move list and game status, produced by a
   *foreign* implementation (`tools/generate_reference_corpus.py`). Deliberately dense
   around endings: **756 mates, 420 stalemates, 400 dead positions, 29 fifty-move draws**.
   Random games alone barely reach mate, and without mate positions the corpus would not
   answer the question that matters.
3. **Invariants over random games** (`MateDetectionTest`). After every halfmove the
   tracked king square is checked against a board scan and `isInCheck` against a
   brute-force test. This targets the one bug that would silently turn a mate into a
   *stalemate*.

⚠️ When regenerating the corpus: python-chess does **not** report the fifty-move rule by
itself (under FIDE it is claimable, not automatic), whereas Chessomnia declares the draw at
100 halfmoves. `status_of()` in the generator mirrors that — without it the comparison
reports false differences.

A by-product of introducing these layers: of ten *hand-written* "mate positions" in
`MateDetectionTest`, **five were not**. The engine had been right every time. Every
position there is now cross-checked — hand-written test data is the least reliable source
in chess.

---

## Localisation

The UI ships in **English (default) and German**. Strings live in
`res/values/strings.xml` and `res/values-de/strings.xml`; keys and order are kept
identical, because a missing key falls back silently to English and then shows up as a
stray English line mid-sentence.

Text that is chosen by non-composable logic — the reason a game ended, the running status
line, piece names — is returned as a **resource id**, not as a finished string. `ResText`
in `PlayerPanel.kt` carries an id plus, where needed, a single argument that is itself a
resource ("Checkmate — White wins"). That keeps those functions pure and unit-testable:
the tests assert on ids, not on translated text.

---

## Assets and licences

There is **not a single raster image, font file or audio file** in the project — everything
is hand-written or script-generated vector XML. The usual stock-image and icon-set problems
therefore do not arise.

The code is under **Apache-2.0** (`LICENSE`, `NOTICE`). Full third-party licence texts are
in `licenses/`; the version shown inside the app is
`app/src/main/res/raw/licenses.txt`.

### Wordmark

`res/drawable/logo_wordmark.xml` is generated by `tools/generate_wordmark.py` from the
**Montserrat** typeface (Julieta Ulanovsky et al., SIL OFL 1.1); an SVG version for store
graphics lives in `branding/chessomnia-wordmark.svg`. Gradient **#2B303E → #3193C6** — the
app palette (`ui/theme/Color.kt`). The **board colours** are deliberately independent of it
and stay classical (`#F0D9B5` / `#B58863`).

The **E has no vertical stem** (CH≡SSOMNIA). Its three bars are derived from the contour of
the real Montserrat E, so they keep the stroke weight, the heights and the differing bar
lengths of the typeface; the advance width is unchanged, so the rest of the wordmark sits
exactly as in plain typesetting.

Only the outlines go into the app — the font file itself is not redistributed. Under the
OFL, a document set with a font is not itself subject to the OFL.

⚠️ On a dark background the left end of the wordmark disappears (the start colour #2B303E
is itself nearly black). It needs a lighter carrier or has to be lightened.

### App icon and mark

`res/drawable/ic_launcher_*.xml` and `res/drawable/logo_mark.xml` are generated by
`tools/generate_app_icon.py`: a **queen in #3193C6 beside a king in #FFFFFF**, on #2B303E.
Both outlines are hand-placed original work — not the Cburnett board set. The wordmark
itself is unusable as an icon at 9.9 : 1.

Three files come out of one source:

| File | Purpose |
|---|---|
| `ic_launcher_foreground.xml` | adaptive icon, foreground layer, 108-unit grid |
| `ic_launcher_monochrome.xml` | themed icon (Android 13+), single colour |
| `logo_mark.xml` | the same artwork cropped to its bounding box, shown on the home screen above the wordmark |

Two constraints shaped the composition, and both are easy to undo by accident:

⚠️ **Placement is fitted, not eyeballed.** The transforms are baked into the path data, and
the group is sized so its corners land on the **72 dp circle** around the centre — the
region every launcher mask reveals. The 66 dp circle in the platform documentation is the
stricter "key content" guidance; the outer ends of the two base bars fall marginally
outside it, so the bases, and nothing else, are what a round mask may shave.

⚠️ **The two pieces must not overlap.** Overlapping looks richer in colour, but the themed
icon flattens both to one colour, and there the silhouettes fuse into an unreadable blob.
The gap between queen and king is therefore load-bearing, not a spacing preference.

### Pieces

The **Cburnett** Staunton set (Wikimedia Commons, the standard set used by Lichess),
converted with `tools/svg_to_vectordrawable.py`. 12 files (6 types × 2 colours), **not**
6 + tint: "white fill with a dark outline" versus "dark fill with a light outline" cannot
be produced cleanly from one asset with `android:tint`.

The author offers the set **multi-licensed** — GFDL 1.2+, CC BY-SA 3.0, BSD 3-Clause and
GPL 2+ — with an explicit free choice. Chessomnia elects **BSD 3-Clause**: attribution
remains required, but share-alike does not apply.

⚠️ Changing that election means changing **three** places: the header in the twelve
`piece_*.xml`, the header generator in `tools/svg_to_vectordrawable.py`, and
`res/raw/licenses.txt` (plus `licenses/`).

### The licence file must stay reachable

`res/raw/licenses.txt` is read by `ui/about/LicensesScreen.kt` and shown under
Settings → About → "Show licenses".

⚠️ **That reference is not merely display, it is the reason the file ships at all.**
`isShrinkResources = true` removes every resource no code refers to. Before this screen
existed, the licence file was in **no** released APK — provable as
`@raw/licenses : reachable=false` in `build/outputs/mapping/release/resources.txt`.
`res/raw/keep.xml` is a second safeguard, but it does not replace the screen: a file that
ships but cannot be reached satisfies no attribution requirement.

Check after any change to the UI:

```bash
grep "raw/licenses" app/build/outputs/mapping/release/resources.txt   # reachable=true
```

The file name inside the APK is not `licenses.txt` but something like `res/1w.txt`, because
resource paths are shortened. Searching by name misleads; searching by size (4,277 bytes)
does not.

### Test corpus

`app/src/test/resources/reference_positions.txt` is generated with **python-chess**
(GPL-3.0-or-later). The corpus is plain chess fact (positions and the moves legal in them),
hence not a derivative work of the library's code, and it lives in `src/test` and never
reaches the APK. Details in `tools/README.md`.

---

## Pitfalls

| Topic | Detail |
|---|---|
| **Java 17 required** | Newer JDKs crash the Kotlin compiler in this configuration. |
| **AndroidX versions are pinned** | See the note under *Technical frame*. Bumping one library alone breaks the build against `compileSdk 36`. |
| **R8 + kotlinx-serialization** | Keep rules are in `proguard-rules.pro`. A missing keep only shows up in the release APK — that is, in the one that ships. Build `assembleRelease` before releasing, not just `assembleDebug`. |
| **`Side`, not `Color`** | In the rule engine White/Black is `Side`. `Color` would collide with `androidx.compose.ui.graphics.Color` and force an import alias in every UI file. |
| **0x88 stays internal** | Conversion to screen rows happens exclusively in `BoardGeometry`. Compose never sees 0x88 arithmetic. |
| **A board canvas, not 64 composables** | One `Canvas` with a single geometry source for hit testing, markers and pieces. 64 children would split hit testing and overlays between child and parent, and those drift apart. |
| **Edge-to-edge** | `targetSdk 36` enforces it. `enableEdgeToEdge()` plus `Modifier.safeDrawingPadding()` are set; without them content slides under the status and navigation bars. |
