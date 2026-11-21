package com.koch.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koch.weather.data.model.AppThemeMode
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType

/**
 * Экран «Поздравляем!» для выпавшей в лутбоксе темы (item 4 фазы 5).
 *
 * Одна рулетка на приложение: отдельного `ThemeRouletteDialog` больше нет, экран живёт
 * внутри [LootboxDialog] и открывается только после реального дропа темы.
 * Внешний вид — refs/congrats.png (1:24): телефон в центре, карточки «Оформление …»
 * сверху и снизу, жёлтые стрелки `#FFD600`, капсула «Применить»/«Продолжить».
 *
 * Стрелки листают ТОЛЬКО доступные темы: закрытую тему нельзя «примерить» и применить.
 */
class ThemeRevealState(
    val available: List<AppThemeMode>,
    initial: AppThemeMode
) {
    var preview by mutableStateOf(initial)
        private set

    /** Соседняя доступная тема: −1 — предыдущая, +1 — следующая (по кругу). */
    fun step(delta: Int) {
        if (available.size < 2) return
        val index = available.indexOf(preview).takeIf { it >= 0 } ?: 0
        preview = available[(index + delta + available.size).mod(available.size)]
    }

    fun select(theme: AppThemeMode) {
        preview = theme
    }

    private fun neighbour(delta: Int): AppThemeMode? {
        if (available.isEmpty()) return null
        if (available.size == 1) return available.first()
        val index = available.indexOf(preview).takeIf { it >= 0 } ?: 0
        return available[(index + delta + available.size).mod(available.size)]
    }

    val above: AppThemeMode? get() = neighbour(-1)
    val below: AppThemeMode? get() = neighbour(+1)
}

@Composable
internal fun rememberThemeRevealState(
    availableThemes: Set<AppThemeMode>,
    initial: AppThemeMode
): ThemeRevealState {
    // Порядок — как в каталоге тем, чтобы стрелки не «прыгали» от порядка Set.
    val available = remember(availableThemes) {
        AppThemeMode.values().filter { it in availableThemes }
    }
    return remember(available, initial) { ThemeRevealState(available, initial) }
}

/**
 * Контент экрана «Поздравляем!»: заголовок, карточки оформления, телефон, стрелки, статус.
 * Кнопки («Применить»/«Продолжить») — в футере листа, поэтому состояние вынесено наружу.
 */
@Composable
internal fun ThemeRevealContent(
    state: ThemeRevealState,
    unlockedThemes: Set<AppThemeMode>,
    droppedTheme: AppThemeMode,
    modifier: Modifier = Modifier
) {
    val isUnlocked = state.preview in unlockedThemes
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Поздравляем!",
            style = KochType.titleSheet,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = KochSpace.s)
        )
        // Заголовок — про ФАКТИЧЕСКИ выпавшую тему, а не про превью: стрелки ниже
        // листают оформление, но дроп один (раньше текст «врал» после листания).
        Text(
            text = "Вам выпала тема «${droppedTheme.titleRu}»",
            style = KochType.sub,
            color = Color.White.copy(alpha = 0.92f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
        )

        Spacer(Modifier.height(KochSpace.m))

        state.above?.let { theme ->
            ThemeTicket(
                theme = theme,
                onClick = { state.select(theme) }
            )
            Spacer(Modifier.height(KochSpace.s))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            YellowArrow(pointsRight = false) { state.step(-1) }
            PhonePreview(theme = state.preview, modifier = Modifier.padding(horizontal = KochSpace.s))
            YellowArrow(pointsRight = true) { state.step(+1) }
        }
        // Статус темы — сразу под телефоном: пилюля относится к ВЫБРАННОЙ теме в превью,
        // а не висит отдельно между нижним билетом и CTA (m5, visual-critic r14).
        // Светлая пилюля + тёмный текст — контраст ≥4.5:1 на любой теме.
        val statusText = if (isUnlocked) Color(0xFF2E7D32) else Color(0xFF8A5A00)
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Color(0xE6FFFFFF))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isUnlocked) Icons.Default.Check else Icons.Default.Lock,
                contentDescription = null,
                tint = statusText,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (isUnlocked) "Доступно" else "В лутбоксах",
                style = KochType.caption,
                color = statusText
            )
        }
        // Подпись к стрелкам: листается только превью, приз уже выдан.
        Text(
            text = "Смотрите другие темы",
            style = KochType.caption,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
        )

        Spacer(Modifier.height(KochSpace.s))

        state.below?.let { theme ->
            ThemeTicket(
                theme = theme,
                onClick = { state.select(theme) }
            )
            Spacer(Modifier.height(KochSpace.s))
        }
    }
}

