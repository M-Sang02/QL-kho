package com.kho.glass.ui

import android.graphics.RenderNode
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

@Stable
internal class BackdropGlassState {
    var renderNode by mutableStateOf<Any?>(null)
    var rootPosition by mutableStateOf(Offset.Zero)
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                RoundedCornerShape(24.dp)
            )
            .padding(16.dp),
        content = content
    )
}

internal fun Modifier.captureBackdrop(
    state: BackdropGlassState,
    enabled: Boolean
): Modifier = this
    .onGloballyPositioned { state.rootPosition = it.positionInWindow() }
    .drawWithContent {
        val targetCanvas = drawContext.canvas
        if (
            !enabled ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            size.width < 1f ||
            size.height < 1f ||
            !targetCanvas.nativeCanvas.isHardwareAccelerated
        ) {
            drawContent()
            return@drawWithContent
        }

        val node = backdropNode(state.renderNode, size.width.toInt(), size.height.toInt())
        val recordingCanvas = node.beginRecording()
        drawContext.canvas = androidx.compose.ui.graphics.Canvas(recordingCanvas)
        try {
            drawContent()
        } finally {
            drawContext.canvas = targetCanvas
            node.endRecording()
        }
        state.renderNode = node
        targetCanvas.nativeCanvas.drawRenderNode(node)
    }

@RequiresApi(Build.VERSION_CODES.S)
private fun backdropNode(existing: Any?, width: Int, height: Int): RenderNode =
    (existing as? RenderNode ?: RenderNode("InventoryBackdrop")).apply {
        setPosition(0, 0, width, height)
    }

@RequiresApi(Build.VERSION_CODES.S)
private fun blurEffect(radius: Float): androidx.compose.ui.graphics.RenderEffect =
    android.graphics.RenderEffect.createBlurEffect(
        radius,
        radius,
        android.graphics.Shader.TileMode.CLAMP
    ).asComposeRenderEffect()

@Composable
internal fun FrostedBackdrop(
    modifier: Modifier = Modifier,
    tint: Color,
    enabled: Boolean,
    shape: Shape = RoundedCornerShape(24.dp),
    backdrop: BackdropGlassState? = null,
    content: @Composable BoxScope.() -> Unit
) {
    var position by remember { mutableStateOf(Offset.Zero) }
    val blurEnabled = enabled && backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val density = LocalDensity.current
    val blur = if (blurEnabled) {
        remember(density) { with(density) { blurEffect(18.dp.toPx()) } }
    } else {
        null
    }

    Box(modifier.clip(shape).onGloballyPositioned { position = it.positionInWindow() }) {
        if (blurEnabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { renderEffect = blur }
                    .drawWithContent {
                        drawContent()
                        val source = backdrop ?: return@drawWithContent
                        val node = source.renderNode as? RenderNode
                            ?: return@drawWithContent
                        val origin = source.rootPosition
                        drawIntoCanvas { canvas ->
                            val native = canvas.nativeCanvas
                            native.save()
                            native.translate(origin.x - position.x, origin.y - position.y)
                            native.drawRenderNode(node)
                            native.restore()
                        }
                    }
            )
        }
        Box(
            Modifier.matchParentSize().background(
                tint.copy(alpha = if (enabled) 0.32f else 1f)
            )
        )
        content()
    }
}
