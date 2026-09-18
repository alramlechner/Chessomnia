# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); this project
uses [Semantic Versioning](https://semver.org/).

## [1.3.3] — 2026-09-18

### Added
- **Spanish.** The app, its licences page and the Play Store listing are now
  available in Spanish alongside English and German.

## [1.3.2] — 2026-09-13

### Added
- **The device's move can be watched arriving.** Its piece slides across the board
  instead of appearing, and a piece it takes no longer vanishes: it stays put until
  the piece taking it has arrived, then floats out to the edge of the board, growing
  on the way, and waits there for ten seconds before fading.

  Both answer the same complaint — that a capture by the device was over before it
  was noticed. The slide catches the eye in the moment; the piece waiting at the edge
  is for looking up a moment too late, which an animation alone cannot do. It rests
  in the free space beside the board, so it covers no square.

  Moves made by a hand at this table are deliberately left alone. They were watched
  being made, and sliding them would only put a delay between a tap and its answer.

## [1.3.1] — 2026-09-12

### Added
- **An optional opponent.** The home screen now offers the choice directly:
  *Two players* or *Play the device*, the latter with four strengths and a choice
  of colour. The board turns to your colour, and the device's panel is not rotated
  — nobody is sitting at that edge.

  Both ways of starting discard a game in progress, so both ask first. The strength
  and colour are only requested once the running game has actually been given up.

  The engine is written for this app and built on the existing rule engine
  unchanged, so a move it plays is legal by the identical code the board itself
  uses. Alpha-beta with a quiescence search; no bundled third-party engine, no
  network, no new permission, and the project stays Apache-2.0.

  Strength is deliberately capped at a good club amateur. It is set by a
  tolerance around the best move rather than by search depth alone — a
  depth-limited engine sees two moves perfectly and then hangs a rook, which
  reads as broken rather than as beatable. Two things override the tolerance: a
  forced mate is always played, and a bare-king endgame is converted at full
  strength. Being handed a draw because the opponent could not finish would read
  as a broken app, not a weak one.

  The weakest strength, *Learning the moves*, is for a child of nine to eleven.
  It is not simply a smaller tolerance: a window around the best move can never
  go below a certain strength, because a free queen is worth nine pawns more than
  every alternative and no tolerance ever reaches the second move. That level
  measures its tolerance from the position it is standing in instead, so taking
  the queen is merely one of thirty moves that do not make its own position
  worse — it walks past most of what is left hanging, while still not handing
  over a piece itself.

  Taking a move back against the device undoes both halfmoves, so it is your turn
  again. The opponent is stored with the game, so a restart does not silently
  turn it back into a two-player game.
- The device's panel carries a CPU-chip badge where a human's panel has its
  action buttons. The headline already says "device", but that is easy to miss
  mid-game.

### Changed
- **"Report a problem" now suggests `chessomnia@lechners.name`** as the
  recipient via `EXTRA_EMAIL`, for whichever mail app is picked from the share
  sheet. It is a hint the receiving app may prefill, not a fixed destination —
  every other app the share sheet offers ignores it, and the report itself is
  still built and sent the same way as before.
- The German name for the weakest opponent strength is now "Schach lernen"
  (was "Lernt die Züge").
- **The store listing and README now describe the app that exists.** Both still
  led with "it is NOT a chess computer — there is no engine, no opponent to play
  against", which was true when it was written and became exactly wrong when the
  opponent shipped. The positioning is now the four things that actually
  distinguish it: free, ad-free, registration-free and no tracking.

  What did not change is the line that used to sit beside it. There is still no
  evaluation bar and no move suggestion; the opponent plays and says nothing
  about your position. Those were always two separate decisions and only one of
  them was reversed.
- **The piece-square tables are now generated from stated chess principles**
  (`tools/generate_pst.py`) instead of being hand-tuned numbers. Nothing about
  how the app plays was meant to change; the tolerance of the weakest strength
  was re-measured against the new scale and is now stated as what it always
  meant — one minor piece.

  The reason is provenance. The tables that had been in the file were derived
  from the best-known published set by rescaling it, closely enough that the
  queen table matched cell for cell under a single substitution, and that set
  is published under a share-alike licence this Apache-2.0 project cannot
  satisfy. Generated tables have a history that can be re-run and checked
  rather than asserted. `NOTICE` records what the situation was and why what
  remains — the five standard piece values — is fine.

### Fixed
- **The king was invisible on the home screen in the light theme.** The mark is
  generated from the same source as the launcher icon and had inherited its
  colours — where a white king is correct, because the launcher draws it on its
  own navy background layer. The home screen has no such layer: the mark sits
  directly on the theme surface, so a white king on a light surface came out at
  a contrast of 1.05:1. Reported by a tester on 1.1.4.
- The wordmark had the same defect in the dark theme, in the other direction:
  its gradient starts at navy `#2B303E`, which is 1.32:1 against the dark
  background, so the first letters of CHESSOMNIA faded out. Found while fixing
  the king — a fixed colour cannot serve both themes.

  Both marks now have a `drawable-night/` variant, and their generators write
  both files in one run rather than leaving the second to be copied by hand. The
  `-night` qualifier keys off `UI_MODE_NIGHT`, the same signal `ChessomniaTheme`
  reads through `isSystemInDarkTheme()`, so the artwork cannot drift away from
  the theme that picks it.

### Changed
- Texts that promised the app had no opponent have been corrected: the home
  screen tagline and footer, and the *About* section in the settings. The footer
  now names what still holds — no ads, no account, no internet.

  ⚠️ The Play Store listings under `store/` and the project page in `docs/` still
  say "not a chess computer". They are deliberately left for whoever publishes the
  release, so that the repository does not describe the app differently from what
  is live in the store. *(Both were rewritten later in this same unreleased
  batch — see "The store listing and README now describe the app that exists"
  above. Nothing has been published from either state yet.)*
- Starting a new game from the player panel no longer asks for confirmation when
  no game is in progress — that is, once the current one is decided, or before
  anyone has moved. The question was worth asking mid-game and misleading
  otherwise: it says "the current game with 41 moves will be lost", when the game
  has in fact already ended and nothing is lost by leaving it. The home screen has
  always behaved this way; only the in-game button asked regardless.

## [1.1.4] — 2026-09-01

First public release, prepared for Google Play. Development up to this point happened in a
private repository.

### Added
- English and German localisation. English is the default; the UI previously existed only
  in German.
- An **About → Show licenses** screen that displays the full third-party attribution.
- The mark now also appears on the home screen, above the wordmark.
- **Settings → About → Source code on GitHub** — the app had no link to its own
  project page. Opening it hands the address to the browser; the app still makes
  no request of its own and holds no `INTERNET` permission.
- A German version of the licences screen (`res/raw-de/licenses.txt`). Until now a
  German user tapped a German button and landed on an English page. The licence
  texts themselves stay in English: they are the legally binding wording.
- A `TranslationParityTest` that fails the build when English and German drift
  apart — missing keys, mismatched format placeholders or differing plural
  quantities. Lint's own `MissingTranslation` check cannot do this here, because
  `checkReleaseBuilds = false` stops it running on release builds at all.
- `RELEASING.md`, and automated publishing to Google Play via the Gradle Play
  Publisher plugin. A bare `publishBundle` targets the internal track; production
  needs an explicit flag.
- A small GitHub Pages site under `docs/`, generated from `PRIVACY.md` by
  `tools/render_pages.py` so that the page Google Play links to cannot drift from
  the document in the repository.

### Fixed
- Clock, bug-report timestamps and durations are formatted with `Locale.ROOT`
  instead of the device's formatting locale. On a device using another numbering
  system the clock would have shown non-Latin digits while the rest of the UI had
  already fallen back to English, and the bug report — which documents itself as
  always being English — would not have been.

### Changed
- The descriptions no longer say "tablet" where any Android device will do. The app
  is designed around a tablet lying flat on the table, but it runs on a phone from
  Android 11 onwards — and a store listing that says "tablet" loses the reader who
  is holding a phone.
- Dropped the "if you want an opponent, this is the wrong app" line from the store
  listings and the README. Setting the expectation is right; telling the reader they
  are in the wrong place is not.
- Minimum Android version lowered from 15 to **11** (`minSdk` 35 → 30). The app never used
  any Android 15 API.
- `targetSdk` raised to 36 (Android 16), with the toolchain moved to AGP 8.13 / Gradle
  8.14 / Kotlin 2.2.
- The screen-orientation setting was removed. From `targetSdk` 36 Android ignores
  orientation locks on displays of 600dp and wider — that is, on the tablets this app is
  built for — so the setting could only have looked broken. The layout was designed for
  both orientations from the start.
- **New app icon.** The previous one was a bare knight's head, which without a pedestal
  reads as an animal rather than as a chess piece. It is now a queen beside a king,
  generated by `tools/generate_app_icon.py` — including the themed-icon layer for
  Android 13+.
- The privacy policy and both store listings no longer claim the app "requests no
  Android permissions at all". It requests one, `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`,
  declared by AndroidX for the app about itself; it grants nothing. The documents
  now say that precisely instead of generously.
- The wordmark is now generated from the Montserrat font by `tools/generate_wordmark.py`,
  giving it a complete and reproducible provenance.
- The chess pieces now elect the **BSD 3-Clause** option of Cburnett's multi-licensed
  Staunton set instead of CC BY-SA 3.0, which removes a share-alike obligation on the
  artwork without losing the attribution.

### Fixed
- The third-party licence file was silently dropped from every release build by the
  resource shrinker, because no code referenced it. It is now shown in the app, which both
  fixes the attribution and keeps the shrinker from removing it.
