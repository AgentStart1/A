/*
 * This is a private project. All rights reserved.
 */

package fonts

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.storyteller_f.a.app.PlatformHeadlessTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AndroidCommunityFontTest : PlatformHeadlessTest() {
    @Test
    fun localFileFontPreservesNativeGlyphMetrics() =
        runTest {
            val fixture = CommunityFontFixture()
            try {
                val bundle =
                    withContext(Dispatchers.Default) {
                        fixture.testFontBundle("android", "Regular.ttf", CommunityFontTestData.primary)
                    }
                val family = assertNotNull(bundle.loaded.family)
                val resolver = assertNotNull(bundle.loaded.resolver)
                val measured =
                    TextMeasurer(resolver, Density(1f), LayoutDirection.Ltr).measure(
                        AnnotatedString("A"),
                        TextStyle(fontFamily = family, fontSize = 100.sp),
                    )
                assertEquals(30, measured.size.width)
            } finally {
                fixture.close()
            }
        }

    @Test
    fun invalidLocalFileDoesNotPublishAFamily() =
        runTest {
            val fixture = CommunityFontFixture()
            try {
                val bundle = fixture.testFontBundle("invalid", "Regular.ttf", byteArrayOf(1, 2, 3))
                assertNull(bundle.loaded.family)
            } finally {
                fixture.close()
            }
        }
}
