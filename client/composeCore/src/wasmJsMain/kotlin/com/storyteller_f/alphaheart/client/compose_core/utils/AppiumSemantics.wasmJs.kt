/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.UnplacedAwareModifierNode
import androidx.compose.ui.platform.InspectorInfo
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTextAreaElement

private var nextAppiumSemanticsId = 0L

/** Wasm implementation of [appiumSemantics] backed by transparent HTML elements. */
@Suppress("LibraryEntitiesShouldNotBePublic", "LongParameterList")
actual fun Modifier.appiumSemantics(
    testTag: String?,
    description: String?,
    text: String?,
    input: Boolean,
    inputValue: String?,
    onInputValueChange: ((String) -> Unit)?,
    onClick: (() -> Unit)?,
    onInputSubmit: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
): Modifier {
    if (!window.location.search.contains("appium=true")) return this
    val semanticsModifier =
        composed {
            val id = remember { nextAppiumSemanticsId++ }
            DisposableEffect(id) {
                onDispose { AppiumHtmlSemantics.remove(id) }
            }
            SideEffect {
                AppiumHtmlSemantics.updateMetadata(id, testTag, description, text)
                AppiumHtmlSemantics.updateAction(id, onClick)
                AppiumHtmlSemantics.updateLongAction(id, onLongClick)
                if (input) {
                    AppiumHtmlSemantics.updateInput(
                        id = id,
                        value = inputValue.orEmpty(),
                        onValueChange = onInputValueChange,
                        onSubmit = onInputSubmit,
                    )
                }
            }
            then(
                AppiumPlacementElement(onUnplaced = { AppiumHtmlSemantics.hide(id) }) { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    AppiumHtmlSemantics.update(
                        id = id,
                        testTag = testTag,
                        description = description,
                        text = text,
                        input = input,
                        action = onClick != null || onLongClick != null,
                        left = bounds.left,
                        top = bounds.top,
                        width = bounds.width,
                        height = bounds.height,
                    )
                    AppiumHtmlSemantics.updateLayoutSize(id, coordinates.size.width, coordinates.size.height)
                },
            )
        }
    return semanticsModifier
}

private object AppiumHtmlSemantics {
    private const val OVERLAY_ID = "appium-html-semantics"
    private val elements = mutableMapOf<Long, HTMLElement>()

    private val inputCallbacks = mutableMapOf<Long, (String) -> Unit>()
    private val submitCallbacks = mutableMapOf<Long, () -> Unit>()
    private val inputValues = mutableMapOf<Long, String>()
    private val actionCallbacks = mutableMapOf<Long, () -> Unit>()
    private val longActionCallbacks = mutableMapOf<Long, () -> Unit>()

    fun update(
        id: Long,
        testTag: String?,
        description: String?,
        text: String?,
        input: Boolean,
        action: Boolean,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
    ) {
        if (!isEnabled()) return
        val element = elements.getOrPut(id) { createElement(id, input) }
        element.setAttribute("data-appium-semantics", id.toString())
        updateMetadata(id, testTag, description, text)
        element.updateInputAttributes(id, input)
        element.setBooleanAttribute("data-appium-action", action)
        element.updateLayout(input, action, left, top, width, height)
        element.updateAttachment(isInViewport(left, top, width, height))
    }

    fun updateLayoutSize(id: Long, width: Int, height: Int) {
        elements[id]?.apply {
            setAttribute("data-appium-layout-width", width.toString())
            setAttribute("data-appium-layout-height", height.toString())
        }
    }

    fun updateMetadata(id: Long, testTag: String?, description: String?, text: String?) {
        val element = elements[id] ?: return
        element.setOrRemoveAttribute("data-testid", testTag)
        element.setOrRemoveAttribute("aria-label", description)
        element.setOrRemoveAttribute("data-appium-text", text)
    }

    private fun createElement(id: Long, input: Boolean): HTMLElement =
        if (input) {
            (document.createElement("textarea") as HTMLTextAreaElement).also { textArea ->
                textArea.addEventListener("click", { actionCallbacks[id]?.invoke() })
                textArea.addEventListener("keydown", { event ->
                    if ((event as org.w3c.dom.events.KeyboardEvent).key == "Enter" && submitCallbacks.containsKey(id)) {
                        event.preventDefault()
                        submitCallbacks[id]?.invoke()
                    }
                })
                textArea.addEventListener("input", {
                    inputCallbacks[id]?.invoke(textArea.value)
                    window.requestAnimationFrame {
                        window.requestAnimationFrame {
                            textArea.setAttribute(
                                "data-appium-input-delivered-length",
                                textArea.value.length.toString(),
                            )
                        }
                    }
                })
            }
        } else {
            (document.createElement("button") as HTMLElement).also { button ->
                button.setAttribute("type", "button")
                button.addEventListener("click", {
                    actionCallbacks[id]?.invoke()
                })
                button.addEventListener("contextmenu", { event ->
                    if (longActionCallbacks.containsKey(id)) {
                        event.preventDefault()
                        longActionCallbacks[id]?.invoke()
                    }
                })
            }
        }

