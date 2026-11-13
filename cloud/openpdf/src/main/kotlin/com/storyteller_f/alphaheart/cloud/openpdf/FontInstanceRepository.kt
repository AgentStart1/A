/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.getOrThrow
import java.security.MessageDigest
import java.util.HexFormat
import java.util.concurrent.atomic.AtomicReference

/**
 * Bounded immutable cache of font bytes, keyed by content, face, all axes and native version.
 * CAS publishes complete instances. Duplicate concurrent work is harmless and owns separate native
 * resources. No mutable OpenPDF Font/BaseFont is cached. Failed computations are never published.
 */
internal class FontInstanceRepository(private val instancer: FontInstancer = HarfBuzzFontInstancer()) {
    private val cache = AtomicReference<Map<String, FontInstance>>(emptyMap())

    fun getInstance(
        bytes: ByteArray,
        faceIndex: Int = 0,
        axes: Map<String, Float> = emptyMap(),
    ): CustomResult<FontInstance, FontError> =
        captureFontFailure {
            val source = bytes.copyOf()
            val metadata = SfntFont(source, faceIndex)
            validateAxes(metadata.axes, axes)
            val normalized = metadata.axes.mapValues { (tag, axis) -> axes[tag] ?: axis.default }.toSortedMap()
            val digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source))
            val standalone = String(source, 0, 4, Charsets.US_ASCII) != "ttcf"
            val static = !metadata.hasTable("fvar") && !metadata.hasTable("CFF2") && standalone
            val engine = if (static) "sfnt-static-v1" else instancer.version
            val key = "$digest/$faceIndex/${normalized.mapValues { (_, value) ->
                if (value == 0f) 0f else value
            }}/$engine"
            cache.get()[key] ?: publish(
                key,
                FontInstance(if (static) source else instancer.instantiate(source, faceIndex, normalized).getOrThrow()),
            )
        }

    private fun publish(key: String, value: FontInstance): FontInstance {
        while (true) {
            val previous = cache.get()
            previous[key]?.let { return it }
            if (previous.size >= 16 || cache.compareAndSet(previous, previous + (key to value))) return value
        }
    }
}

internal class FontInstance(bytes: ByteArray) {
    private val data = bytes.copyOf()
    fun copyBytes(): ByteArray = data.copyOf()
}
