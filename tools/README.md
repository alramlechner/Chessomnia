# Tools

Generator scripts. None of them runs as part of the build — they are invoked by hand when
their input changes, and their output is committed.

---

## `generate_wordmark.py` — the wordmark

Generates `android-app/app/src/main/res/drawable/logo_wordmark.xml` (and optionally an SVG
version for store graphics) from the **Montserrat** typeface.

```bash
curl -sSLO https://raw.githubusercontent.com/JulietaUla/Montserrat/master/fonts/ttf/Montserrat-Light.ttf
python3 tools/generate_wordmark.py Montserrat-Light.ttf \
    android-app/app/src/main/res/drawable/logo_wordmark.xml \
    branding/chessomnia-wordmark.svg
```

Requires `python3-fonttools`.

The font file is **deliberately not in the repository**. Only the letter outlines go into
the app — under the OFL a document set with a font is not itself subject to the OFL.
Shipping the TTF would add an obligation to redistribute the licence text without gaining
anything.

⚠️ The E is set without its vertical stem. The three bars are derived from the contour of
the real Montserrat E, not guessed. If a future font version changes that contour so it is
no longer twelve points, the script aborts rather than silently drawing something wrong.

---

## `generate_app_icon.py` — the app icon and the in-app mark

Generates three drawables from one description of the artwork — a **queen beside a king**:

```bash
python3 tools/generate_app_icon.py
```

| Output | Purpose |
|---|---|
| `res/drawable/ic_launcher_foreground.xml` | adaptive icon, foreground layer |
| `res/drawable/ic_launcher_monochrome.xml` | themed icon (Android 13+), single colour |
| `res/drawable/logo_mark.xml` | the same artwork cropped, shown on the home screen |

No dependencies. Both outlines are hand-placed coordinates in the script — original work,
**not** derived from the Cburnett board set that `svg_to_vectordrawable.py` handles.

The placement is computed, not eyeballed: the group is fitted so its corners land on the
72 dp circle that every launcher mask reveals, and the transforms are baked into the path
data so no `<group>` scaling is left for a converter to drop.

⚠️ **Queen and king must keep their gap.** Overlapping them looks richer in colour, but the
themed icon flattens both to one colour and fused silhouettes become an unreadable blob.
If you change `PLACE_QUEEN` or `PLACE_KING`, check the monochrome output, not just the
colour one.

After changing the icon, regenerate the store artwork as well — it is derived from the
launcher foreground:

```bash
python3 tools/render_store_assets.py
rsvg-convert store/play-icon-512.svg -w 512 -h 512 -o store/play-icon-512.png
```

---

## `svg_to_vectordrawable.py` — the chess pieces

Converts the original Cburnett SVGs into
`android-app/app/src/main/res/drawable/piece_*.xml`. The source SVGs are not in the
repository; they come from
[Wikimedia Commons](https://commons.wikimedia.org/wiki/Category:SVG_chess_pieces).

The scope is deliberately narrow: it covers exactly what occurs in those twelve source
files (style inheritance through `<g>`, `translate()` on groups, and one pure rotation
matrix on a path). Anything else makes it abort.

Off-the-shelf converters were tried first and rejected: on these particular files they lose
the viewport, every fill/stroke attribute, and — worst, because it is invisible — the
matrix transform on the knight's eye.

Licence election: Cburnett offers the set multi-licensed (GFDL, CC BY-SA 3.0, BSD 3-Clause,
GPL 2+) with a free choice. Chessomnia elects **BSD 3-Clause** — attribution yes,
share-alike no. The header this script writes into every generated file says so explicitly;
changing the election means changing it here **and** in `licenses/` **and** in
`android-app/app/src/main/res/raw/licenses.txt`.

---

## `generate_reference_corpus.py` — the test reference corpus

Generates `android-app/app/src/test/resources/reference_positions.txt` — 4,576 positions
with their full legal-move list and game status — against **python-chess**.

```bash
pip install chess
python3 tools/generate_reference_corpus.py > android-app/app/src/test/resources/reference_positions.txt
```

The point is cross-checking against a *foreign* implementation: a hand-written test
otherwise only checks a hand-written engine against its author's own assumptions.

**On licensing:** python-chess is GPL-3.0-or-later. The generated corpus is plain chess
fact (positions and the moves legal in them) and therefore not a derivative work of the
library's code; it also lives in `src/test` and never reaches the APK. python-chess is a
development-only dependency and is not redistributed.

⚠️ python-chess does **not** report the fifty-move rule by itself (under FIDE it is
claimable, not automatic), whereas Chessomnia declares the draw at 100 halfmoves.
`status_of()` in the generator mirrors that — without that line the comparison reports
false differences.
