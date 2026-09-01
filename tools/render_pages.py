#!/usr/bin/env python3
"""
Renders the GitHub Pages site in docs/ from the repository's own documents.

    python3 tools/render_pages.py

The privacy policy is what Google Play links to, so it must not drift from
PRIVACY.md. Rather than keeping a second copy in HTML, this converts the
Markdown at build time. Editing docs/*.html by hand is therefore pointless --
the next run overwrites it.

Deliberately no static-site generator and no Jekyll: two pages do not justify a
toolchain, and .nojekyll keeps GitHub from trying to process the output.
"""
import html
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parent.parent
DOCS = ROOT / "docs"

NAVY, BLUE, TEXT, MUTED = "#2B303E", "#3193C6", "#E8ECF2", "#9AA5B8"
REPO = "https://github.com/alramlechner/Chessomnia"

STYLE = f"""
  *{{box-sizing:border-box}}
  body{{margin:0;padding:0 20px;background:#161A22;color:{TEXT};
       font:16px/1.65 -apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif}}
  .wrap{{max-width:720px;margin:0 auto;padding:56px 0 80px}}
  h1{{font-size:1.9rem;line-height:1.2;margin:0 0 .2em}}
  h2{{font-size:1.15rem;margin:2.2em 0 .5em;color:{BLUE}}}
  a{{color:{BLUE}}}
  code{{background:{NAVY};padding:.15em .4em;border-radius:4px;font-size:.9em;
        overflow-wrap:anywhere}}
  ul{{padding-left:1.2em}}
  .lede{{color:{MUTED}}}
  .mark{{display:block;width:96px;margin:0 0 24px}}
  footer{{margin-top:56px;padding-top:20px;border-top:1px solid #3C4456;
          color:{MUTED};font-size:.9rem}}
"""


def page(title, body, description):
    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{html.escape(title)}</title>
<meta name="description" content="{html.escape(description)}">
<meta name="color-scheme" content="dark">
<style>{STYLE}</style>
</head>
<body><div class="wrap">
{body}
<footer>Chessomnia is free software under the Apache License 2.0 &middot;
<a href="{REPO}">Source code on GitHub</a></footer>
</div></body>
</html>
"""


def inline(text):
    """The small subset of Markdown these documents actually use."""
    text = html.escape(text)
    text = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r'<a href="\2">\1</a>', text)
    text = re.sub(r"&lt;(https?://[^&]+)&gt;", r'<a href="\1">\1</a>', text)
    text = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", text)
    text = re.sub(r"(?<!\*)\*([^*]+)\*(?!\*)", r"<em>\1</em>", text)
    text = re.sub(r"`([^`]+)`", r"<code>\1</code>", text)
    return text


def markdown_to_html(md):
    out, para, in_list = [], [], False

    def flush():
        nonlocal para
        if para:
            out.append(f"<p>{inline(' '.join(para))}</p>")
            para = []

    def close_list():
        nonlocal in_list
        if in_list:
            out.append("</ul>")
            in_list = False

    for raw in md.splitlines():
        line = raw.rstrip()
        if not line.strip():
            flush(); close_list(); continue
        if line.startswith("## "):
            flush(); close_list(); out.append(f"<h2>{inline(line[3:])}</h2>"); continue
        if line.startswith("# "):
            flush(); close_list(); out.append(f"<h1>{inline(line[2:])}</h1>"); continue
        if line.lstrip().startswith("- "):
            flush()
            if not in_list:
                out.append("<ul>"); in_list = True
            out.append(f"<li>{inline(line.lstrip()[2:])}</li>")
            continue
        close_list()
        para.append(line.strip())
    flush(); close_list()
    return "\n".join(out)


def main():
    DOCS.mkdir(exist_ok=True)
    (DOCS / ".nojekyll").write_text("")

    icon = (ROOT / "store/play-icon-512.png")
    (DOCS / "icon.png").write_bytes(icon.read_bytes())

    privacy = markdown_to_html((ROOT / "PRIVACY.md").read_text(encoding="utf-8"))
    (DOCS / "privacy.html").write_text(page(
        "Privacy Policy — Chessomnia",
        f'<img class="mark" src="icon.png" alt="">\n{privacy}',
        "Chessomnia collects no data at all. Full privacy policy.",
    ), encoding="utf-8")

    (DOCS / "index.html").write_text(page(
        "Chessomnia — a chess board for two players",
        f'''<img class="mark" src="icon.png" alt="">
<h1>Chessomnia</h1>
<p class="lede">A chess board for two people sharing one device.
Not a chess computer.</p>
<p>Two players sit opposite each other, the tablet &mdash; or phone &mdash; lies flat
on the table between them, and they play the way they would on wood and felt, except
that the app knows the rules. There is no engine, no evaluation and no move
suggestion.</p>
<p>Free, ad-free, tracking-free, and it works entirely offline: the app holds no
permission that grants it anything, not even internet access.</p>
<h2>More</h2>
<ul>
  <li><a href="privacy.html">Privacy policy</a> &mdash; short version: nothing is collected</li>
  <li><a href="{REPO}">Source code on GitHub</a> (Apache&nbsp;2.0)</li>
</ul>''',
        "Chessomnia is a chess board for two players on one tablet or phone. "
        "No engine, no ads, no tracking, fully offline.",
    ), encoding="utf-8")

    for f in sorted(DOCS.iterdir()):
        print(f"  docs/{f.name}  ({f.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
