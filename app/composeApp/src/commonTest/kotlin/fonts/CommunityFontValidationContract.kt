/*
 * This is a private project. All rights reserved.
 */

package fonts

import com.storyteller_f.alphaheart.app.common.createFileEngine
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontBundle
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontDownload
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontState
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Shared cases exercise the production download/metadata/platform/preload publication boundary. */
abstract class CommunityFontValidationContract {
    protected abstract suspend fun prepareFontEngine()

    @Test
    fun duplicateWeightAndStyleRejectsTheWholeArchive() =
        runTest {
            prepareFontEngine()
            val bytes =
                fontTestArchive(
                    "one-Regular.ttf" to CommunityFontTestData.primary,
                    "two-Regular.ttf" to CommunityFontTestData.primary,
                )
            assertRejected(bytes, "family.zip", "Duplicate font weight and style")
        }

    @Test
    fun renamedWeightRejectsTheWholeArchive() =
        runTest {
            prepareFontEngine()
            val bytes =
                fontTestArchive(
                    "Regular.ttf" to CommunityFontTestData.primary,
                    "Bold.ttf" to CommunityFontTestData.primary,
                )
            assertRejected(bytes, "family.zip", "Internal font weight or style does not match the filename")
        }

    @Test
    fun renamedItalicRejectsTheWholeArchive() =
        runTest {
            prepareFontEngine()
            val bytes =
                fontTestArchive(
                    "Regular.ttf" to CommunityFontTestData.primary,
                    "Italic.ttf" to CommunityFontTestData.primary,
                )
            assertRejected(bytes, "family.zip", "Internal font weight or style does not match the filename")
        }

    @Test
    fun internalItalicCannotBeLoadedAsRegular() =
        runTest {
            prepareFontEngine()
            assertRejected(
                CommunityFontTestData.primaryItalic,
                "Regular.ttf",
                "Internal font weight or style does not match the filename",
            )
        }

    @Test
    fun differentInternalFamiliesRejectTheWholeArchive() =
        runTest {
            prepareFontEngine()
            val bytes =
                fontTestArchive(
                    "Regular.ttf" to CommunityFontTestData.code,
                    "Bold.ttf" to CommunityFontTestData.primaryBold,
                )
            assertRejected(bytes, "family.zip", "Font faces have different internal families")
        }

    @Test
    fun matchingLegacyNamesCannotHideDifferentTypographicFamilies() =
        runTest {
            prepareFontEngine()
            val bytes =
                fontTestArchive(
                    "Regular.ttf" to CommunityFontTestData.primary,
                    "Bold.ttf" to CommunityFontTestData.foreignBold,
                )
            assertRejected(bytes, "family.zip", "Font faces have different internal families")
        }

    @Test
    fun missingWeightMetadataDoesNotPublishAFamily() =
        runTest {
            prepareFontEngine()
            val bytes = renameFontTable(CommunityFontTestData.primary, "OS/2", "xxxx")
            assertFailsWith<IllegalStateException> { downloadValidationFont(bytes, "Regular.ttf") }
        }

    @Test
    fun typographicFamilyGroupsDifferentLegacyStyleFamilies() =
        runTest {
            prepareFontEngine()
            val bytes =
                fontTestArchive(
                    "Regular.ttf" to CommunityFontTestData.primary,
                    "Light.ttf" to CommunityFontTestData.primaryLight,
                )
            val bundle = downloadValidationFont(bytes, "family.zip")
            assertNotNull(bundle.loaded.family)
            assertNotNull(bundle.loaded.resolver)
            assertEquals(2, bundle.faces.size)
        }

    @Test
    fun legacyFamilyIsAcceptedWhenTypographicFamilyIsAbsent() =
        runTest {
            prepareFontEngine()
            val bytes = removeTypographicNames(CommunityFontTestData.primary)
            assertNotNull(downloadValidationFont(bytes, "Regular.ttf").loaded.family)
        }

