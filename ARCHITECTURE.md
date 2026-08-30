# Chessomnia — Architecture

A chess board for Android tablets. **It replaces the physical board** — two people play
each other on one device.

**Explicitly not a chess computer:** no virtual opponent, no position evaluation, no move
suggestion. The app knows the rules, not strategy.

Its second purpose is learning: tap a piece and every legal move for it is marked,
including the special moves — castling, en passant, promotion — that beginners overlook.

---

## Product decisions

| Point | Decision |
|---|---|
| Setup | **The tablet lies flat on the table and the players sit opposite each other** — as at a real board |
| Board orientation | **Fixed, White always at the bottom.** No auto-rotation, no flip flag |
| Learning aids | Mark legal moves · check/mate/stalemate · take back a move |
| Deliberately absent | Move list / SAN notation, PGN export, any form of engine analysis |
| Clock | **Counts upward, never expires.** Fully switchable |
| Pieces | Classic Staunton as VectorDrawables |
| Extras | Resign / agree a draw · the game survives an app restart · hints switchable · the screen stays on |

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
over the full area, the board in a `Box(Modifier.weight(1f))` with
`fillMaxHeight().aspectRatio(1f)`, panels above and below. In landscape the board simply
gets smaller and the panels get wider.

⚠️ `ChessomniaPrefs.migrate()` has a `settings_version` chain. Step v3 removes the orphaned
orientation key. **Every future change to a default needs the same treatment** — otherwise
it only takes effect for fresh installs, since anyone who has touched the settings already
has the old value stored.

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
game"** separately, the latter with a confirmation while a game is running. There is
deliberately **no** "new game" button on the game screen — even the game-over state only
leads back to the menu. Ending and restarting should be explicit steps.

The clock is **paused automatically** when the game screen is left (`DisposableEffect` in
`GameScreen`), otherwise thinking time would accrue in the menu.

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

### App icon

`res/drawable/ic_launcher_*.xml` — a **white knight silhouette on #2B303E**, a single
hand-drawn path and therefore original work. The wordmark itself is unusable as an icon at
9.9 : 1. The motif sits, via `<group>` scaling, inside the inner ~66 % of the 108-unit
grid; round launcher masks cut away everything outside that.

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
