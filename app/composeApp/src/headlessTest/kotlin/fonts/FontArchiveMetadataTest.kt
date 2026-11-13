/*
 * This is a private project. All rights reserved.
 */

package fonts

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.storyteller_f.alphaheart.app.pages.community.createFontFace
import kotlin.test.Test
import kotlin.test.assertEquals

class FontArchiveMetadataTest {
    @Test
    fun windowsArchivePathPreservesWeightAndStyle() {
        val face = createFontFace("""C:\download-cache\family\BoldItalic.ttf""")
        assertEquals(FontWeight.Bold, face.weight)
        assertEquals(FontStyle.Italic, face.style)
    }
}
