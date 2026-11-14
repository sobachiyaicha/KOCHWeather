package com.koch.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koch.weather.data.model.AppThemeMode
import com.koch.weather.ui.design.KochPrimaryButton
import com.koch.weather.ui.design.KochSheet
import com.koch.weather.ui.design.KochSheetVariant
import com.koch.weather.ui.design.KochChip
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * «Поздравляем!» / рулетка тем — инсет-лист по refs/congrats.png (1:24):
 * светло-голубой градиент, две карточки «Оформление …» (сверху и снизу), телефон в центре,
 * жёлтые ◀ ▶, капсула «Продолжить». Фиолетовый дизайн удалён.
 *
 * Закрытые темы не применяются: «Продолжить» активна только для темы из [availableThemes].
 */
@Composable
fun ThemeRouletteDialog(
    currentTheme: AppThemeMode,
    availableThemes: Set<AppThemeMode>,
    iphoneImageUrl: String? = null,
    onSelectTheme: (AppThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKochColors.current
    val allThemes = remember { AppThemeMode.values().toList() }
    val unlocked = remember(availableThemes) {
        allThemes.filter { it in availableThemes }
    }
    var previewTheme by remember {
        mutableStateOf(if (currentTheme in availableThemes) currentTheme else unlocked.firstOrNull() ?: currentTheme)
    }

    // Автопрокрут как в видео: телефон «крутится» при открытии и останавливается на доступной теме.
    LaunchedEffect(Unit) {
        if (unlocked.isEmpty()) return@LaunchedEffect
        repeat(7) { step ->
            delay(80L + step * 20L)
            previewTheme = unlocked[Random.nextInt(unlocked.size)]
        }
    }

    val isPreviewUnlocked = previewTheme in availableThemes
    val above = neighbourTheme(previewTheme, allThemes, -1)
    val below = neighbourTheme(previewTheme, allThemes, +1)

    KochSheet(
        onDismiss = onDismiss,
        variant = KochSheetVariant.FullScreenInset,
        maxHeightFraction = 0.94f,
        content = {
            Text(
                text = "Поздравляем!",
                style = KochType.titleSheet,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = KochSpace.s)
            )

            ThemeTicket(
                theme = above,
                isUnlocked = above in availableThemes,
                onClick = { previewTheme = above },
                modifier = Modifier.padding(top = KochSpace.s)
            )

            Spacer(Modifier.height(KochSpace.s))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                YellowArrow(pointsRight = false) {
                    previewTheme = neighbourTheme(previewTheme, allThemes, -1)
                }

                PhonePreview(
                    theme = previewTheme,
                    photoUrl = iphoneImageUrl,
                    modifier = Modifier.weight(1f).padding(horizontal = KochSpace.s)
                )

                YellowArrow(pointsRight = true) {
                    previewTheme = neighbourTheme(previewTheme, allThemes, +1)
                }
            }

            Spacer(Modifier.height(KochSpace.m))

            ThemeTicket(
                theme = below,
                isUnlocked = below in availableThemes,
                onClick = { previewTheme = below }
            )

            Spacer(Modifier.height(KochSpace.xs))

            // Чипы тем: перенос по строкам (FlowRow), поэтому ни один не режется краем экрана.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KochSpace.s),
                verticalArrangement = Arrangement.spacedBy(KochSpace.s)
            ) {
                allThemes.forEach { theme ->
                    KochChip(
                        text = theme.titleRu,
                        selected = theme == previewTheme,
                        locked = theme !in availableThemes,
                        onClick = { previewTheme = theme }
                    )
                }
            }

            Spacer(Modifier.height(KochSpace.s))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPreviewUnlocked) {
                    Text(
                        text = "Доступно",
                        style = KochType.caption,
                        color = colors.accentSuccess
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Открывается в лутбоксах",
                        style = KochType.caption,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        },
        footer = {
            Column {
                KochPrimaryButton(
                    text = if (isPreviewUnlocked) "Продолжить" else "Тема закрыта",
                    enabled = isPreviewUnlocked,
                    onClick = {
                        onSelectTheme(previewTheme)
                        onDismiss()
                    }
                )
                if (!isPreviewUnlocked) {
                    Spacer(Modifier.height(KochSpace.s))
                    Text(
                        text = "Эту тему можно получить только в лутбоксах",
                        style = KochType.caption,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    )
}

private fun neighbourTheme(
    theme: AppThemeMode,
    all: List<AppThemeMode>,
    delta: Int
): AppThemeMode {
    val index = all.indexOf(theme).coerceAtLeast(0)
    val next = (index + delta + all.size) % all.size
    return all[next]
}

/** Карточка-«билет» с превью темы: подпись «Оформление …», замок у закрытой. */
@Composable
private fun ThemeTicket(
    theme: AppThemeMode,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(KochRadius.input)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .clip(shape)
            .background(themePreviewBrush(theme))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = KochSpace.l, vertical = KochSpace.s),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
        if (!isUnlocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Тема закрыта",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Телефон в центре: реальное фронтальное фото (ImageCategory.IPHONE_FRONT) с обязательным
 * фолбэком на мокап из Compose — если фото не загрузилось, остаётся фронтальный экран,
 * а не серая заглушка. Заднюю крышку не используем.
 */
@Composable
private fun PhonePreview(
    theme: AppThemeMode,
    photoUrl: String?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .width(118.dp)
            .height(160.dp)
            .clip(shape)
            .background(Color(0xFF101418))
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl != null) {
            RemotePhoto(
                url = photoUrl,
                contentDescription = "iPhone с оформлением ${theme.titleRu}",
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(20.dp),
                // Fit, а не Crop: фронтальное фото должно попадать в рамку целиком,
                // иначе от телефона остаётся кусок обоев.
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                fallback = { PhoneMock(theme) }
            )
        } else {
            PhoneMock(theme)
        }
    }
}

@Composable
private fun PhoneMock(theme: AppThemeMode) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(themePreviewBrush(theme))
            .padding(KochSpace.s),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(34.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.Black.copy(alpha = 0.75f))
        )
        Spacer(Modifier.height(KochSpace.m))
        Text(
            text = theme.titleRu,
            style = KochType.microCaps,
            color = Color.White.copy(alpha = 0.8f),
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = "21°",
            style = KochType.displayTemp.copy(fontSize = 34.sp, lineHeight = 36.sp),
            color = Color.White.copy(alpha = 0.92f)
        )
        Spacer(Modifier.height(KochSpace.s))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.16f))
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.12f))
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White.copy(alpha = 0.75f))
                .padding(vertical = 5.dp),
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

/** Жёлтый треугольник без круга и обводки (refs/congrats.png, цвет заморожен: #FFD600). */
@Composable
private fun YellowArrow(
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
