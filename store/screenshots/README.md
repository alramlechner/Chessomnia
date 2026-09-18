# Screenshots

Taken on a device, then cropped where needed. Nothing here is rendered from the vector
sources: Google Play requires screenshots to show the real app, and a mock-up of
the board would misrepresent it.

## Phone

| File | Shows | Size |
|---|---|---|
| `phone-1-castling.png` | the king selected, its legal moves marked, castling highlighted separately | 768 × 766 |
| `phone-2-en-passant.png` | an en passant capture offered on the board | 768 × 766 |
| `phone-3-two-sided-layout.png` | the whole screen: a panel at each table edge, the far one rotated | 768 × 1522 |
| `phone-4-settings-de.png` | settings, German UI | 768 × 1529 |

**Aspect ratio.** Play rejects anything where the longer side is more than twice
the shorter one. A raw screenshot from a modern phone is often about 2.23:1 and
fails that outright — the crops here exist partly for this reason.

**Cropping is a judgement call, not a default.** On a phone the board fills only
about 43 % of the screen height, so in a Play thumbnail the move markers become
invisible; cropping to the board is what makes the learning aid legible at all.
But the two-sided layout — a panel at each edge, the opponent's rotated — is the
one thing this app has and others do not. At least one screenshot must therefore
show the whole screen, or the listing advertises something interchangeable.

Taken with `tools/capture_screenshots.py` on a phone-sized device, before the
opponent shipped. Still valid — nothing shown in them changed — but a phone
screenshot of the opponent (headline "· device", the CPU badge) does not exist
yet. See `tablet/` for that.

## Tablet (`tablet/`)

1840 × 2944, taken with `tools/capture_screenshots.py` and, for the three
settings variants, by hand with `cmd locale set-app-locales` to switch only the
app's language without touching the device's system locale (Android 13+).

| File | Shows |
|---|---|
| `01-opening-move-options-light.png` | a piece selected, its legal moves marked (light theme) |
| `02-castling-available-dark.png` | castling offered, highlighted separately from an ordinary king move (dark theme) |
| `03-capture-offered-light.png` | a capture on offer (light theme) |
| `04-en-passant-dark.png` | an en passant capture on offer (dark theme) |
| `05-check-light.png` | check, the king and the threat visible (light theme) |
| `06-promotion-dialog-dark.png` | the promotion dialog, oriented towards whoever triggered it (dark theme) |
| `07-checkmate-light.png` | checkmate — the board uncovered, king and last move highlighted (light theme) |
| `vs-device-01-learning-light.png` | a game against the device, headline "· Gerät", CPU badge on its panel (light theme) |
| `vs-device-02-club-dark.png` | the same, strongest level, dark theme |
| `settings-en.png` | Settings, English UI |
| `settings-de.png` | Settings, German UI |
| `settings-es.png` | Settings, Spanish UI |

The nine board/opponent shots exist only with German UI chrome (clock, status
line, "· Gerät") — the special-move situations they illustrate (castling, en
passant, checkmate, the opponent) are visual and not language-specific, so they
were not retaken per language. Only Settings, which is mostly text, got all
three.

## Still missing

A **phone** screenshot of the device opponent (the tablet ones above cover it,
but Play also wants phone screenshots and none of the existing four show it).

A **7″ tablet** set — Play distinguishes 7″ from 10″; everything above was taken
on a 10″-class device (1840 × 2944, ~340 dpi).
