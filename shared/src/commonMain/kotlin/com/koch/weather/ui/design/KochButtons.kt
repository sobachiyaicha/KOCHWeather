package com.koch.weather.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.koch.weather.ui.theme.KochShape
import com.koch.weather.ui.theme.KochSize
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/**
 * Единственная CTA приложения: капсула 52 dp, синяя, белый текст 16/600.
 * Состояния: normal / pressed (−8 % L + scale 0.98) / disabled (белый 10 %, текст 40 %) / loading.
 */
@Composable
fun KochPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val colors = LocalKochColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val active = enabled && !loading
    val scale by animateFloatAsState(
        targetValue = if (pressed && active) 0.98f else 1f,
        label = "koch_cta_scale"
    )
    val container = when {
        !active -> colors.textPrimary.copy(alpha = 0.10f)
        pressed -> colors.accentPressed
        else -> colors.accent
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = KochSize.cta)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(KochShape.pill)
            .background(container)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = active,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = KochSpace.l, vertical = KochSpace.m),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = text,
                style = KochType.button,
                color = if (active) Color.White else colors.textPrimary.copy(alpha = 0.40f),
                maxLines = 1
            )
        }
    }
}

/** Текстовое действие: без заливки, 14/500, 55 %. Это «Отмена» и вторичные ссылки. */
@Composable
fun KochTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color? = null
) {
    val colors = LocalKochColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier = modifier
            .heightIn(min = KochSize.buttonSecondary)
            .clip(KochShape.pill)
            .background(if (pressed) colors.textPrimary.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = KochSpace.m),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = KochType.body,
            color = (color ?: colors.textSecondary).copy(alpha = if (enabled) 0.9f else 0.35f),
            maxLines = 1
        )
    }
}

/** Иконочная кнопка 44 dp: закрытие, центрирование карты, очистка. */
@Composable
fun KochIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    containerAlpha: Float = 0.12f
) {
    val colors = LocalKochColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier = modifier
            .size(KochSize.iconButton)
            .clip(CircleShape)
            .background(colors.textPrimary.copy(alpha = if (pressed) 0.22f else containerAlpha))
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
