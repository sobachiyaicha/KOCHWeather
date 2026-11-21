package com.koch.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koch.weather.ui.design.KochPrimaryButton
import com.koch.weather.ui.design.KochSheet
import com.koch.weather.ui.theme.KochBorderWidth
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * «Пора на улицу!» — блокирующий лист по refs/grass_lock.png (2:44):
 * заголовок, абзац, пунктирная зона загрузки, синяя капсула. Без иконки, без крестика.
 */
@Composable
fun GrassLockDialog(
    onGrassUploaded: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKochColors.current
    val coroutineScope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf<String?>(null) }

    fun startUpload() {
        if (isUploading) return
        isUploading = true
        uploadStatus = "Открываем камеру..."
        coroutineScope.launch {
            delay(1000)
            uploadStatus = "Проверяем подлинность травы..."
            delay(1200)
            uploadStatus = "Трава подтверждена!"
            delay(600)
            onGrassUploaded()
        }
    }

    KochSheet(
        onDismiss = onDismiss,
        blocking = true,
        showHandle = false,
        maxHeightFraction = 0.62f,
        content = {
            Spacer(Modifier.height(KochSpace.l))
            Text(
                text = "Пора на улицу!",
                style = KochType.titleSheet,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(KochSpace.m))
            Text(
                text = "Вы слишком долго смотрели погоду, дальнейший просмотр будет " +
                    "доступен только после загрузки фотографии травы",
                style = KochType.body.copy(lineHeight = 21.sp),
                color = colors.textPrimary.copy(alpha = 0.95f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(KochSpace.xl))
            UploadZone(
                isUploading = isUploading,
                status = uploadStatus,
                onClick = { startUpload() }
            )
        },
        footer = {
            KochPrimaryButton(
                text = "Открыть камеру",
                onClick = { startUpload() },
                enabled = !isUploading
            )
        }
    )
}

@Composable
private fun UploadZone(
    isUploading: Boolean,
    status: String?,
    onClick: () -> Unit
) {
    val colors = LocalKochColors.current
    val shape = RoundedCornerShape(KochRadius.panel)
    val dashColor = colors.textPrimary.copy(alpha = 0.4f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 140.dp)
            .clip(shape)
            .background(colors.textPrimary.copy(alpha = 0.06f))
            .drawBehind {
                val stroke = KochBorderWidth.dashed.toPx()
                drawRoundRect(
                    color = dashColor,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(KochRadius.panel.toPx()),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 4.dp.toPx())
                        )
                    )
                )
            }
            .clickable(enabled = !isUploading, onClick = onClick)
            .padding(KochSpace.l),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isUploading) {
                CircularProgressIndicator(
                    color = colors.textPrimary.copy(alpha = 0.7f),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.height(KochSpace.s))
                Text(
                    text = status ?: "Загрузка...",
                    style = KochType.sub,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Загрузите фото травы",
                    style = KochType.body,
                    color = colors.textPrimary.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
