/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal interface BrowserHistory {
    fun read(): String?
    fun replace(value: String)
    fun push(value: String)
    fun go(delta: Int)
    fun listen(onPop: () -> Unit): () -> Unit
}

@Serializable
internal data class BrowserNavigationStack(val keys: List<String>, val positions: List<Int>)

@Serializable
internal data class BrowserNavigationEntry(
    val scope: String,
    val position: Int = 0,
    val stacks: Map<String, BrowserNavigationStack> = emptyMap(),
)

/** One document owner serializes all mounted root and nested navigation stacks. */
internal class BrowserNavigationController(private val scope: String, private val history: BrowserHistory) {
    private val bindings = mutableMapOf<String, BrowserNavigationBinding>()
    private var entry = readEntry() ?: BrowserNavigationEntry(scope)
    private var stopListening: (() -> Unit)? = null
    private var awaitingPop = false
    private val queued = mutableMapOf<String, List<String>>()

    fun register(id: String, binding: BrowserNavigationBinding) {
        if (bindings.isEmpty()) {
            entry = readEntry() ?: BrowserNavigationEntry(scope)
            stopListening = history.listen(::restoreHistory)
        }
        bindings[id] = binding
        // The pending browser entry owns restoration; mounting must not append placeholder pages.
        if (awaitingPop) return
        val saved = entry.stacks[id]
        if (saved != null) {
            binding.restore(saved.keys)
        } else {
            val keys = binding.read()
            require(keys.isNotEmpty())
            val stack = BrowserNavigationStack(keys.take(1), listOf(entry.position))
            entry = entry.copy(stacks = entry.stacks + (id to stack))
            history.replace(encodeEntry())
            if (keys.size > 1) publishNavigation(id, stack, keys)
        }
    }

    fun unregister(id: String, binding: BrowserNavigationBinding) {
        if (bindings[id] === binding) bindings.remove(id)
        if (bindings.isEmpty()) {
            stopListening?.invoke()
            stopListening = null
            awaitingPop = false
            queued.clear()
        }
    }

    fun update(id: String, binding: BrowserNavigationBinding, keys: List<String>) {
        if (bindings[id] !== binding || keys.isEmpty()) return
        val previous = entry.stacks[id] ?: return
        if (awaitingPop) {
            queued[id] = keys
        } else if (keys != previous.keys) {
            val isPop = keys.size < previous.keys.size && previous.keys.take(keys.size) == keys
            if (isPop) {
                awaitingPop = true
                history.go(previous.positions[keys.lastIndex] - entry.position)
            } else {
                publishNavigation(id, previous, keys)
            }
        }
    }

    private fun publishNavigation(id: String, previous: BrowserNavigationStack, keys: List<String>) {
        val prefix = keys.zip(previous.keys).takeWhile { (next, old) -> next == old }.size
        for (size in prefix + 1..keys.size) {
            val position = entry.position + 1
            val current = entry.stacks.getValue(id)
            val stablePrefix = keys.zip(current.keys).takeWhile { (next, old) -> next == old }.size
            val positions =
                keys.take(size).indices.map {
                    if (it < stablePrefix) current.positions[it] else position
                }
            // A parent returns to the complete subtree last visible beneath that destination.
            val stacks =
                entry.stacks.filterKeys { !it.startsWith("$id/") }.mapValues { (owner, stack) ->
                    if (id.startsWith("$owner/")) {
                        stack.copy(positions = stack.positions.dropLast(1) + position)
                    } else {
                        stack
                    }
                }
            entry =
                entry.copy(
                    position = position,
                    stacks = stacks + (id to BrowserNavigationStack(keys.take(size), positions)),
                )
            history.push(encodeEntry())
        }
    }

    private fun restoreHistory() {
        val restored = readEntry() ?: return
        entry = restored
        awaitingPop = false
        val pending = queued.toMap()
        queued.clear()
        bindings.toMap().forEach { (id, binding) ->
            restored.stacks[id]?.let { binding.restore(it.keys) }
        }
        pending.forEach { (id, keys) ->
            bindings[id]?.let { binding ->
                binding.restore(keys)
                update(id, binding, keys)
            }
        }
    }

    private fun encodeEntry(): String = Json.encodeToString(entry)

    private fun readEntry(): BrowserNavigationEntry? =
        try {
            history.read()?.let { Json.decodeFromString<BrowserNavigationEntry>(it) }
                ?.takeIf { value ->
                    value.scope == scope && value.position >= 0 &&
                        value.stacks.values.all {
                            it.keys.isNotEmpty() && it.keys.size == it.positions.size &&
                                it.positions.all { position -> position in 0..value.position } &&
                                it.positions.zipWithNext().all { (before, after) -> before < after }
                        }
                }
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
}
