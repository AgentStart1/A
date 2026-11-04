"""Regenerate tiny, project-owned font vectors (requires fontTools)."""
import base64
import io
import re
import zipfile
from pathlib import Path

from fontTools.fontBuilder import FontBuilder
from fontTools.pens.ttGlyphPen import TTGlyphPen


def make_font(name, characters):
    builder = FontBuilder(1000, isTTF=True)
    glyphs = {".notdef": 400, **{f"u{point:X}": width for point, width in characters.items()}}
    builder.setupGlyphOrder(list(glyphs))
    builder.setupCharacterMap({point: f"u{point:X}" for point in characters})
    outlines = {}
    for glyph, width in glyphs.items():
        pen = TTGlyphPen(None)
        pen.moveTo((20, 0))
        pen.lineTo((width - 20, 0))
        pen.lineTo((width - 20, 700))
        pen.lineTo((20, 700))
        pen.closePath()
        outlines[glyph] = pen.glyph()
    builder.setupGlyf(outlines)
    builder.setupHorizontalMetrics({glyph: (width, 0) for glyph, width in glyphs.items()})
    builder.setupHorizontalHeader(ascent=800, descent=-200)
    builder.setupNameTable({"familyName": name, "styleName": "Regular", "uniqueFontIdentifier": name,
                           "fullName": name, "psName": name})
    builder.setupOS2(sTypoAscender=800, sTypoDescender=-200, usWinAscent=800, usWinDescent=200)
    builder.setupPost()
    builder.font.recalcTimestamp = False
    builder.font["head"].created = builder.font["head"].modified = 2082844800
    output = io.BytesIO()
    builder.save(output)
    return base64.b64encode(output.getvalue()).decode()


target = Path(__file__).resolve().parents[2] / "app/composeApp/src/commonTest/kotlin/fonts/CommunityFontTestData.kt"
target.parent.mkdir(parents=True, exist_ok=True)
text = """/*
 * This is a private project. All rights reserved.
 */

package fonts

import kotlin.io.encoding.Base64

// Synthetic glyphs with distinct advances; no OS fonts or external font files are required.
internal object CommunityFontTestData {
"""
accessors = []
for key, characters in {"primary": {65: 300}, "code": {65: 600}}.items():
    encoded = make_font("Synthetic" + key.title(), characters)
    chunks = [encoded[i:i + 88] for i in range(0, len(encoded), 88)]
    constant = re.sub(r"(?<!^)(?=[A-Z])", "_", key).upper() + "_DATA"
    accessors.append(f"    val {key}: ByteArray get() = Base64.decode({constant})")
    text += f"    private const val {constant} =\n" + " +\n".join(f'{" " * (8 if index == 0 else 12)}"{chunk}"' for index, chunk in enumerate(chunks)) + "\n\n"
archive = io.BytesIO()
with zipfile.ZipFile(archive, "w", compression=zipfile.ZIP_DEFLATED) as zipped:
    entry = zipfile.ZipInfo("family/Regular.ttf", date_time=(2000, 1, 1, 0, 0, 0))
    entry.compress_type = zipfile.ZIP_DEFLATED
    zipped.writestr(entry, base64.b64decode(make_font("SyntheticPrimary", {65: 300})))
encoded = base64.b64encode(archive.getvalue()).decode()
accessors.append("    val primaryZip: ByteArray get() = Base64.decode(PRIMARY_ZIP_DATA)")
text += "    private const val PRIMARY_ZIP_DATA =\n" + " +\n".join(
    f'{" " * (8 if i == 0 else 12)}"{encoded[i:i + 88]}"' for i in range(0, len(encoded), 88)
) + "\n\n"
text += "\n".join(accessors) + "\n"
target.write_text(text.rstrip() + "\n}\n", encoding="utf-8", newline="\n")
