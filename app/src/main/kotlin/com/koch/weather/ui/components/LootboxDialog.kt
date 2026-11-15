package com.koch.weather.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koch.weather.data.model.AppThemeMode
import com.koch.weather.data.model.LootRarity
import com.koch.weather.data.model.LootReward
import com.koch.weather.data.model.LootRewardType
import com.koch.weather.data.model.LootboxState
import com.koch.weather.data.model.ThemePreferenceCodec
import com.koch.weather.data.model.WmoHelper
import com.koch.weather.ui.design.KochCard
import com.koch.weather.ui.design.KochEmptyState
import com.koch.weather.ui.design.KochMaterial
import com.koch.weather.ui.design.KochPrimaryButton
import com.koch.weather.ui.design.KochSheet
import com.koch.weather.ui.design.KochSheetVariant
import com.koch.weather.ui.design.KochTextButton
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochShape
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin
import kotlin.random.Random

/**
 * Лутбокс (item 8 фазы 5) — не «иконка в ореоле», а настоящий сундук с анимацией:
 *
 * 1. Закрытое состояние: нарисованный `Canvas`-сундук (корпус, крышка, металлические полосы,
 *    замок, блик), мягкий пульсирующий ореол, подпись «Сундук Утешения #2025» и честный шанс iPhone.
 * 2. «Открыть бесплатно» → `isOpening`: тряска сундука 700 мс (поворот ±4°, масштаб 1→1.06),
 *    крышка приоткрывается, вспышка. Никакого спама анимацией на каждую рекомпозицию.
 * 3. Дроп ловится ровно один раз по `lootboxState.lastRewardSeq`: вибрация
 *    (`VibrationEffect.createWaveform`), конфетти 72 частицы цвета редкости (гравитация, 1.2 с),
 *    затем карточка приза и — для темы — экран «Поздравляем!» ([ThemeRevealContent]).
 * 4. История — последние 5 дропов из персистентной `lootboxState.history` с цветом редкости.
 *    Пустая история — `KochEmptyState` с объяснением, а не пустая плашка.
 *
 * Кнопок-обманок нет: у приза-темы «Применить» (`onApplyTheme`), у остальных — «Продолжить»,
 * которая закрывает лист; «Открыть бесплатно» в футере реально открывает сундук.
 */
