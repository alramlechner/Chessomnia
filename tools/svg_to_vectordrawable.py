#!/usr/bin/env python3
"""
Converts the cburnett piece SVGs into Android VectorDrawables.

Why not `svg2vectordrawable`: on exactly these files that tool loses the viewport (it
sets 24 instead of 45), every fill/stroke attribute, and - worst, because it is
invisible - the matrix() transform on the knight's eye.

The scope here is deliberately narrow: it covers precisely what occurs in the twelve
source files (style inheritance through <g>, translate() on groups, and one pure
rotation matrix on a path). For anything else it aborts rather than silently producing
something wrong.
"""
import math
import re
import sys
import xml.etree.ElementTree as ET

SVG = "{http://www.w3.org/2000/svg}"

STYLE_KEYS = (
    "fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin",
    "stroke-miterlimit", "fill-rule", "fill-opacity", "stroke-opacity", "opacity",
)


def parse_style(el, inherited):
    """Merges presentation attributes and style="", with style taking precedence."""
    style = dict(inherited)
    for k in STYLE_KEYS:
        if el.get(k) is not None:
            style[k] = el.get(k)
    raw = el.get("style")
    if raw:
        for decl in raw.split(";"):
            if ":" not in decl:
                continue
            k, v = decl.split(":", 1)
            k, v = k.strip(), v.strip()
            if k in STYLE_KEYS:
                style[k] = v
    return style


def norm_color(value):
    if value is None or value in ("none", "transparent"):
        return None
    v = value.strip().lower()
    if v.startswith("#"):
        h = v[1:]
        if len(h) == 3:
            h = "".join(c * 2 for c in h)
        if len(h) == 6:
            return "#" + h.upper()
    if v == "black":
        return "#000000"
    if v == "white":
        return "#FFFFFF"
    raise SystemExit(f"Unbekannte Farbe: {value!r}")



def ellipse_as_path(el):
    """
    <circle>/<ellipse> as an equivalent path. Two half-arcs, because a single
    360-degree arc would be degenerate (start and end point identical).
    """
    cx = float(el.get("cx", 0))
    cy = float(el.get("cy", 0))
    if el.tag.endswith("circle"):
        rx = ry = float(el.get("r"))
    else:
        rx = float(el.get("rx"))
        ry = float(el.get("ry"))
    d = (f"M{cx - rx:g},{cy:g} "
         f"a{rx:g},{ry:g} 0 1,0 {2 * rx:g},0 "
         f"a{rx:g},{ry:g} 0 1,0 {-2 * rx:g},0 z")
    fake = ET.Element(SVG + "path")
    fake.set("d", d)
    for k, v in el.items():
        if k != "d":
            fake.set(k, v)
    return fake


def parse_transform(value):
    """Returns (a, b, c, d, e, f) or None."""
    if not value:
        return None
    value = value.strip()
    m = re.fullmatch(r"translate\(\s*([-\d.]+)[,\s]+([-\d.]+)\s*\)", value)
    if m:
        return (1.0, 0.0, 0.0, 1.0, float(m.group(1)), float(m.group(2)))
    m = re.fullmatch(r"translate\(\s*([-\d.]+)\s*\)", value)
    if m:
        return (1.0, 0.0, 0.0, 1.0, float(m.group(1)), 0.0)
    m = re.fullmatch(r"matrix\(\s*([-\d.,\s]+)\s*\)", value)
    if m:
        nums = [float(x) for x in re.split(r"[,\s]+", m.group(1).strip())]
        if len(nums) == 6:
            return tuple(nums)
    raise SystemExit(f"unsupported transform: {value!r}")


def group_attrs(t):
    """
    SVG matrix -> VectorDrawable <group> attributes.

    VectorDrawable has no free matrix, only rotation/scale/translate. With pivot (0,0),
    p' = R(rotation)*S(scale)*p + (translateX, translateY), so a pure rotation combined
    with a translation maps exactly.
    """
    a, b, c, d, e, f = t
    attrs = []
    if abs(a - 1) < 1e-9 and abs(d - 1) < 1e-9 and abs(b) < 1e-9 and abs(c) < 1e-9:
        pass  # a pure translation
    else:
        # Decompose into rotation + uniform scale:
        #   a = s*cos, b = s*sin, c = -s*sin, d = s*cos
        # Tolerance 1e-3, because the matrix values in the source SVG are rounded to
        # three decimals (0.866 instead of cos 30 degrees).
        if not (abs(a - d) < 1e-3 and abs(b + c) < 1e-3):
            raise SystemExit(f"matrix is neither a translation nor rotation+scale: {t}")
        scale = math.hypot(a, b)
        attrs.append(('android:rotation', f"{math.degrees(math.atan2(b, a)):.4f}"))
        if abs(scale - 1) > 1e-3:
            attrs.append(('android:scaleX', f"{scale:.6g}"))
            attrs.append(('android:scaleY', f"{scale:.6g}"))
    if abs(e) > 1e-9:
        attrs.append(('android:translateX', f"{e:g}"))
    if abs(f) > 1e-9:
        attrs.append(('android:translateY', f"{f:g}"))
    return attrs


