package com.kho.glass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable fun GlassCard(modifier:Modifier=Modifier, content:@Composable ColumnScope.()->Unit) {
    Column(modifier.clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha=.72f)).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha=.10f), RoundedCornerShape(24.dp)).padding(16.dp), content=content)
}
