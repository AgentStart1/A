/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.getOrThrow
import com.sun.jna.Pointer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.openpdf.text.pdf.BaseFont
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FontInstanceRepositoryTest {
    @Test
    fun `static faces bypass native instancing`() {
        val bytes = fontBytes("mono-regular.ttf")
        val repository = FontInstanceRepository(UnavailableInstancer())
        assertContentEquals(bytes, repository.getInstance(bytes).getOrThrow().copyBytes())
    }

    @Test
    fun `static faces take priority for each weight`() {
        val regular = fontBytes("mono-regular.ttf")
        val bold = fontBytes("mono-bold.ttf")
        val family = PdfFontFamily(regular, bold, byteArrayOf(1))
        val repository = FontInstanceRepository(UnavailableInstancer())
        assertContentEquals(regular, family.getInstance(repository, 400).getOrThrow().copyBytes())
        assertContentEquals(bold, family.getInstance(repository, 700).getOrThrow().copyBytes())
    }

    @Test
    fun `invalid selected static faces do not fall back`() {
        val variable = fontBytes("variable.ttf")
        val repository = FontInstanceRepository(UnavailableInstancer())
        for (invalid in listOf(byteArrayOf(1), variable)) {
            val family = PdfFontFamily(invalid, null, variable)
            assertIs<FontError.InvalidFont>(family.getInstance(repository, 400).errorOrNull())
        }
    }

    @Test
    fun `static cache identity uses font content`() {
        val regular = fontBytes("mono-regular.ttf")
        val repository = FontInstanceRepository(UnavailableInstancer())
        val first = repository.getInstance(regular).getOrThrow()
        assertSame(first, repository.getInstance(regular.copyOf()).getOrThrow())
        assertNotSame(first, repository.getInstance(fontBytes("mono-bold.ttf")).getOrThrow())
    }

    @Test
    fun `variable family supplies missing static weight`() {
        val regular = fontBytes("mono-regular.ttf")
        val variable = fontBytes("variable.ttf")
        val repository = FontInstanceRepository()
        val family = PdfFontFamily(regular, null, variable)
        assertContentEquals(regular, family.getInstance(repository, 400).getOrThrow().copyBytes())
        assertFalse(SfntFont(family.getInstance(repository, 700).getOrThrow().copyBytes(), 0).hasTable("fvar"))
    }

    @Test
    fun `invalid font requests return typed failures`() {
        val repository = FontInstanceRepository()
        val variable = fontBytes("variable.ttf")
        for (axis in listOf(mapOf("wght" to 999f), mapOf("wght" to Float.NaN), mapOf("nope" to 400f))) {
            assertIs<FontError.InvalidFont>(repository.getInstance(variable, axes = axis).errorOrNull())
        }
        assertIs<FontError.InvalidFont>(repository.getInstance(variable, faceIndex = 1).errorOrNull())
        assertIs<FontError.InvalidFont>(repository.getInstance(byteArrayOf(1, 2)).errorOrNull())
    }

    @Test
    fun `cache normalizes defaults and protects font bytes`() {
        val bytes = fontBytes("variable.ttf")
        val repository = FontInstanceRepository()
        val first = repository.getInstance(bytes, axes = mapOf("wght" to 400f)).getOrThrow()
        val second =
            repository.getInstance(
                bytes,
                axes = SfntFont(bytes, 0).axes.mapValues { (tag, axis) -> if (tag == "wght") 400f else axis.default },
            ).getOrThrow()
        assertSame(first, second)
        val copy = first.copyBytes()
        val original = copy.copyOf()
        copy.fill(0)
        assertContentEquals(original, first.copyBytes())
        assertNotSame(first, repository.getInstance(bytes, axes = mapOf("wght" to 700f)).getOrThrow())
    }

    @Test
    fun `weight instancing updates metadata and outlines`() {
        val source = fontBytes("variable.ttf")
        val repository = FontInstanceRepository()
        val instances =
            listOf(400f, 700f).map { weight ->
                val bytes = repository.getInstance(source, axes = mapOf("wght" to weight)).getOrThrow().copyBytes()
                val metadata = SfntFont(bytes, 0)
                listOf("fvar", "gvar", "avar", "HVAR", "MVAR").forEach { assertFalse(metadata.hasTable(it), it) }
                assertEquals(weight.toInt(), readFontWeight(bytes))
                assertTrue(
                    BaseFont.createFont(
                        "metric.ttf",
                        BaseFont.IDENTITY_H,
                        true,
                        false,
                        bytes,
                        null,
                    ).getWidth('M'.code) >
                        0,
                )
                bytes
            }
        assertFalse(instances[0].contentEquals(instances[1]))
    }

    @Test
    fun `concurrent cache publication is complete`() =
        runTest {
            val bytes = fontBytes("variable.ttf")
            val repository = FontInstanceRepository()
            val instances =
                (1..8).map {
                    async(Dispatchers.Default) {
                        repository.getInstance(bytes, axes = mapOf("wght" to 400f)).getOrThrow()
                    }
                }.awaitAll()
            instances.forEach { assertSame(instances.first(), it) }
            assertFalse(SfntFont(instances.first().copyBytes(), 0).hasTable("fvar"))
        }

    @Test
    fun `cancellation propagates without cache publication`() {
        val calls = AtomicInteger()
        val cancellation = CancellationException("cancel font operation")
        val instancer =
            object : FontInstancer {
                override val version = "test"
                override fun instantiate(
                    source: ByteArray,
                    faceIndex: Int,
                    axes: Map<String, Float>,
                ): CustomResult<ByteArray, FontError> {
                    calls.incrementAndGet()
                    throw cancellation
                }
            }
        val repository = FontInstanceRepository(instancer)
        repeat(2) {
            assertSame(
                cancellation,
                assertFailsWith<CancellationException> {
                    repository.getInstance(fontBytes("variable.ttf"))
                },
            )
        }
        assertEquals(2, calls.get())
    }

    @Test
    fun `native failure closes handles in reverse order`() {
        val variable = fontBytes("variable.ttf")
        for (failurePoint in 1..6) {
            val native = FailingNative(failurePoint)
            assertTrue(HarfBuzzFontInstancer(native).instantiate(variable, 0, mapOf("wght" to 400f)).isFailure)
            assertEquals(native.allocated.reversed(), native.closed)
        }
    }

    @Test
    fun `native failures are not cached`() {
        val calls = AtomicInteger()
        val output = fontBytes("mono-regular.ttf")
        val instancer =
            object : FontInstancer {
                override val version = "test"
                override fun instantiate(
                    source: ByteArray,
                    faceIndex: Int,
                    axes: Map<String, Float>,
                ): CustomResult<ByteArray, FontError> =
                    if (calls.incrementAndGet() == 1) {
                        CustomResult.failure(FontError.InstancingFailed(IllegalStateException("synthetic failure")))
                    } else {
                        CustomResult.success(output)
                    }
            }
        val repository = FontInstanceRepository(instancer)
        val source = fontBytes("variable.ttf")
        assertIs<FontError.InstancingFailed>(repository.getInstance(source).errorOrNull())
        assertContentEquals(output, repository.getInstance(source).getOrThrow().copyBytes())
        assertEquals(2, calls.get())
    }

    @Test
    fun `collection face cache entries are independent`() {
        val regular = fontBytes("mono-regular.ttf")
        val bold = fontBytes("mono-bold.ttf")
        val collection = createFontCollection(listOf(regular, bold))
        val repository = FontInstanceRepository()
        val first = repository.getInstance(collection, 0).getOrThrow()
        val second = repository.getInstance(collection, 1).getOrThrow()
        assertNotSame(first, second)
        assertFalse(first.copyBytes().contentEquals(second.copyBytes()))
        assertTrue(SfntFont(first.copyBytes(), 0).hasTable("glyf"))
        assertTrue(SfntFont(second.copyBytes(), 0).hasTable("glyf"))
    }

    @Test
    fun `static CFF faces reach native extraction`() {
        val source =
            ByteBuffer.allocate(28).order(ByteOrder.BIG_ENDIAN)
                .putInt(encodeTag("OTTO")).putShort(1).putShort(0).putShort(0).putShort(0)
                .putInt(encodeTag("CFF ")).putInt(0).putInt(28).putInt(0).array()
        val native = FailingNative(2)
        val result = HarfBuzzFontInstancer(native).instantiate(source, 0, emptyMap())
        assertIs<FontError.InstancingFailed>(result.errorOrNull())
        assertEquals(listOf(1), native.allocated)
        assertEquals(listOf(1), native.closed)
    }

    @Test
    fun `older HarfBuzz rejects CFF2 before allocation`() {
        val native = FailingNative(6, supportsCff2 = false)
        val result = HarfBuzzFontInstancer(native).instantiate(cff2Metadata(), 0, emptyMap())
        assertIs<FontError.InstancingFailed>(result.errorOrNull())
        assertTrue(native.allocated.isEmpty())
    }

    @Test
    fun `CFF2 downgrade preserves flags and closes handles`() {
        val native = FailingNative(6)
        val result = HarfBuzzFontInstancer(native).instantiate(cff2Metadata(), 0, emptyMap())
        assertIs<FontError.InstancingFailed>(result.errorOrNull())
        assertEquals(0x4080, native.flags)
        assertEquals(native.allocated.reversed(), native.closed)
    }

    @Test
    fun `residual CFF2 output fails static validation`() {
        val fake = FailingNative(6)
        val native =
            object : HarfBuzzCalls by fake {
                override fun readBlob(blob: Pointer): ByteArray = cff2Metadata(variable = false)
            }
        val result = HarfBuzzFontInstancer(native).instantiate(cff2Metadata(), 0, emptyMap())
        assertIs<FontError.InstancingFailed>(result.errorOrNull())
        assertEquals(fake.allocated.reversed(), fake.closed)
    }

    @Test
    fun `static CFF2 still requires native conversion`() {
        val calls = AtomicInteger()
        val output = fontBytes("mono-regular.ttf")
        val instancer =
            object : FontInstancer {
                override val version = "test"
                override fun instantiate(
                    source: ByteArray,
                    faceIndex: Int,
                    axes: Map<String, Float>,
                ): CustomResult<ByteArray, FontError> {
                    calls.incrementAndGet()
                    return CustomResult.success(output)
                }
            }
        val result = FontInstanceRepository(instancer).getInstance(cff2Metadata(variable = false)).getOrThrow()
        assertContentEquals(output, result.copyBytes())
        assertEquals(1, calls.get())
    }

    private fun cff2Metadata(variable: Boolean = true): ByteArray =
        if (variable) {
            ByteBuffer.allocate(60).order(ByteOrder.BIG_ENDIAN)
                .putInt(encodeTag("OTTO")).putShort(2).putShort(0)
                .putShort(0).putShort(0)
                .putInt(encodeTag("CFF2")).putInt(0).putInt(60).putInt(0)
                .putInt(encodeTag("fvar")).putInt(0).putInt(44).putInt(16)
                .putInt(0x10000).putShort(16).putShort(0)
                .putShort(0).putShort(20).putInt(0).array()
        } else {
            ByteBuffer.allocate(28).order(ByteOrder.BIG_ENDIAN)
                .putInt(encodeTag("OTTO")).putShort(1).putShort(0)
                .putShort(0).putShort(0)
                .putInt(encodeTag("CFF2")).putInt(0).putInt(28).putInt(0).array()
        }

    @Test
    fun `native errors preserve fatal VM failures`() {
        val result = captureFontFailure<Unit> { throw UnsatisfiedLinkError("missing native") }
        assertIs<FontError.NativeUnavailable>(result.errorOrNull())
        assertFailsWith<OutOfMemoryError> { captureFontFailure<Unit> { throw OutOfMemoryError("synthetic fatal") } }
    }

    private class UnavailableInstancer : FontInstancer {
        override val version: String get() = error("Static fonts must not load native code")
        override fun instantiate(
            source: ByteArray,
            faceIndex: Int,
            axes: Map<String, Float>,
        ): CustomResult<ByteArray, FontError> = error("Static fonts must not call instancing")
    }

    private class FailingNative(private val failurePoint: Int, private val supportsCff2: Boolean = true) :
        HarfBuzzCalls {
        override val version = "test"
        val allocated = mutableListOf<Int>()
        val closed = mutableListOf<Int>()
        private var step = 0
        private fun allocate(): FontNativeHandle {
            check(++step != failurePoint) { "Synthetic allocation failure" }
            val id = step
            allocated.add(id)
            return FontNativeHandle(Pointer(id.toLong())) { closed.add(id) }
        }
        override fun createBlob(bytes: ByteArray): FontNativeHandle = allocate()
        override fun createHandle(name: String, destroy: String, vararg args: Any?): FontNativeHandle = allocate()
        var flags = 0x80
            private set
        override fun invokeInt(name: String, vararg args: Any?): Int =
            when {
                name == "hb_version_atleast" -> if (supportsCff2) 1 else 0
                name == "hb_subset_input_get_flags" -> flags
                else -> 1
            }
        override fun invokeVoid(name: String, vararg args: Any?) {
            if (name == "hb_subset_input_set_flags") flags = checkNotNull(args[1]) as Int
        }
        override fun readBlob(blob: Pointer): ByteArray = error("Synthetic serialization failure")
    }
}