@Composable
fun LootboxDialog(
    lootboxState: LootboxState,
    availableThemes: Set<AppThemeMode>,
    currentTheme: AppThemeMode,
    onOpenLootbox: () -> Unit,
    onApplyTheme: (AppThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKochColors.current
    val context = LocalContext.current

    val lastReward = lootboxState.lastReward
    // Тема из последнего дропа (null, если дроп не тематический).
    val droppedTheme: AppThemeMode? = remember(lastReward) {
        if (lastReward?.type == LootRewardType.THEME) {
            ThemePreferenceCodec.decode(lastReward.unlockedThemeName)
        } else {
            null
        }
    }

    var revealOpen by remember { mutableStateOf(false) }
    var appliedTheme by remember { mutableStateOf<AppThemeMode?>(null) }
    val revealState = droppedTheme?.let { rememberThemeRevealState(availableThemes, it) }

    // --- анимация сундука ---------------------------------------------------
    val shake = remember { Animatable(0f) }
    val lid = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    val confetti = remember { Animatable(0f) }
    var confettiVisible by remember { mutableStateOf(false) }
    var burstSeed by remember { mutableIntStateOf(0) }
    var burstRarity by remember { mutableStateOf(LootRarity.COMMON) }
    var seenSeq by remember { mutableIntStateOf(lootboxState.lastRewardSeq) }

    // Тряска + крышка + вспышка — на открытие (isOpening), а не на дроп.
    //
    // Тайминги подчинены одному правилу: вся последовательность короче 1200 мс, после
    // которых VM отдаёт награду и гасит `isOpening` (смена ключа отменяет этот эффект).
    // Поэтому вспышка идёт ПАРАЛЛЕЛЬНО крышке: 700 (тряска) + 290 (вспышка) = 990 мс.
    // Раньше было 700 + 240 + 70 + 300 = 1310 мс, и отмена на 1200-й оставляла `flash`
    // на ~0.2–0.4 — белый блик «залипал» до закрытия листа (item 8/14).
    // `finally` + NonCancellable гарантируют `flash == 0f` даже при отмене.
    LaunchedEffect(lootboxState.isOpening) {
        if (!lootboxState.isOpening) return@LaunchedEffect
        revealOpen = false
        shake.snapTo(0f)
        lid.snapTo(0f)
        flash.snapTo(0f)
        try {
            shake.animateTo(targetValue = 1f, animationSpec = tween(700, easing = LinearEasing))
            coroutineScope {
                launch {
                    flash.animateTo(targetValue = 1f, animationSpec = tween(70, easing = LinearEasing))
                    flash.animateTo(targetValue = 0f, animationSpec = tween(220, easing = FastOutSlowInEasing))
                }
                lid.animateTo(targetValue = 1f, animationSpec = tween(240, easing = FastOutSlowInEasing))
            }
        } finally {
            // Отмена (дроп / закрытие листа) не имеет права оставить ненулевой блик.
            withContext(NonCancellable) { flash.snapTo(0f) }
        }
    }

    // Ровно один выстрел анимации/вибрации на дроп: ключ — lastRewardSeq, а не рекомпозиция.
    // `seenSeq` отсекает повтор при переоткрытии листа с той же наградой.
    LaunchedEffect(lootboxState.lastRewardSeq) {
        val seq = lootboxState.lastRewardSeq
        if (seq == seenSeq) return@LaunchedEffect
        seenSeq = seq

        vibrateOnDrop(context)

        // Дроп гасит блик от открытия: даже если эффект `isOpening` был отменён
        // на середине, к конфетти `flash` уже строго 0, а сундук стоит ровно.
        flash.snapTo(0f)
        shake.snapTo(1f)
        lid.snapTo(1f)
        burstSeed = seq
        burstRarity = lastReward?.rarity ?: LootRarity.COMMON
        confettiVisible = true
        confetti.snapTo(0f)
        confetti.animateTo(targetValue = 1f, animationSpec = tween(1200, easing = LinearEasing))
        confettiVisible = false

        if (droppedTheme != null) {
            revealOpen = true
            appliedTheme = null
        }
    }

    val activeReveal = if (revealOpen) revealState else null
    val showReveal = activeReveal != null

    KochSheet(
        onDismiss = onDismiss,
        variant = if (showReveal) KochSheetVariant.FullScreenInset else KochSheetVariant.Bottom,
        // Лист лутбокса — самый плотный в приложении: шапка, сундук, карточка приза и
        // 5 строк истории. 0.88 обрезал историю под CTA (visual-critic r15);
        // 0.94 даёт под неё ~40 dp, а контент остаётся скроллируемым (KochSheet).
        maxHeightFraction = 0.94f,
        content = {
            if (activeReveal != null && droppedTheme != null) {
                ThemeRevealContent(
                    state = activeReveal,
                    unlockedThemes = availableThemes,
                    // Заголовок «Вам выпала тема …» — всегда про выпавшую, не про превью.
                    droppedTheme = droppedTheme
                )
            } else {
                LootboxChestContent(
                    lootboxState = lootboxState,
                    lastReward = lastReward,
                    currentTheme = currentTheme,
                    shake = shake.value,
                    lid = lid.value,
                    flash = flash.value,
                    confettiVisible = confettiVisible,
                    confettiProgress = confetti.value,
                    burstRarity = burstRarity,
                    burstSeed = burstSeed,
                    onApplyTheme = onApplyTheme,
                    onContinue = onDismiss
                )
            }
        },
        footer = {
            if (activeReveal != null) {
                Column {
                    val preview = activeReveal.preview
                    val isUnlocked = preview in availableThemes
                    // Мёртвых кликов нет: уже применённая тема гасит кнопку и подписывается
                    // результатом. Выбрали другую стрелками — снова «Применить».
                    val isApplied = isUnlocked && appliedTheme == preview
                    KochPrimaryButton(
                        text = when {
                            !isUnlocked -> "Тема закрыта"
                            isApplied -> "Тема применена"
                            else -> "Применить"
                        },
                        enabled = isUnlocked && !isApplied,
                        onClick = {
                            if (!isApplied) {
                                onApplyTheme(preview)
                                appliedTheme = preview
                            }
                        }
                    )
                    Spacer(Modifier.height(KochSpace.s))
                    KochTextButton(
                        text = "Продолжить",
                        onClick = { revealOpen = false },
                        modifier = Modifier.fillMaxWidth(),
                        // Инсет-лист на светлой теме получает тёмный скрим (KochSheet):
                        // вторичное действие белым читается, серый сливался с фоном (M6).
                        color = Color.White
                    )
                }
            } else {
                KochPrimaryButton(
                    text = "Открыть бесплатно",
                    onClick = onOpenLootbox,
                    enabled = !lootboxState.isOpening,
                    loading = lootboxState.isOpening
                )
            }
        }
    )
}

/** Содержимое закрытого/открывающегося сундука: герой, карточка приза и история дропов. */
@Composable
private fun LootboxChestContent(
    lootboxState: LootboxState,
    lastReward: LootReward?,
    currentTheme: AppThemeMode,
    shake: Float,
    lid: Float,
    flash: Float,
    confettiVisible: Boolean,
    confettiProgress: Float,
    burstRarity: LootRarity,
    burstSeed: Int,
    onApplyTheme: (AppThemeMode) -> Unit,
    onContinue: () -> Unit
) {
    val colors = LocalKochColors.current

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = KochSpace.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CardGiftcard,
            contentDescription = null,
            tint = colors.accentPromo,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(KochSpace.m))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Погодные Лутбоксы",
                style = KochType.titleCard,
                color = colors.textPrimary
            )
            Text(
                text = "Не повезло с погодой? Повезёт тут!",
                style = KochType.sub,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    // Герой-зона: ореол, сундук, вспышка и конфетти — всё в одном боксе,
    // чтобы лист не «прыгал» при смене состояния.
    // Высота 206 → 124 dp (visual-critic r15): история из 5 строк перестала влезать
    // под сундук в листе 0.88 — освобождённые ~80 dp отданы строкам истории.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .padding(top = KochSpace.xs),
        contentAlignment = Alignment.Center
    ) {
        ChestGlow(isOpening = lootboxState.isOpening)

        ChestCanvas(
            lidOpen = lid.coerceIn(0f, 1f),
            shake = shake,
            modifier = Modifier
                .width(142.dp)
                .height(118.dp)
        )

        // Вспышка при открытии: короткий белый радиальный блик из центра сундука.
        if (flash > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.85f * flash),
                                    Color(0xFFFFE082).copy(alpha = 0.35f * flash),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.minDimension * 0.55f
                            ),
                            radius = size.minDimension * 0.55f,
                            center = center
                        )
                    }
            )
        }

        if (confettiVisible) {
            ConfettiLayer(
                progress = confettiProgress,
                rarityColor = Color(burstRarity.colorHex),
                seed = burstSeed,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = when {
                lootboxState.isOpening -> "Сундук открывается..."
                else -> "Сундук Утешения #2025"
            },
            style = KochType.bodyStrong,
            color = colors.textPrimary
        )
        Text(
            text = "Шанс на iPhone 16 Pro Max: 0,001 %",
            style = KochType.sub,
            color = colors.textSecondary
        )
    }

    if (lastReward != null) {
        Spacer(Modifier.height(KochSpace.s))
        LootRewardCard(
            reward = lastReward,
            currentTheme = currentTheme,
            onApplyTheme = onApplyTheme,
            onContinue = onContinue
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = KochSpace.s, bottom = KochSpace.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "История дропов",
            style = KochType.titleSection,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Открыто: ${lootboxState.totalOpened}",
            style = KochType.caption,
            color = colors.textTertiary
        )
    }

    val recent = remember(lootboxState.history) { lootboxState.history.take(5) }
    if (recent.isEmpty()) {
        KochEmptyState(
            icon = Icons.Default.CardGiftcard,
            title = "Вы ещё не открывали сундуки",
            subtitle = "Нажмите «Открыть бесплатно» — призы появятся здесь."
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(KochSpace.xs)) {
            recent.forEach { reward ->
                LootHistoryRow(reward)
            }
        }
    }

    // Нижний отступ контента: последняя строка истории не прилипает к CTA и к
    // градиентной шторке над футером (KochSheet добавляет свои 24 dp ниже).
    Spacer(Modifier.height(KochSpace.s))
}

