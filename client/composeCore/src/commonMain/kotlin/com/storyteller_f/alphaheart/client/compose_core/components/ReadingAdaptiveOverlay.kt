/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.storyteller_f.alphaheart.client.compose_core.navigation.LocalAdaptiveOverlay
import com.storyteller_f.alphaheart.client.compose_core.navigation.LocalOverlayHost
import com.storyteller_f.alphaheart.client.compose_core.navigation.LocalOverlayTrigger
import com.storyteller_f.alphaheart.client.compose_core.utils.OverlayHostState
import com.storyteller_f.alphaheart.client.compose_core.utils.OverlayPointerObserver
import com.storyteller_f.alphaheart.client.compose_core.utils.OverlayPosition
import com.storyteller_f.alphaheart.client.compose_core.utils.OverlayTrigger
import com.storyteller_f.alphaheart.client.compose_core.utils.ReadingOverlaySize
import kotlin.math.roundToInt

@Composable
fun ReadingOverlayHost(content: @Composable () -> Unit) {
    val trigger = remember { OverlayTrigger() }
    val host = remember { OverlayHostState() }
    CompositionLocalProvider(LocalOverlayHost provides host, LocalOverlayTrigger provides trigger) {
        Box(Modifier.fillMaxSize().observeOverlayTrigger(trigger), propagateMinConstraints = true) {
            content()
            // Popup emits an anchor layout node. Keep it out of the toolbar, row, or list that opened it.
            Box(Modifier.matchParentSize()) {
                host.layers.forEach { layer -> key(layer) { layer() } }
            }
        }
    }
}

@Composable
fun ReadingAdaptiveOverlay(onDismiss: () -> Unit, size: ReadingOverlaySize, content: @Composable () -> Unit) {
    val trigger = LocalOverlayTrigger.current
    val anchor = remember { trigger?.takeBounds() }
    val window = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val currentContent by rememberUpdatedState(content)
    val body = remember { movableContentOf { currentContent() } }
    if (with(density) { window.width.toDp() } > 600.dp) {
        val margin = with(density) { 12.dp.roundToPx() }
        val gap = with(density) { 8.dp.roundToPx() }
        val position =
            remember(anchor, size, margin, gap) {
                OverlayPosition(anchor, margin, gap, centerOnTrigger = size == ReadingOverlaySize.MENU)
            }
        InReadingOverlayHost {
            Popup(position, onDismiss, PopupProperties(focusable = true)) {
                val maximumHeight = with(density) { (window.height - margin * 2).coerceAtLeast(0).toDp() }
                Surface(
                    Modifier.width(size.width).heightIn(max = maximumHeight),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 8.dp,
                ) {
                    OverlayContent(trigger, body)
                }
            }
        }
    } else {
        ReadingDialog(onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            ReadingBottomOverlay(onDismiss) { OverlayContent(trigger, body) }
        }
    }
}

@Composable
private fun InReadingOverlayHost(content: @Composable () -> Unit) {
    val host = LocalOverlayHost.current
    if (host == null) {
        content()
        return
    }
    // Popovers can use the route's ViewModelStore and session even though the host draws them at the root.
    val locals by rememberUpdatedState(currentCompositionLocalContext)
    val currentContent by rememberUpdatedState(content)
    val layer: @Composable () -> Unit =
        remember {
            { CompositionLocalProvider(locals) { currentContent() } }
        }
    DisposableEffect(host, layer) {
        host.layers.add(layer)
        onDispose { host.layers.remove(layer) }
    }
}

@Composable
private fun OverlayContent(trigger: OverlayTrigger?, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAdaptiveOverlay provides true) {
        Box(
            (if (trigger != null) Modifier.observeOverlayTrigger(trigger) else Modifier).fillMaxWidth(),
            propagateMinConstraints = true,
        ) { content() }
    }
}

@Composable
fun ReadingBottomOverlay(onDismiss: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val dismiss by rememberUpdatedState(onDismiss)
    BoxWithConstraints(modifier.fillMaxSize()) {
        Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { dismiss() } })
        Surface(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max = maxHeight * 0.84f),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column {
                ReadingSheetHandle(dismiss)
                Box(Modifier.weight(1f, fill = false)) { content() }
            }
        }
    }
}

@Composable
private fun ReadingSheetHandle(dismiss: () -> Unit) {
    val onDismiss by rememberUpdatedState(dismiss)
    Box(
        Modifier.fillMaxWidth().padding(top = 6.dp).height(14.dp).pointerInput(Unit) {
            var distance = 0f
            detectVerticalDragGestures(
                onDragStart = { distance = 0f },
                onDragEnd = { if (distance > 48.dp.toPx()) onDismiss() },
            ) { change, delta ->
                distance += delta
                change.consume()
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.width(32.dp).height(4.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp)),
        )
    }
}

@Composable
internal fun rememberOverlayAnchor(onClick: () -> Unit): Pair<Modifier, () -> Unit> {
    val trigger = LocalOverlayTrigger.current ?: return Modifier to onClick
    val observer = remember { OverlayPointerObserver() }
    val modifier = Modifier.onGloballyPositioned { observer.coordinates = it }
    val action: () -> Unit = {
        observer.coordinates?.takeIf { it.isAttached }?.boundsInWindow()?.let { bounds ->
            trigger.record(
                IntRect(
                    bounds.left.roundToInt(),
                    bounds.top.roundToInt(),
                    bounds.right.roundToInt(),
                    bounds.bottom.roundToInt(),
                ),
            )
        }
        onClick()
    }
    return modifier to action
}

@Composable
private fun Modifier.observeOverlayTrigger(trigger: OverlayTrigger): Modifier {
    val observer = remember(trigger) { OverlayPointerObserver() }
    return onGloballyPositioned { observer.coordinates = it }.pointerInput(observer) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val press = event.changes.firstOrNull { it.pressed && !it.previousPressed }
                val coordinates = observer.coordinates
                if (press != null && coordinates?.isAttached == true) {
                    val point = coordinates.localToWindow(press.position)
                    if (point.x.isFinite() && point.y.isFinite()) {
                        val position = IntOffset(point.x.roundToInt(), point.y.roundToInt())
                        trigger.record(IntRect(position, IntSize.Zero))
                    }
                }
            }
        }
    }
}
