package com.kho.glass.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView

@Composable
internal fun PhotoCropDialog(
    sourceUri: Uri?,
    sourceBitmap: Bitmap?,
    onDismiss: () -> Unit,
    onCropComplete: (Bitmap) -> Unit,
    onCropError: (String) -> Unit
) {
    val context = LocalContext.current
    val onCompleteState = rememberUpdatedState(onCropComplete)
    val onErrorState = rememberUpdatedState(onCropError)
    val cropping = remember { mutableStateOf(false) }
    val cropError = remember { mutableStateOf<String?>(null) }
    val ratio = remember { mutableIntStateOf(1) }
    val cropView = remember(context, sourceUri, sourceBitmap) {
        CropImageView(context).apply {
            setImageCropOptions(
                CropImageOptions(
                    guidelines = CropImageView.Guidelines.ON_TOUCH,
                    showProgressBar = false
                )
            )
            setFixedAspectRatio(true)
            setAspectRatio(1, 1)
            if (sourceUri != null) {
                setImageUriAsync(sourceUri)
            } else if (sourceBitmap != null) {
                setImageBitmap(sourceBitmap)
            }
        }
    }

    DisposableEffect(cropView) {
        cropView.setOnCropImageCompleteListener { _, result ->
            cropping.value = false
            val bitmap = result.bitmap
            val failure = result.error
            if (failure != null) {
                val message = failure.localizedMessage ?: "Lỗi không xác định."
                cropError.value = message
                onErrorState.value(message)
            } else if (bitmap != null) {
                cropError.value = null
                onCompleteState.value(bitmap)
            } else {
                val message = "Không tạo được ảnh đã cắt."
                cropError.value = message
                onErrorState.value(message)
            }
        }
        onDispose { cropView.setOnCropImageCompleteListener(null) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Cắt và chỉnh ảnh",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (cropping.value) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    } else {
                        IconButton(
                            enabled = sourceUri != null || sourceBitmap != null,
                            onClick = {
                                cropping.value = true
                                cropError.value = null
                                cropView.croppedImageAsync()
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Dùng ảnh đã cắt")
                        }
                    }
                    IconButton(onClick = onDismiss, enabled = !cropping.value) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng chỉnh ảnh")
                    }
                }
                AndroidView(
                    factory = { cropView },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    listOf("Tự do", "1:1", "10:16", "16:10").forEachIndexed { index, label ->
                        FilterChip(
                            selected = ratio.intValue == index,
                            onClick = {
                                ratio.intValue = index
                                when (index) {
                                    0 -> cropView.setFixedAspectRatio(false)
                                    1 -> {
                                        cropView.setFixedAspectRatio(true)
                                        cropView.setAspectRatio(1, 1)
                                    }
                                    2 -> {
                                        cropView.setFixedAspectRatio(true)
                                        cropView.setAspectRatio(10, 16)
                                    }
                                    else -> {
                                        cropView.setFixedAspectRatio(true)
                                        cropView.setAspectRatio(16, 10)
                                    }
                                }
                            },
                            label = { Text(label) },
                            enabled = !cropping.value,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { cropView.rotateImage(-90) },
                        enabled = !cropping.value
                    ) {
                        Icon(Icons.Default.RotateLeft, contentDescription = "Xoay trái")
                    }
                    IconButton(
                        onClick = { cropView.rotateImage(90) },
                        enabled = !cropping.value
                    ) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Xoay phải")
                    }
                    IconButton(
                        onClick = { cropView.flipImageHorizontally() },
                        enabled = !cropping.value
                    ) {
                        Icon(Icons.Default.Flip, contentDescription = "Lật ngang")
                    }
                    IconButton(
                        onClick = { cropView.flipImageVertically() },
                        enabled = !cropping.value
                    ) {
                        Icon(
                            Icons.Default.Flip,
                            contentDescription = "Lật dọc",
                            modifier = Modifier.graphicsLayer(rotationZ = 90f)
                        )
                    }
                }
                cropError.value?.let { message ->
                    Text(
                        message,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