/** Мягкий пульсирующий ореол под сундуком. */
@Composable
private fun ChestGlow(isOpening: Boolean) {
    val pulse = rememberInfiniteTransition(label = "chest_glow")
    val glow by pulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chest_glow_alpha"
    )
    val boost = if (isOpening) 1.35f else 1f
    Box(
        modifier = Modifier
            // Ореол подстроен под уменьшенную герой-зону (124 dp), чтобы не диктовать высоту.
            .size(width = 208.dp, height = 118.dp)
            .drawBehind {
                drawOval(
                    brush = Brush.radialGradient(
                        listOf(
                            Color(0xFFFFC65A).copy(alpha = 0.30f * glow * boost),
                            Color(0xFFFF9D2E).copy(alpha = 0.12f * glow * boost),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset.Zero,
                    size = size
                )
            }
    )
}

/**
 * Сундук: корпус с градиентом дерева, металлические полосы, замок, блик и крышка,
 * которая поворачивается вокруг петли. [shake] — прогресс тряски 0..1 (поворот ±4°,
 * масштаб 1→1.06); [lidOpen] — прогресс открытия крышки.
 */
@Composable
private fun ChestCanvas(
    lidOpen: Float,
    shake: Float,
    modifier: Modifier = Modifier
) {
    val woodLight = Color(0xFFC08A4A)
    val woodMid = Color(0xFF8B5A28)
    val woodDark = Color(0xFF5A3714)
    val metal = Color(0xFFD9B45A)
    val metalLight = Color(0xFFF6E4A8)
    val metalDark = Color(0xFF8C6A22)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bodyLeft = w * 0.08f
        val bodyRight = w * 0.92f
        val bodyTop = h * 0.50f
        val bodyBottom = h * 0.94f
        val hingeY = bodyTop

        // Тряска: поворот влево-вправо на ±4° и лёгкий «вдох» масштаба до 1.06.
        val rotation = sin(shake * Math.PI * 6.0).toFloat() * 4f
        val scale = 1f + 0.06f * sin(shake * Math.PI).toFloat()

        rotate(degrees = rotation, pivot = Offset(w * 0.5f, bodyBottom)) {
            val cx = w * 0.5f
            val cy = bodyBottom
            // Масштаб вокруг центра нижней кромки: сундук «приседает» на месте.
            scale(scaleX = scale, scaleY = scale, pivot = Offset(cx, cy)) {

                // Тень под сундуком.
                drawOval(
                    color = Color(0x73000000),
                    topLeft = Offset(w * 0.14f, bodyBottom - h * 0.02f),
                    size = Size(w * 0.72f, h * 0.09f)
                )

                // Корпус.
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(woodLight, woodMid, woodDark),
                        startY = bodyTop,
                        endY = bodyBottom
                    ),
                    topLeft = Offset(bodyLeft, bodyTop),
                    size = Size(bodyRight - bodyLeft, bodyBottom - bodyTop),
                    cornerRadius = CornerRadius(w * 0.05f)
                )

                // Металлические вертикальные полосы.
                val bandWidth = w * 0.055f
                listOf(0.23f, 0.70f).forEach { fx ->
                    drawRoundRect(
                        color = metal,
                        topLeft = Offset(w * fx, bodyTop),
                        size = Size(bandWidth, bodyBottom - bodyTop),
                        cornerRadius = CornerRadius(bandWidth * 0.3f)
                    )
                    drawRoundRect(
                        color = metalLight.copy(alpha = 0.55f),
                        topLeft = Offset(w * fx + bandWidth * 0.18f, bodyTop),
                        size = Size(bandWidth * 0.22f, bodyBottom - bodyTop),
                        cornerRadius = CornerRadius(bandWidth * 0.15f)
                    )
                }

                // Обод корпуса по верхней кромке.
                drawRoundRect(
                    color = metalDark,
                    topLeft = Offset(bodyLeft, bodyTop - h * 0.03f),
                    size = Size(bodyRight - bodyLeft, h * 0.045f),
                    cornerRadius = CornerRadius(w * 0.02f)
                )

                // Замок: металлическая пластина, скважина и «язычок».
                val lockWidth = w * 0.19f
                val lockHeight = h * 0.19f
                drawRoundRect(
                    color = metal,
                    topLeft = Offset(w * 0.5f - lockWidth / 2f, bodyTop - h * 0.01f),
                    size = Size(lockWidth, lockHeight),
                    cornerRadius = CornerRadius(w * 0.03f)
                )
                drawRoundRect(
                    color = metalLight.copy(alpha = 0.6f),
                    topLeft = Offset(w * 0.5f - lockWidth * 0.34f, bodyTop),
                    size = Size(lockWidth * 0.24f, lockHeight * 0.8f),
                    cornerRadius = CornerRadius(w * 0.012f)
                )
                drawCircle(
                    color = Color(0xFF2B1A0C),
                    radius = w * 0.026f,
                    center = Offset(w * 0.5f, bodyTop + h * 0.06f)
                )
                drawRoundRect(
                    color = Color(0xFF2B1A0C),
                    topLeft = Offset(w * 0.5f - w * 0.012f, bodyTop + h * 0.06f),
                    size = Size(w * 0.024f, h * 0.055f),
                    cornerRadius = CornerRadius(w * 0.008f)
                )

                // Крышка: свет из щели, когда она приоткрыта.
                drawRoundRect(
                    color = Color(0xFFFFE9A8).copy(alpha = 0.75f * lidOpen),
                    topLeft = Offset(bodyLeft + w * 0.02f, bodyTop - h * 0.055f),
                    size = Size(bodyRight - bodyLeft - w * 0.04f, h * 0.05f),
                    cornerRadius = CornerRadius(w * 0.02f)
                )

                // Крышка поворачивается вокруг петли на задней кромке.
                rotate(degrees = -lidOpen * 40f, pivot = Offset(w * 0.5f, hingeY)) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(woodLight, woodMid),
                            startY = h * 0.16f,
                            endY = hingeY
                        ),
                        topLeft = Offset(bodyLeft, h * 0.16f),
                        size = Size(bodyRight - bodyLeft, hingeY - h * 0.16f),
                        cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)
                    )
                    // Металлические накладки крышки.
                    listOf(0.23f, 0.70f).forEach { fx ->
                        drawRoundRect(
                            color = metal,
                            topLeft = Offset(w * fx, h * 0.17f),
                            size = Size(bandWidth, hingeY - h * 0.18f),
                            cornerRadius = CornerRadius(bandWidth * 0.3f)
                        )
                    }
                    // Обод крышки.
                    drawRoundRect(
                        color = metalDark,
                        topLeft = Offset(bodyLeft, hingeY - h * 0.07f),
                        size = Size(bodyRight - bodyLeft, h * 0.055f),
                        cornerRadius = CornerRadius(w * 0.02f)
                    )
                    // Блик на крышке.
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.18f),
                        topLeft = Offset(bodyLeft + w * 0.07f, h * 0.20f),
                        size = Size(w * 0.26f, h * 0.055f),
                        cornerRadius = CornerRadius(w * 0.03f)
                    )
                }

                // Блик на корпусе.
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.12f),
                    topLeft = Offset(bodyLeft + w * 0.05f, bodyTop + h * 0.10f),
                    size = Size(w * 0.10f, h * 0.28f),
                    cornerRadius = CornerRadius(w * 0.04f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Конфетти
// ---------------------------------------------------------------------------

private data class ConfettiParticle(
    val startX: Float,
    val startY: Float,
    val driftX: Float,
    val fallSpeed: Float,
    val sizeDp: Float,
    val spin: Float,
    val colorIndex: Int
)

/**
 * 72 частицы с гравитацией: считаются один раз на дроп (сид = номер дропа),
 * кадр лишь пересчитывает позиции — без Random в draw-фазе.
 */
private fun confettiParticles(seed: Int): List<ConfettiParticle> {
    val random = Random(seed * 7919 + 13)
    return List(CONFETTI_COUNT) {
        ConfettiParticle(
            startX = random.nextFloat(),
            startY = random.nextFloat() * 0.18f,
            driftX = (random.nextFloat() - 0.5f) * 0.75f,
            fallSpeed = 0.55f + random.nextFloat() * 0.85f,
            sizeDp = 3f + random.nextFloat() * 4f,
            spin = (random.nextFloat() - 0.5f) * 1080f,
            colorIndex = random.nextInt(7)
        )
    }
}

private const val CONFETTI_COUNT = 72

@Composable
private fun ConfettiLayer(
    progress: Float,
    rarityColor: Color,
    seed: Int,
    modifier: Modifier = Modifier
) {
    val palette = remember(rarityColor) {
        listOf(
            rarityColor,
            Color(0xFFFFB300),
            Color(0xFFAB47BC),
            Color(0xFF2196F3),
            Color(0xFFFF1744),
            Color(0xFF7BC98A),
            Color.White
        )
    }
    val particles = remember(seed) { confettiParticles(seed) }
    Canvas(modifier = modifier) {
        if (progress <= 0f || progress >= 1f) return@Canvas
        val w = size.width
        val h = size.height
        val gravity = progress * progress
        val fade = (1f - ((progress - 0.72f) / 0.28f).coerceIn(0f, 1f))
        particles.forEach { particle ->
            val x = ((particle.startX + particle.driftX * progress) % 1f + 1f) % 1f * w
            val y = (particle.startY + particle.fallSpeed * progress * 0.75f + gravity * 0.75f) * h
            if (y > h) return@forEach
            val particleWidth = particle.sizeDp.dp.toPx()
            val particleHeight = particleWidth * 0.55f
            rotate(degrees = particle.spin * progress, pivot = Offset(x, y)) {
                drawRoundRect(
                    color = palette[particle.colorIndex].copy(alpha = fade),
                    topLeft = Offset(x - particleWidth / 2f, y - particleHeight / 2f),
                    size = Size(particleWidth, particleHeight),
                    cornerRadius = CornerRadius(particleHeight * 0.25f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Карточка приза и история
// ---------------------------------------------------------------------------

@Composable
private fun LootRewardCard(
    reward: LootReward,
    currentTheme: AppThemeMode,
    onApplyTheme: (AppThemeMode) -> Unit,
    onContinue: () -> Unit
) {
    val colors = LocalKochColors.current
    val rarity = Color(reward.rarity.colorHex)
    val theme = if (reward.type == LootRewardType.THEME) {
        ThemePreferenceCodec.decode(reward.unlockedThemeName)
    } else {
        null
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KochRadius.card))
            .background(rarity.copy(alpha = 0.10f))
            .border(1.dp, rarity.copy(alpha = 0.55f), RoundedCornerShape(KochRadius.card))
            .drawBehind {
                // Свечение редкости из левого верхнего угла карточки.
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(rarity.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = size.width * 0.8f
                    )
                )
            }
            // Вертикальные отступы m → s: карточка приза отдаёт ~8 dp истории.
            .padding(horizontal = KochSpace.m, vertical = KochSpace.s)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(rarity.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    RewardIcon(reward = reward, tint = rarity)
                }
                Spacer(Modifier.width(KochSpace.m))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reward.rarity.titleRu.uppercase(),
                        style = KochType.microCaps,
                        color = rarity
                    )
                    Text(
                        text = reward.title,
                        style = KochType.bodyStrong,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = reward.subtitle,
                        style = KochType.sub,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Погодный приз: реальная температура из прогноза, а не выдуманная.
            // Диапазон уже стоит в подзаголовке выше — второй раз не дублируем (m6).
            val weatherCode = reward.weatherCode
            if (weatherCode != null) {
                Spacer(Modifier.height(KochSpace.s))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WeatherIcon(
                        iconType = WmoHelper.getIconType(weatherCode, isDay = true),
                        size = 22.dp
                    )
                    Spacer(Modifier.width(KochSpace.s))
                    Text(
                        text = WmoHelper.getDescriptionRu(weatherCode),
                        style = KochType.body,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(KochSpace.xs))
            RewardActionButton(
                text = when {
                    theme == null -> "Продолжить"
                    theme == currentTheme -> "Применена"
                    else -> "Применить"
                },
                accent = rarity,
                onClick = {
                    if (theme != null) onApplyTheme(theme) else onContinue()
                }
            )
        }
    }
}

@Composable
private fun RewardActionButton(
    text: String,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .clip(KochShape.pill)
            .background(accent.copy(alpha = 0.20f))
            .border(1.dp, accent.copy(alpha = 0.55f), KochShape.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = KochSpace.l, vertical = KochSpace.xs),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = KochType.chip,
            color = Color.White,
            maxLines = 1
        )
    }
}

/** Крупная иконка приза: погода по WMO-коду, тема, предмет — без «одной иконки на всё». */
@Composable
private fun RewardIcon(reward: LootReward, tint: Color) {
    val weatherCode = reward.weatherCode
    if (weatherCode != null) {
        WeatherIcon(
            iconType = WmoHelper.getIconType(weatherCode, isDay = true),
            size = 28.dp
        )
        return
    }
    val icon: ImageVector = when (reward.type) {
        LootRewardType.IPHONE -> Icons.Default.PhoneIphone
        LootRewardType.THEME -> Icons.Default.Palette
        LootRewardType.WEATHER_TOMORROW -> Icons.Default.CardGiftcard
        LootRewardType.UMBRELLA_DISCOUNT -> Icons.Default.BeachAccess
        LootRewardType.RUBBER_BOOTS -> Icons.AutoMirrored.Filled.DirectionsWalk
        LootRewardType.COFFEE -> Icons.Default.LocalCafe
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(26.dp)
    )
}

/** Строка истории: цвет редкости полоской слева, заголовок и редкость. */
@Composable
private fun LootHistoryRow(reward: LootReward) {
    val colors = LocalKochColors.current
    val rarity = Color(reward.rarity.colorHex)
    KochCard(
        material = KochMaterial.Solid,
        padding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = KochSpace.m,
            // Вертикаль s → xs: 5 строк истории дают ~40 dp экономии на листе 0.93.
            vertical = KochSpace.xs
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 32.dp)
                    .clip(CircleShape)
                    .background(rarity)
            )
            Spacer(Modifier.width(KochSpace.m))
            Column(modifier = Modifier.weight(1f)) {
                // M2b: «Вам выпала облачная погода на» обрезалось без «завтра» —
                // вторая строка возвращает формулировку приза целиком.
                Text(
                    text = reward.title,
                    style = KochType.caption,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = reward.rarity.titleRu,
                    style = KochType.sub,
                    color = rarity,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Вибрация
// ---------------------------------------------------------------------------

/**
 * Один выстрел вибрации на дроп. API ≥ 31 — `VibratorManager.defaultVibrator`,
 * API ≥ 26 — `VibrationEffect.createWaveform(0,40,60,40, -1)`, ниже — `vibrate(120)`.
 * Разрешение `android.permission.VIBRATE` объявлено в манифесте.
 */
@Suppress("DEPRECATION")
private fun vibrateOnDrop(context: Context) {
    val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    if (vibrator == null || !vibrator.hasVibrator()) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 40), -1))
    } else {
        vibrator.vibrate(120)
    }
}
