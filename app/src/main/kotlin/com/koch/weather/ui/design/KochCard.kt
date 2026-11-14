package com.koch.weather.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import com.koch.weather.ui.theme.KochBorderWidth
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochShape
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.LocalKochColors

/** Три материала из плана §4.7: стекло, плотная поверхность темы, вложенная панель. */
enum class KochMaterial { Glass, Solid, Overlay }

/**
 * Карточка. Никаких цветных обводок: только кант из токена и верхний блик.
 * Материал по умолчанию — стекло на светлой теме и плотная поверхность на тёмной.
 */
@Composable
fun KochCard(
    modifier: Modifier = Modifier,
    material: KochMaterial? = null,
    shape: Shape = KochShape.card,
    padding: PaddingValues = PaddingValues(horizontal = KochSpace.l, vertical = KochSpace.m),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalKochColors.current
    val resolved = material ?: if (colors.isDark) KochMaterial.Solid else KochMaterial.Glass
    val fill = when (resolved) {
        KochMaterial.Glass -> colors.surfaceGlass
        KochMaterial.Solid -> colors.surface
        KochMaterial.Overlay -> colors.surfaceAlt
    }
    val borderColor = when (resolved) {
        KochMaterial.Glass -> colors.border
        KochMaterial.Solid -> colors.borderStrong
        KochMaterial.Overlay -> colors.border
    }
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill)
            .border(KochBorderWidth.hairline, borderColor, shape)
            .then(clickableModifier)
    ) {
        // Верхний блик: единственный «глянец», который разрешён системой.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(topHighlightBrush())
        )
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

/** Вложенная панель (цена, зона загрузки, мини-группа). */
@Composable
fun KochPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(KochRadius.panel),
    padding: PaddingValues = PaddingValues(horizontal = KochSpace.l, vertical = KochSpace.m),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalKochColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceAlt)
            .border(KochBorderWidth.hairline, colors.border, shape)
            .padding(padding),
        content = content
    )
}
