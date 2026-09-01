# Google Play listing — English (default)

## App name (30 characters max)

```
Chessomnia
```

## Short description (80 characters max)

```
A chess board for two players. No engine, no ads, no tracking. Fully offline.
```

*(77 characters — the limit is 80.)*

## Full description (4000 characters max)

```
Chessomnia turns your tablet or phone into a chess board. Two players, one device, sitting opposite each other — exactly as you would at a real board.

It is NOT a chess computer. There is no engine, no opponent to play against, no evaluation bar and no move suggestions. Chessomnia knows the rules, not strategy. That is a deliberate decision, not a missing feature.


FREE, AD-FREE, TRACKING-FREE

No advertising. No analytics. No account. No in-app purchases. Nothing to unlock.

The app holds no Android permission that grants it anything — not even internet access — so it is technically incapable of sending anything anywhere. Your games never leave your device. You can verify that yourself: Chessomnia is open source.


WORKS COMPLETELY OFFLINE

There is nothing to connect to. On a plane, in a cellar, in a tent — it makes no difference. The app has no server side at all.


NOT A CHESS COMPUTER — A REPLACEMENT FOR THE BOARD

The point is the two people at the table. Chessomnia takes over exactly the jobs a wooden board cannot do:

• It knows every rule, including castling, en passant, promotion, the fifty-move rule, threefold repetition and dead positions.
• It never lets an illegal move through, and it never misses a checkmate.
• It keeps the pieces where you put them when the device goes to sleep.


A LEARNING AID, NOT A TEACHER

Tap a piece and every square it may legally move to is marked — with castling and en passant highlighted separately, because those are the moves beginners overlook. Switch the hints off once you no longer need them.

What you will never get is a suggestion of which move to play. Working that out is the game.


BUILT FOR A DEVICE LYING FLAT ON THE TABLE

A tablet suits it best. A phone works just as well — the board is smaller, nothing else changes.

Everything follows from that one idea:

• Clock, status and buttons exist twice, once at each player's edge, so neither of you is reading upside down.
• The pieces of the player sitting opposite are drawn rotated, just as the three-dimensional shape of a real piece does the job at a wooden board.
• "Turn the board" rotates it in place, instead of somebody having to pick the device up.
• Prompts and the promotion dialog turn to face whoever triggered them.
• When a game ends, nothing covers the board. After a mate you want to see why — the highlighted king and the marked last move are the answer.


A CLOCK THAT INFORMS RATHER THAN JUDGES

The clock counts upward: it shows how long each player has thought in total. It never runs out, and it never ends a game. At a home board that is what a clock is for. Switch it off entirely if you prefer.

Take a move back and the thinking time comes back with it — the way a takeback actually works among friends.


CORRECTNESS YOU CAN CHECK

The rules engine is verified against 12.4 million positions of standard perft tests and cross-checked against an independent implementation across 4,576 positions, deliberately dense around checkmates and stalemates.

If it ever does get something wrong, the built-in "Report a problem" produces the exact text needed to reproduce it — including every move that was legal in the disputed position.


OPEN SOURCE

Apache-2.0. Read it, build it, fork it:
https://github.com/alramlechner/Chessomnia
```

## Categorisation

| Field | Value |
|---|---|
| App or game | Game |
| Category | Board |
| Tags | Chess, Board game, Two player |
| Contains ads | No |
| In-app purchases | No |
| Target audience | 13+ (avoids the additional Families-programme requirements; the app has no content concerns at any age) |
| Content rating | Complete the questionnaire — no violence, no user interaction, no data collection, no purchases |

## Data safety declaration

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | n/a — nothing is transmitted |
| Do you provide a way for users to request that their data is deleted? | n/a — nothing is collected |

Supporting evidence, should a reviewer ask: the release manifest requests no
permission that grants the app any capability — the single `uses-permission`
line is `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, a signature-level
permission AndroidX declares for the app about itself, so that it can register
a receiver as not-exported. There is no `INTERNET` permission, and the app
contains no analytics, advertising or crash-reporting SDK.

## Privacy policy URL

```
https://github.com/alramlechner/Chessomnia/blob/main/PRIVACY.md
```

## Assets

| Asset | File | Status |
|---|---|---|
| App icon, 512×512 PNG | `store/play-icon-512.png` | ready |
| Feature graphic, 1024×500 PNG | `store/play-feature-1024x500.png` | ready |
| Phone screenshots (2–8, min 320px) | — | **must be taken on a device** |
| 7" tablet screenshots (up to 8) | — | **must be taken on a device** |
| 10" tablet screenshots (up to 8) | — | **must be taken on a device** |

Screenshots are deliberately not generated from the vector sources: Google Play
requires them to show the real app, and a rendered mock-up would misrepresent
it. Suggested set, in this order:

1. The board mid-game with legal moves shown for a selected piece.
2. Checkmate — both panels showing the result and the reason, board uncovered.
3. The promotion dialog, rotated towards the player who triggered it.
4. The home screen.
5. Settings.
