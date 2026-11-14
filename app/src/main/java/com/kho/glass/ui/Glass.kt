package com.kho.glass.ui

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Shape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import eightbitlab.com.blurview.BlurView

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

@Composable
internal fun FrostedBackdrop(
    modifier: Modifier = Modifier,
    tint: Color,
    enabled: Boolean,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val hostView = LocalView.current
    Box(modifier.clip(shape)) {
        if (enabled) {
            AndroidView(
                factory = { context ->
                    val rootView = hostView.rootView
                        .findViewById<ViewGroup>(android.R.id.content)
                        ?: hostView.rootView as? ViewGroup
                        ?: error("The app content view is unavailable for backdrop blur.")
                    BlurView(context).apply {
                        setupWith(rootView)
                            .setBlurRadius(18f)
                            .setOverlayColor(tint.copy(alpha = 0.32f).toArgb())
                    }
                },
                update = { blurView ->
                    blurView.setOverlayColor(tint.copy(alpha = 0.32f).toArgb())
                },
                modifier = Modifier.matchParentSize()
            )
        } else {
            Box(Modifier.matchParentSize().background(tint))
        }
        content()
    }
}