    private fun HTMLElement.updateInputAttributes(id: Long, input: Boolean) {
        if (input) {
            setAttribute("data-appium-input", "true")
            val value = inputValues[id].orEmpty()
            val textArea = this as HTMLTextAreaElement
            if (textArea.value != value) {
                textArea.value = value
            }
            setAttribute("data-appium-input-compose-length", value.length.toString())
        } else {
            removeAttribute("data-appium-input")
            removeAttribute("data-appium-input-compose-length")
            removeAttribute("data-appium-input-delivered-length")
        }
    }

    private fun HTMLElement.updateLayout(
        input: Boolean,
        action: Boolean,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
    ) {
        style.apply {
            position = "fixed"
            this.left = "${left}px"
            this.top = "${top}px"
            this.width = "${width}px"
            this.height = "${height}px"
            setProperty("pointer-events", if (input || action) "auto" else "none")
            opacity = "0.01"
            zIndex = "2147483647"
        }
    }

    private fun isInViewport(left: Float, top: Float, width: Float, height: Float): Boolean =
        width > 0f &&
            height > 0f &&
            left + width > 0f &&
            top + height > 0f &&
            left < window.innerWidth &&
            top < window.innerHeight

    private fun HTMLElement.updateAttachment(inViewport: Boolean) {
        if (inViewport && parentElement == null) {
            removeAttribute("aria-hidden")
            overlay().appendChild(this)
        } else if (!inViewport && parentElement != null) {
            setAttribute("aria-hidden", "true")
            remove()
        }
    }

    fun updateInput(id: Long, value: String, onValueChange: ((String) -> Unit)?, onSubmit: (() -> Unit)?) {
        if (!isEnabled()) return
        inputValues[id] = value
        if (onSubmit != null) submitCallbacks[id] = onSubmit else submitCallbacks.remove(id)
        if (onValueChange != null) {
            inputCallbacks[id] = onValueChange
        } else {
            inputCallbacks.remove(id)
        }
        val textArea = elements[id] as? HTMLTextAreaElement ?: return
        if (textArea.value != value) {
            textArea.value = value
        }
        textArea.setAttribute("data-appium-input-compose-length", value.length.toString())
    }

    fun updateAction(id: Long, onClick: (() -> Unit)?) {
        if (!isEnabled()) return
        if (onClick != null) {
            actionCallbacks[id] = onClick
        } else {
            actionCallbacks.remove(id)
        }
    }

    fun hide(id: Long) {
        elements[id]?.updateAttachment(false)
    }

    fun remove(id: Long) {
        inputCallbacks.remove(id)
        submitCallbacks.remove(id)
        inputValues.remove(id)
        actionCallbacks.remove(id)
        longActionCallbacks.remove(id)
        elements.remove(id)?.remove()
    }

    fun updateLongAction(id: Long, onLongClick: (() -> Unit)?) {
        if (!isEnabled()) return
        if (onLongClick != null) longActionCallbacks[id] = onLongClick else longActionCallbacks.remove(id)
    }

    private fun isEnabled(): Boolean = window.location.search.contains("appium=true")

    private fun HTMLElement.setOrRemoveAttribute(name: String, value: String?) {
        if (value == null) {
            removeAttribute(name)
        } else {
            setAttribute(name, value)
        }
    }

    private fun HTMLElement.setBooleanAttribute(name: String, value: Boolean) {
        if (value) {
            setAttribute(name, "true")
        } else {
            removeAttribute(name)
        }
    }

    private fun overlay(): HTMLElement =
        document.getElementById(OVERLAY_ID) as? HTMLElement
            ?: (document.createElement("div") as HTMLElement).also { element ->
                element.id = OVERLAY_ID
                element.style.setProperty("pointer-events", "none")
                document.body?.appendChild(element)
            }
}

private data class AppiumPlacementElement(val onUnplaced: () -> Unit, val onPosition: (LayoutCoordinates) -> Unit) :
    ModifierNodeElement<AppiumPlacementNode>() {
    override fun create() = AppiumPlacementNode(onUnplaced, onPosition)

    override fun update(node: AppiumPlacementNode) {
        node.unplaced = onUnplaced
        node.positioned = onPosition
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "appiumPlacement"
    }
}

private class AppiumPlacementNode(var unplaced: () -> Unit, var positioned: (LayoutCoordinates) -> Unit) :
    Modifier.Node(),
    GlobalPositionAwareModifierNode,
    UnplacedAwareModifierNode {
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) = positioned(coordinates)

    override fun onUnplaced() = unplaced()

    override fun onDetach() = unplaced()
}