/**
 * Телефон с локальным фоном темы. Фон — уменьшенная копия `AppThemeMode.backgroundKey`
 * ([ThemePreviewImage], `inSampleSize = 4`): мокап 118×160 dp не требует полного
 * декодирования 1080×1920, а память важна, пока открыт лист лутбокса.
 *
 * Дроп-подпись темы внутри мокапа обязана читаться ЦЕЛИКОМ: кегль `KochType.tab`
 * (10 sp) вместо капса с letterSpacing 1.1, `maxLines = 2`, центр. Блоки ниже
 * зафиксированы по высоте, чтобы мокап не обрезал нижнюю плашку «Главная».
 */
@Composable
internal fun PhonePreview(
    theme: AppThemeMode,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(118.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF101418))
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
        ) {
            ThemePreviewImage(theme = theme, modifier = Modifier.fillMaxSize())
            // Затемнение под белый текст мокапа.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(Color(0x33000000), Color(0x8C000000)))
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = KochSpace.s, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                )
                Text(
                    text = theme.titleRu,
                    style = KochType.tab,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    // Обрезки нет: длинные названия («Неоновый киберпанк») переносятся.
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "21°",
                    style = KochType.displayTemp.copy(fontSize = 30.sp, lineHeight = 30.sp),
                    color = Color.White.copy(alpha = 0.92f)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color.White.copy(alpha = 0.75f))
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Главная",
                        style = KochType.tab,
                        color = Color(0xFF20313D)
                    )
                }
            }
        }
    }
}

/**
 * Карточка-«билет» как в refs/congrats.png: локальный фон темы, диагональные штрихи,
 * пунктирная рамка, подпись «Оформление …». Тап выбирает тему в превью.
 */
@Composable
internal fun ThemeTicket(
    theme: AppThemeMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(KochRadius.input)
    val dashColor = Color.White.copy(alpha = 0.6f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        ThemePreviewImage(theme = theme, modifier = Modifier.matchParentSize())
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0x4D000000))
                .drawBehind {
                    // Текстура: диагональные штрихи поверх фото темы.
                    val step = 14.dp.toPx()
                    val stroke = 2.dp.toPx()
                    var x = -size.height
                    while (x < size.width + size.height) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(x, 0f),
                            end = Offset(x + size.height, size.height),
                            strokeWidth = stroke
                        )
                        x += step
                    }
                    // Пунктирная рамка «билета».
                    drawRoundRect(
                        color = dashColor,
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(7.dp.toPx(), 5.dp.toPx())
                            )
                        ),
                        cornerRadius = CornerRadius(KochRadius.input.toPx())
                    )
                }
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = KochSpace.l, vertical = KochSpace.s)
        ) {
            Text(
                text = "Оформление",
                style = KochType.caption,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
            Text(
                text = theme.titleRu,
                style = KochType.bodyStrong,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

/** Жёлтый треугольник без круга и обводки (refs/congrats.png, цвет заморожен: #FFD600). */
@Composable
internal fun YellowArrow(
    pointsRight: Boolean,
    onClick: () -> Unit
) {
    val yellow = Color(0xFFFFD600)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(percent = 50))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(26.dp)) {
            val path = Path().apply {
                if (pointsRight) {
                    moveTo(0f, 0f)
                    lineTo(size.width, size.height / 2f)
                    lineTo(0f, size.height)
                } else {
                    moveTo(size.width, 0f)
                    lineTo(0f, size.height / 2f)
                    lineTo(size.width, size.height)
                }
                close()
            }
            drawPath(path = path, color = yellow)
        }
    }
}
