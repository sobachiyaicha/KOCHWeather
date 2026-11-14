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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
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
            // Карточка рисуется ОДНИМ проходом: заливка, кант и верхний блик — это три
            // `drawPath` по одному и тому же контуру. Раньше на каждую карточку в кадре
            // приходилось 4 слоя (`clip` + `background` + `border` + дочерний Box
            // `matchParentSize().background(brush)`), то есть лишний composable-layout-node
            // и лишняя заливка во весь размер — на ленте из ~7 карточек это заметный
            // вклад в 30 FPS. Кисть блика и контур кешируются до смены размера/темы.
            .drawWithCache {
                val outline = shape.createOutline(size, layoutDirection, this)
                val path = when (outline) {
                    is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                    is Outline.Generic -> outline.path
                    is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
                }
                val highlight = topHighlightBrush()
                val borderStroke = Stroke(width = KochBorderWidth.hairline.toPx())
                // Порядок как раньше: заливка → кант → блик поверх (блик лёгкий, 12 % белого).
                onDrawBehind {
                    drawPath(path, fill)
                    drawPath(path, borderColor, style = borderStroke)
                    drawPath(path, highlight)
                }
            }
            .then(clickableModifier)
    ) {
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
