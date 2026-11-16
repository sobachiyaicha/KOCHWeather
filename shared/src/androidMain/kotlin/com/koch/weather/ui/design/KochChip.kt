package com.koch.weather.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.koch.weather.ui.theme.KochBorderWidth
import com.koch.weather.ui.theme.KochShape
import com.koch.weather.ui.theme.KochSize
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/**
 * Чип по контенту: ширина = тексту + 24 dp. Обрезка запрещена — «Изумрудный лес» обязан
 * влезть целиком; при переполнении ряда родитель скроллит, а не режет текст.
 */
@Composable
fun KochChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    locked: Boolean = false,
    leadingIcon: ImageVector? = null,
    selectedColor: androidx.compose.ui.graphics.Color? = null
) {
    val colors = LocalKochColors.current
    val accent = selectedColor ?: colors.accent
    val fill = when {
        selected -> accent.copy(alpha = 0.16f)
        locked -> colors.textPrimary.copy(alpha = 0.06f)
        else -> colors.textPrimary.copy(alpha = 0.12f)
    }
    val borderColor = when {
        selected -> accent.copy(alpha = 0.45f)
        else -> colors.textPrimary.copy(alpha = 0.16f)
    }
    val textColor = when {
        selected -> accent
        locked -> colors.textPrimary.copy(alpha = 0.45f)
        else -> colors.textPrimary.copy(alpha = 0.9f)
    }

    Row(
        modifier = modifier
            .heightIn(min = KochSize.chip)
            .clip(KochShape.pill)
            .background(fill)
            .border(KochBorderWidth.hairline, borderColor, KochShape.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = KochSpace.m, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        leadingIcon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = KochType.chip,
            color = textColor,
            maxLines = 1,
            softWrap = false
        )
        if (locked) {
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
        } else if (selected) {
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

/** Кружок-переключатель расцветки (зонты): 40 dp, выбранный — кольцо акцента. */
@Composable
fun KochColorDot(
    color: androidx.compose.ui.graphics.Color,
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalKochColors.current
    Box(
        modifier = modifier
            .size(if (selected) 44.dp else 40.dp)
            .clip(CircleShape)
            .border(
                width = if (selected) 2.dp else KochBorderWidth.hairline,
                color = if (selected) colors.textPrimary else colors.borderStrong,
                shape = CircleShape
            )
            .clickable(
                role = Role.RadioButton,
                onClickLabel = contentDescription,
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription }
            .padding(4.dp)
            .clip(CircleShape)
            .background(color)
    ) {}
}
