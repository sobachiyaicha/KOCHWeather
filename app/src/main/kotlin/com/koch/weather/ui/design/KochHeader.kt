package com.koch.weather.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.koch.weather.ui.theme.KochSize
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/** Ручка листа: 36×4, белый 35 %. */
@Composable
fun KochDragHandle(modifier: Modifier = Modifier) {
    val colors = LocalKochColors.current
    Box(
        modifier = modifier
            .size(width = KochSize.grabberWidth, height = KochSize.grabberHeight)
            .clip(CircleShape)
            .background(colors.textPrimary.copy(alpha = 0.35f))
    )
}

/** Крестик листа: 44 dp, стеклянный круг, 20 dp иконка. */
@Composable
fun KochCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Закрыть"
) {
    val colors = LocalKochColors.current
    Box(
        modifier = modifier
            .size(KochSize.iconButton)
            .clip(CircleShape)
            .background(colors.textPrimary.copy(alpha = 0.10f))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = contentDescription,
            tint = colors.textPrimary.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Заголовок секции 16/600 + необязательное действие справа. */
@Composable
fun KochSection(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    val colors = LocalKochColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = KochSpace.m, bottom = KochSpace.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = KochType.titleSection,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
    }
}

/**
 * Шапка полноэкранной страницы: 44 dp назад, заголовок 28/700, подпись 12/55 %.
 * Без разделителя — его роль играет отступ 24 dp.
 */
@Composable
fun KochPageHeader(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = LocalKochColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                start = KochSpace.xs,
                end = KochSpace.xl,
                top = KochSpace.xs,
                bottom = KochSpace.s
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        KochIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Назад",
            onClick = onBack,
            tint = colors.textPrimary.copy(alpha = 0.9f),
            containerAlpha = 0f
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = KochType.titlePage,
                color = colors.textPrimary,
                maxLines = 1,
                softWrap = false
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = KochType.sub,
                    color = colors.textTertiary,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        actions()
    }
}