    @Test
    fun allFourRealStylesArePublishedTogether() =
        runTest {
            prepareFontEngine()
            val bytes = fontTestZip("Regular.ttf", "Bold.ttf", "Italic.ttf", "BoldItalic.ttf")
            val bundle = downloadValidationFont(bytes, "family.zip")
            assertEquals(4, bundle.faces.size)
            assertNotNull(bundle.loaded.family)
            assertNotNull(bundle.loaded.resolver)
        }

    @Test
    fun missingInternalMetadataDoesNotPublishAFamily() =
        runTest {
            prepareFontEngine()
            val bytes = renameFontTable(CommunityFontTestData.primary, "name", "xxxx")
            assertFailsWith<IllegalStateException> { downloadValidationFont(bytes, "Regular.ttf") }
        }

    @Test
    fun invalidFaceIsNeverSkipped() =
        runTest {
            prepareFontEngine()
            val bytes =
                fontTestArchive(
                    "Regular.ttf" to CommunityFontTestData.primary,
                    "Bold.ttf" to byteArrayOf(1, 2, 3),
                )
            assertFailsWith<IllegalStateException> { downloadValidationFont(bytes, "family.zip") }
        }

    @Test
    fun rejectedFamilyUsesTheExistingStoreFailureState() =
        runTest {
            prepareFontEngine()
            var fetches = 0
            val store =
                CommunityFontStore(
                    backgroundScope,
                    observe = { file ->
                        flow {
                            try {
                                fetches++
                                emit(
                                    CommunityFontState.Ready(
                                        downloadValidationFont(CommunityFontTestData.primary, file.name),
                                    ),
                                )
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: IllegalStateException) {
                                emit(CommunityFontState.Failed(failure))
                            }
                        }
                    },
                    resume = {},
                )
            val file = fontFile(CommunityFontTestData.primary, "Bold.ttf")
            assertNull(store.load(file))
            assertNull(store.load(file))
            assertEquals(1, fetches)
        }

    private suspend fun CoroutineScope.assertRejected(bytes: ByteArray, name: String, message: String) {
        val failure = assertFailsWith<IllegalStateException> { downloadValidationFont(bytes, name) }
        assertEquals(message, failure.message)
    }
}

internal suspend fun CoroutineScope.downloadValidationFont(bytes: ByteArray, name: String): CommunityFontBundle =
    HttpClient(MockEngine { respond(bytes) }).use { client ->
        val fixture = DownloadTestFixture(this, createFileEngine(), client)
        try {
            CommunityFontDownload(fixture.records, fixture.downloader, fixture.files, fixture.namespace)
                .load(fontFile(bytes, name))
        } finally {
            fixture.close()
        }
    }

private fun renameFontTable(source: ByteArray, tag: String, replacement: String): ByteArray =
    source.copyOf().also {
        val offset = findFontTable(it, tag)
        replacement.encodeToByteArray().copyInto(it, offset)
    }

private fun removeTypographicNames(source: ByteArray): ByteArray =
    source.copyOf().also { bytes ->
        val entry = findFontTable(bytes, "name")
        val offset = (0..3).fold(0) { value, index -> value.shl(8).or(bytes[entry + 8 + index].toInt().and(255)) }
        val count = readFontShort(bytes, offset + 2)
        repeat(count) { index ->
            val nameId = offset + 6 + index * 12 + 6
            if (readFontShort(bytes, nameId) in 16..17) bytes[nameId + 1] = 0x7f
        }
    }

private fun findFontTable(bytes: ByteArray, tag: String): Int =
    (0 until readFontShort(bytes, 4)).map { 12 + it * 16 }.first { offset ->
        (0..3).all { bytes[offset + it].toInt() == tag[it].code }
    }

private fun readFontShort(bytes: ByteArray, offset: Int): Int =
    bytes[offset].toInt().and(255).shl(8).or(bytes[offset + 1].toInt().and(255))
