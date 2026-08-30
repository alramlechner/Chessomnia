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
