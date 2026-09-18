# Google Play listing — English (default)

## App name (30 characters max)

```
Chessomnia
```

## Short description (80 characters max)

```
Chess for two, or against the device. Free, no ads, no account, no tracking.
```

## Full description (4000 characters max)

```
Chessomnia turns your tablet or phone into a chess board. Two players, one device, sitting opposite each other — exactly as you would at a real board. And when nobody else is around, the board plays: four strengths, from one a nine-year-old can beat to a good club amateur.


FREE. NO ADS. NO ACCOUNT. NO TRACKING.

Those four are the point, and none of them has an asterisk.

Free means free — not a trial, not a lite version, nothing to unlock, no in-app purchases. No advertising and no analytics. No account, no sign-up, no email address: you install it and you play.

And the tracking claim is not a promise, it is a property. The app holds no Android permission that grants it anything — not even internet access — so it is technically incapable of sending anything anywhere. Your games never leave your device. You can verify that yourself: Chessomnia is open source.


WORKS COMPLETELY OFFLINE

Nothing to connect to. On a plane, in a cellar, in a tent — no difference. The app has no server side at all, and the opponent runs on your device rather than somewhere else.


A REPLACEMENT FOR THE BOARD

The point is the two people at the table. Chessomnia takes over exactly the jobs a wooden board cannot do:

• It knows every rule, including castling, en passant, promotion, the fifty-move rule, threefold repetition and dead positions.
• It never lets an illegal move through, and it never misses a checkmate.
• It keeps the pieces where you put them when the device goes to sleep.


AN OPPONENT WHEN NOBODY ELSE IS AROUND

Play the device instead of a person, in the colour of your choice. Four strengths: the weakest is meant for a child of nine to eleven — it keeps its own position together but walks past most of what you leave hanging — and the strongest is a good club amateur, deliberately not as strong as it could be. An opponent nobody can beat is not a feature.

Take a move back against it and it is your turn again, not its.


A LEARNING AID, NOT A TEACHER

Tap a piece and every square it may legally move to is marked — with castling and en passant highlighted separately, because those are the moves beginners overlook. Switch the hints off once you no longer need them.

What you will never get is a suggestion of which move to play, or a bar telling you who is winning. The opponent makes its move and says nothing about your position. Working that out is the game.


BUILT FOR A DEVICE LYING FLAT ON THE TABLE

A tablet suits it best. A phone works just as well: the board is smaller, nothing else changes.

Everything follows from that one idea:

• Clock, status and buttons exist twice, once at each player's edge, so neither of you is reading upside down.
• The pieces of the player opposite are drawn rotated — the job a real piece's shape does at a wooden board.
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
| Tags | Chess, Board game, Two player, Single player |
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
| Phone screenshots (2–8, min 320px) | `store/screenshots/phone-*.png` | ready, but predates the device opponent — see `store/screenshots/README.md` |
| 7" tablet screenshots (up to 8) | — | **must be taken on a device** |
| 10" tablet screenshots (up to 8) | `store/screenshots/tablet/*.png` | ready — opening, castling, capture, en passant, check, promotion, checkmate, the device opponent (two levels), and Settings in en/de/es |

Screenshots are deliberately not generated from the vector sources: Google Play
requires them to show the real app, and a rendered mock-up would misrepresent
it. Full inventory and what each one shows: `store/screenshots/README.md`.
