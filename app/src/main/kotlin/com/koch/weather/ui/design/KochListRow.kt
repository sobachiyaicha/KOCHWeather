package com.koch.weather.ui.design

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.koch.weather.ui.theme.KochSize
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/**
 * Строка списка внутри листа или страницы: без «карточки в карточке».
 * Выделение — заливкой и полоской 2 dp слева, без цветной рамки.
 */
@Composable
fun KochListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    showDivider: Boolean = false,
    minHeight: Dp = KochSize.row,
    titleMaxLines: Int = 1,
    subtitleMaxLines: Int = 2
) {
    val colors = LocalKochColors.current
    val selectedFill = if (selected) colors.accent.copy(alpha = 0.10f) else androidx.compose.ui.graphics.Color.Transparent
    val barColor = if (selected) colors.accent else androidx.compose.ui.graphics.Color.Transparent

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .drawBehind {
                    if (selected) {
                        drawRect(selectedFill)
                        drawRect(
                            color = barColor,
                            size = Size(2.dp.toPx(), size.height)
                        )
                    }
                }
                .then(
                    if (onClick != null) {
                        Modifier.clickable(role = Role.Button, onClick = onClick)
                    } else {
                        Modifier
                    }
                )
                .padding(
                    start = KochSpace.l + if (selected) 2.dp else 0.dp,
                    end = KochSpace.l,
                    top = KochSpace.s,
                    bottom = KochSpace.s
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(KochSpace.m))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = KochType.bodyStrong,
                    color = colors.textPrimary,
                    maxLines = titleMaxLines,
                    // Обрезка всегда с многоточием: «Краснодарский край, Россия · Сильный»
                    // читалось как незаконченная фраза (M2a, visual-critic r14).
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = KochType.sub,
                        color = colors.textTertiary,
                        maxLines = subtitleMaxLines,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(KochSpace.m))
                trailing()
            }
        }
        if (showDivider) {
            HorizontalDivider(
                color = colors.textPrimary.copy(alpha = 0.08f),
                thickness = KochSize.dividerSoft
            )
        }
    }
}

/** Пустое состояние: иконка, заголовок и пояснение — по центру, без рамок. */
@Composable
fun KochEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalKochColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KochSpace.x3l, vertical = KochSpace.x2l),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textPrimary.copy(alpha = 0.32f),
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.heightIn(min = KochSpace.m))
        Text(
            text = title,
            style = KochType.bodyStrong,
            color = colors.textPrimary
        )
        Spacer(Modifier.heightIn(min = KochSpace.xs))
        Text(
            text = subtitle,
            style = KochType.sub,
            color = colors.textTertiary
        )
    }
}