def path_attrs(style, path_data):
    out = [('android:pathData', path_data)]
    fill = norm_color(style.get("fill"))
    if fill:
        out.append(('android:fillColor', fill))
        rule = style.get("fill-rule", "nonzero")
        out.append(('android:fillType', "evenOdd" if rule == "evenodd" else "nonZero"))
        fo = style.get("fill-opacity")
        if fo and float(fo) < 1:
            out.append(('android:fillAlpha', f"{float(fo):g}"))
    stroke = norm_color(style.get("stroke"))
    if stroke:
        out.append(('android:strokeColor', stroke))
        out.append(('android:strokeWidth', f'{float(style.get("stroke-width", 1)):g}'))
        cap = style.get("stroke-linecap")
        if cap in ("butt", "round", "square"):
            out.append(('android:strokeLineCap', cap))
        join = style.get("stroke-linejoin")
        if join in ("miter", "round", "bevel"):
            out.append(('android:strokeLineJoin', join))
        ml = style.get("stroke-miterlimit")
        if ml:
            out.append(('android:strokeMiterLimit', f"{float(ml):g}"))
        so = style.get("stroke-opacity")
        if so and float(so) < 1:
            out.append(('android:strokeAlpha', f"{float(so):g}"))
    return out


def convert(svg_path, header):
    root = ET.parse(svg_path).getroot()
    width = float(root.get("width", 45))
    height = float(root.get("height", 45))

    lines = []

    def emit_path(el, style, indent):
        d = " ".join(el.get("d").split())
        t = parse_transform(el.get("transform"))
        pad = "    " * indent
        if t:
            ga = group_attrs(t)
            lines.append(f"{pad}<group")
            for i, (k, v) in enumerate(ga):
                lines.append(f'{pad}    {k}="{v}"' + (">" if i == len(ga) - 1 else ""))
            inner = indent + 1
        else:
            inner = indent
        ipad = "    " * inner
        attrs = path_attrs(style, d)
        lines.append(f"{ipad}<path")
        for i, (k, v) in enumerate(attrs):
            lines.append(f'{ipad}    {k}="{v}"' + (" />" if i == len(attrs) - 1 else ""))
        if t:
            lines.append(f"{pad}</group>")

    def walk(el, inherited, indent):
        for child in el:
            tag = child.tag.replace(SVG, "")
            style = parse_style(child, inherited)
            if tag == "g":
                t = parse_transform(child.get("transform"))
                pad = "    " * indent
                if t:
                    ga = group_attrs(t)
                    lines.append(f"{pad}<group")
                    for i, (k, v) in enumerate(ga):
                        lines.append(f'{pad}    {k}="{v}"' + (">" if i == len(ga) - 1 else ""))
                    walk(child, style, indent + 1)
                    lines.append(f"{pad}</group>")
                else:
                    walk(child, style, indent)
            elif tag == "path":
                emit_path(child, style, indent)
            elif tag in ("circle", "ellipse"):
                emit_path(ellipse_as_path(child), style, indent)
            elif tag in ("title", "desc", "defs", "metadata"):
                continue
            else:
                raise SystemExit(f"Unbekanntes Element <{tag}> in {svg_path}")

    walk(root, {}, 1)

    body = "\n".join(lines)
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        f"{header}"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{width:g}dp"\n'
        f'    android:height="{height:g}dp"\n'
        f'    android:viewportWidth="{width:g}"\n'
        f'    android:viewportHeight="{height:g}">\n'
        f"{body}\n"
        "</vector>\n"
    )


if __name__ == "__main__":
    src, dst = sys.argv[1], sys.argv[2]
    hdr = (
        "<!--\n"
        "  Chess piece from the Staunton set by Colin M. L. Burnett (\"cburnett\"),\n"
        "  Wikimedia Commons. The author offers the set multi-licensed\n"
        "  (GFDL / CC BY-SA 3.0 / BSD 3-Clause / GPL 2+) with an explicit free choice;\n"
        "  Chessomnia elects BSD 3-Clause - see licenses/BSD-3-Clause-cburnett.txt.\n"
        "  Generated from the original SVG by tools/svg_to_vectordrawable.py.\n"
        "-->\n"
    )
    open(dst, "w").write(convert(src, hdr))
