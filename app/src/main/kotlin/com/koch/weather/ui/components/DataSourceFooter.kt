package com.koch.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/**
 * Строка об источнике данных и причине failover/кэша. Одна на все экраны:
 * главная, «Список», «Вид». Долгое нажатие обрабатывает родитель (debug-жест FPS).
 */
@Composable
fun DataSourceFooter(
    text: String,
    note: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalKochColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KochSpace.l, vertical = KochSpace.s)
            // Стеклянная подложка: на голубом небе белый 55 % давал 1.6:1.
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(KochSpace.m))
            .background(colors.textPrimary.copy(alpha = 0.10f))
            .padding(horizontal = KochSpace.m, vertical = KochSpace.s),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            style = KochType.tab,
            color = colors.textPrimary.copy(alpha = 0.92f),
            textAlign = TextAlign.Center
        )
        if (!note.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = note,
                style = KochType.tab,
                color = colors.textPrimary.copy(alpha = 0.78f),
                textAlign = TextAlign.Center
            )
        }
    }
}
