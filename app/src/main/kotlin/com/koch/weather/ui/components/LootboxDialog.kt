package com.koch.weather.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.annotation.AnyRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.koch.weather.R
import com.koch.weather.data.model.AppThemeMode
import com.koch.weather.data.model.LootRarity
import com.koch.weather.data.model.LootReward
import com.koch.weather.data.model.LootRewardType
import com.koch.weather.data.model.LootboxAttemptsPolicy
import com.koch.weather.data.model.LootboxState
import com.koch.weather.data.model.ThemePreferenceCodec
import com.koch.weather.data.model.WmoHelper
import com.koch.weather.ui.design.KochSheet
import com.koch.weather.ui.design.KochSheetVariant
import com.koch.weather.ui.design.KochTextButton
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochShape
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin
import kotlin.random.Random

// --- палитра лутбокса: «карбоновая» панель + тёплый оранжевый акцент ---------
// Отдельная от темы приложения палитра: лутбокс — игровая сцена, он одинаков на всех
// семи темах (как экран награды в Clash Royale / Dixlo Open Case).
private val LootOrangeDark = Color(0xFFFF8A3D)
private val LootOrangeLight = Color(0xFFFFB74D)
private val LootPanelTop = Color(0xFF1C1511)
private val LootPanelBottom = Color(0xFF0C0907)
private val LootCardTop = Color(0xFF2A1E15)
private val LootCardBottom = Color(0xFF17100B)
private val LootWarmWhite = Color(0xFFFDF3E6)
private val LootWarmSecondary = Color(0xFFC3AC94)
private val LootWarmTertiary = Color(0xFF8E7A66)

/**
 * Лутбокс (полная переработка внешнего вида, референс — Clash Royale / Dixlo Open Case).
 *
 * Сцена: тёмная карбоновая панель, сундук по центру, над ним направленный вверх световой
 * луч ([LightBeam]), по редкости последнего приза — цветное свечение ([RarityAura]).
 * Кнопка «ОТКРЫТЬ» — крупная (56 dp, оранжевый градиент, тень) в закреплённом футере.
 *
 * Результат: карточка приза в центре ([LootPrizeCard]) — «ВЫПАЛО:»/«ВЫПАЛА:» из
 * `reward.revealVerbRu`, название ≥ 22 sp, подзаголовок и плашка редкости цветом
 * `rarity.colorHex`; появление — scaleIn(0.8) + fadeIn + вспышка.
 *
 * История дропов — подменю-аккордеон, закрытое по умолчанию: свёрнутое состояние не
 * рендерит список вообще (до 20 последних наград, внутренний скролл).
 *
 * Сохранено: тряска сундука, вибрация, конфетти 72 частицы и экран «Поздравляем!»
 * для выпавшей темы ([ThemeRevealContent], разблокировка применяется через `onApplyTheme`).
 *
 * Бонусные попытки: приз с `grantsExtraAttempt` показывает строку «+1 попытка» на карточке;
 * пока попыток меньше пяти и бонусный запас не выбран, под кнопкой «ОТКРЫТЬ» есть
 * «Смотреть рекламу → +1 попытка». Ролик (`ad_lion` / `ad_slippers`) играет полноэкранно
 * без контролов поверх диалога; по завершении — автовозврат и [onGrantAdAttempt].
 */
@Composable
fun LootboxDialog(
    lootboxState: LootboxState,
    availableThemes: Set<AppThemeMode>,
    currentTheme: AppThemeMode,
    onOpenLootbox: () -> Unit,
    onApplyTheme: (AppThemeMode) -> Unit,
    onGrantAdAttempt: () -> Unit,
    onDismiss: () -> Unit
) {
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

    // Ролик «+1 попытка за рекламу». null — диалог рекламы не показан.
    // Ресурс чередуется на каждый просмотр, чтобы один и тот же ролик не шёл дважды.
    var adVideoRes by remember { mutableStateOf<Int?>(null) }
    var adViews by remember { mutableIntStateOf(0) }

    // --- анимация сундука ---------------------------------------------------
    val shake = remember { Animatable(0f) }
    val lid = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    val confetti = remember { Animatable(0f) }
    val cardScale = remember { Animatable(0.8f) }
    val cardAlpha = remember { Animatable(0f) }
    val revealFlash = remember { Animatable(0f) }
    var confettiVisible by remember { mutableStateOf(false) }
    var burstSeed by remember { mutableIntStateOf(0) }
    var burstRarity by remember { mutableStateOf(LootRarity.COMMON) }
    var seenSeq by remember { mutableIntStateOf(lootboxState.lastRewardSeq) }

    // Тряска + крышка + вспышка — на открытие (isOpening), а не на дроп.
    //
    // Тайминги подчинены одному правилу: вся последовательность короче 1200 мс, после
    // которых VM отдаёт награду и гасит `isOpening` (смена ключа отменяет этот эффект).
    // Поэтому вспышка идёт ПАРАЛЛЕЛЬНО крышке: 700 (тряска) + 290 (вспышка) = 990 мс.
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

    // Появление карточки приза: scaleIn(0.8) + fadeIn + короткая вспышка.
    // Ключ — номер дропа, поэтому повторно выпавшая та же награда тоже анимируется.
    LaunchedEffect(lootboxState.lastRewardSeq, lastReward?.id) {
        if (lastReward == null) return@LaunchedEffect
        cardScale.snapTo(0.8f)
        cardAlpha.snapTo(0f)
        revealFlash.snapTo(1f)
        coroutineScope {
            launch {
                cardScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.62f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch { cardAlpha.animateTo(1f, tween(200, easing = FastOutSlowInEasing)) }
            launch { revealFlash.animateTo(0f, tween(520, easing = FastOutSlowInEasing)) }
        }
    }

    val activeReveal = if (revealOpen) revealState else null
    val showReveal = activeReveal != null

    KochSheet(
        onDismiss = onDismiss,
        variant = if (showReveal) KochSheetVariant.FullScreenInset else KochSheetVariant.Bottom,
        // Сцена 256 dp + карточка приза + аккордеон истории: 0.94 даёт им место,
        // а контент остаётся скроллируемым на невысоких экранах (KochSheet).
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
                LootboxPanel {
                    LootboxScene(
                        isOpening = lootboxState.isOpening,
                        rarityColor = Color((lastReward?.rarity ?: burstRarity).colorHex),
                        shake = shake.value,
                        lid = lid.value,
                        flash = flash.value,
                        confettiVisible = confettiVisible,
                        confettiProgress = confetti.value,
                        burstRarity = burstRarity,
                        burstSeed = burstSeed
                    )

                    AttemptsLine(attemptsLeft = lootboxState.attemptsLeft)

                    if (lastReward != null) {
                        LootPrizeCard(
                            reward = lastReward,
                            scale = cardScale.value,
                            alpha = cardAlpha.value,
                            flash = revealFlash.value
                        )
                    }

                    Spacer(Modifier.height(KochSpace.s))

                    LootHistoryAccordion(history = lootboxState.history)
                }
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
                    LootboxOpenButton(
                        text = when {
                            !isUnlocked -> "ТЕМА ЗАКРЫТА"
                            isApplied -> "ТЕМА ПРИМЕНЕНА"
                            else -> "ПРИМЕНИТЬ"
                        },
                        enabled = isUnlocked && !isApplied,
                        loading = false,
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
                Column {
                    LootboxOpenButton(
                        text = "ОТКРЫТЬ",
                        enabled = lootboxState.attemptsLeft > 0 && !lootboxState.isOpening,
                        loading = lootboxState.isOpening,
                        onClick = onOpenLootbox
                    )
                    // «Смотреть рекламу → +1 попытка» — пока попыток меньше пяти и не выбран
                    // бонусный запас (5 бонусов, LootboxAttemptsPolicy): мёртвой кнопки,
                    // которая уже ничего не начислит, не бывает.
                    val showAdAttempt = lootboxState.attemptsLeft < LootboxState.FREE_ATTEMPTS_TOTAL &&
                        LootboxAttemptsPolicy.canGrantAdAttempt(lootboxState.attemptsUsed)
                    if (showAdAttempt) {
                        Spacer(Modifier.height(KochSpace.s))
                        LootAdAttemptButton(
                            onClick = {
                                // Чередуем два ролика («лев в очках» и «тапки»).
                                adVideoRes = if (adViews % 2 == 0) {
                                    R.raw.ad_lion
                                } else {
                                    R.raw.ad_slippers
                                }
                            }
                        )
                    }
                }
            }
        }
    )

    // Полноэкранный ролик: рендерится отдельным окном поверх листа лутбокса,
    // без контролов плеера; по завершении возвращаемся и начисляем попытку.
    adVideoRes?.let { videoRes ->
        RewardAdDialog(
            videoRes = videoRes,
            onFinished = {
                adVideoRes = null
                // Следующий бонусный ролик — другой.
                adViews += 1
                onGrantAdAttempt()
            },
            onClosed = { adVideoRes = null }
        )
    }
}

/**
 * Тёмная карбоновая панель лутбокса: глубокий вертикальный градиент, тканая
 * диагональная текстура карбона, тонкая оранжевая кромка сверху и рамка.
 * Внутри — сцена, карточка приза и аккордеон истории.
 */
@Composable
private fun LootboxPanel(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(KochRadius.cardLg)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(LootPanelTop, LootPanelBottom)))
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(LootOrangeLight.copy(alpha = 0.45f), Color.White.copy(alpha = 0.08f))
                    )
                ),
                shape
            )
            .drawBehind {
                // Карбоновая «плетёнка»: диагональные штрихи в двух направлениях.
                val step = 7.dp.toPx()
                val hair = 1.dp.toPx()
                var x = -size.height
                while (x < size.width + size.height) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.018f),
                        start = Offset(x, 0f),
                        end = Offset(x + size.height, size.height),
                        strokeWidth = hair
                    )
                    drawLine(
                        color = Color.Black.copy(alpha = 0.16f),
                        start = Offset(x, size.height),
                        end = Offset(x + size.height, 0f),
                        strokeWidth = hair
                    )
                    x += step
                }
                // Тёплая кромка сверху: панель «подсвечена» оранжевым акцентом.
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(LootOrangeLight.copy(alpha = 0.16f), Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.16f
                    ),
                    size = Size(size.width, size.height * 0.16f)
                )
            }
            .padding(horizontal = KochSpace.m, vertical = KochSpace.m)
    ) {
        content()
    }
}

/**
 * Сцена: световой луч, свечение по редкости, сундук по центру, вспышка и конфетти.
 * Всё в одном боксе фиксированной высоты, чтобы лист не «прыгал» между состояниями.
 */
@Composable
private fun LootboxScene(
    isOpening: Boolean,
    rarityColor: Color,
    shake: Float,
    lid: Float,
    flash: Float,
    confettiVisible: Boolean,
    confettiProgress: Float,
    burstRarity: LootRarity,
    burstSeed: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(256.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        LightBeam(
            intensity = if (isOpening) 1f else 0.55f,
            modifier = Modifier.fillMaxSize()
        )

        RarityAura(
            color = rarityColor,
            isOpening = isOpening,
            modifier = Modifier.fillMaxSize()
        )

        ChestCanvas(
            lidOpen = lid.coerceIn(0f, 1f),
            shake = shake,
            modifier = Modifier
                .padding(bottom = KochSpace.s)
                .width(170.dp)
                .height(148.dp)
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
                                center = Offset(size.width * 0.5f, size.height * 0.72f),
                                radius = size.minDimension * 0.62f
                            ),
                            radius = size.minDimension * 0.62f,
                            center = Offset(size.width * 0.5f, size.height * 0.72f)
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
}

/**
 * Направленный вверх световой луч из сундука: три вложенных конуса с вертикальным
 * градиентом (снизу ярко, сверху прозрачно) — читается как объёмный свет, а не как
 * залитый треугольник. Мягко пульсирует.
 */
@Composable
private fun LightBeam(
    intensity: Float,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "loot_beam")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loot_beam_pulse"
    )
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        // Свет выходит из-под крышки сундука и уходит вверх, за край панели.
        val baseY = size.height * 0.42f
        val strength = (0.35f + 0.45f * intensity) * pulse

        // Конусы: от широкого и почти прозрачного к узкому и яркому.
        val cones = listOf(
            Triple(0.34f, 0.10f, 0.30f),
            Triple(0.24f, 0.075f, 0.50f),
            Triple(0.15f, 0.045f, 0.85f)
        )
        cones.forEach { (halfTopFrac, halfBottomFrac, alpha) ->
            val halfTop = size.width * halfTopFrac
            val halfBottom = size.width * halfBottomFrac
            val path = Path().apply {
                moveTo(cx - halfBottom, baseY)
                lineTo(cx + halfBottom, baseY)
                lineTo(cx + halfTop, 0f)
                lineTo(cx - halfTop, 0f)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.55f to LootOrangeDark.copy(alpha = alpha * 0.20f * strength),
                    1f to Color(0xFFFFE9C4).copy(alpha = alpha * 0.62f * strength),
                    startY = 0f,
                    endY = baseY
                )
            )
        }

        // Яркое «пятно» у основания луча — источник света на крышке.
        drawOval(
            brush = Brush.radialGradient(
                listOf(
                    Color(0xFFFFF0D2).copy(alpha = 0.55f * strength),
                    LootOrangeLight.copy(alpha = 0.18f * strength),
                    Color.Transparent
                ),
                center = Offset(cx, baseY),
                radius = size.width * 0.26f
            ),
            topLeft = Offset(cx - size.width * 0.26f, baseY - size.width * 0.10f),
            size = Size(size.width * 0.52f, size.width * 0.20f)
        )
    }
}

/**
 * Свечение по редкости последнего приза: тёплый ореол вокруг сундука + мягкий отсвет
 * на «пол» сцены. Пульсирует; при открытии усиливается.
 */
@Composable
private fun RarityAura(
    color: Color,
    isOpening: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "loot_aura")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loot_aura_pulse"
    )
    val boost = if (isOpening) 1.35f else 1f
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height * 0.74f
        val radius = size.width * 0.46f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    color.copy(alpha = 0.34f * pulse * boost),
                    color.copy(alpha = 0.12f * pulse * boost),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = radius
            ),
            radius = radius,
            center = Offset(cx, cy)
        )
        // Отсвет на «пол»: эллипс под сундуком.
        drawOval(
            brush = Brush.radialGradient(
                listOf(
                    LootOrangeLight.copy(alpha = 0.26f * pulse * boost),
                    Color.Transparent
                ),
                center = Offset(cx, size.height * 0.92f),
                radius = size.width * 0.40f
            ),
            topLeft = Offset(cx - size.width * 0.36f, size.height * 0.84f),
            size = Size(size.width * 0.72f, size.height * 0.14f)
        )
    }
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
        val scaleFactor = 1f + 0.06f * sin(shake * Math.PI).toFloat()

        rotate(degrees = rotation, pivot = Offset(w * 0.5f, bodyBottom)) {
            val cx = w * 0.5f
            val cy = bodyBottom
            // Масштаб вокруг центра нижней кромки: сундук «приседает» на месте.
            scale(scaleX = scaleFactor, scaleY = scaleFactor, pivot = Offset(cx, cy)) {

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
// Результат: «ВЫПАЛО:» / «ВЫПАЛА:» и карточка приза
// ---------------------------------------------------------------------------

/**
 * Карточка приза в центре диалога. Крупная надпись «ВЫПАЛО:»/«ВЫПАЛА:» берётся из
 * `reward.revealVerbRu` (согласование рода готовит data-слой), название — 24 sp,
 * ниже подзаголовок и плашка редкости цветом `rarity.colorHex`.
 * Появление — `scaleIn(0.8f) + fadeIn` (анимация снаружи, здесь только применение)
 * и вспышка цвета редкости.
 */
@Composable
private fun LootPrizeCard(
    reward: LootReward,
    scale: Float,
    alpha: Float,
    flash: Float
) {
    val rarity = Color(reward.rarity.colorHex)
    val shape = RoundedCornerShape(KochRadius.card)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = KochSpace.xs)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(LootCardTop, LootCardBottom)))
            .border(
                BorderStroke(
                    1.5.dp,
                    Brush.verticalGradient(
                        listOf(rarity.copy(alpha = 0.85f), rarity.copy(alpha = 0.22f))
                    )
                ),
                shape
            )
            .drawBehind {
                // Свечение редкости из верхней кромки карточки.
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(rarity.copy(alpha = 0.26f), Color.Transparent),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.width * 0.75f
                    )
                )
                // Вспышка появления.
                if (flash > 0.01f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = 0.55f * flash),
                                rarity.copy(alpha = 0.22f * flash),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.width * 0.8f
                        )
                    )
                }
            }
            .padding(horizontal = KochSpace.l, vertical = KochSpace.l),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(rarity.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    RewardIcon(reward = reward, tint = rarity)
                }
                Spacer(Modifier.width(KochSpace.s))
                Text(
                    text = "${reward.revealVerbRu.uppercase()}:",
                    style = KochType.microCaps.copy(fontSize = 15.sp, lineHeight = 18.sp, letterSpacing = 2.2.sp),
                    color = LootOrangeLight,
                    maxLines = 1
                )
            }

            Text(
                text = reward.title,
                style = KochType.titleSheet.copy(fontSize = 24.sp, lineHeight = 29.sp),
                color = LootWarmWhite,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = KochSpace.s)
            )

            if (reward.subtitle.isNotBlank()) {
                Text(
                    text = reward.subtitle,
                    style = KochType.body,
                    color = LootWarmSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = KochSpace.xs)
                )
            }

            // Погодный приз: реальная погода из прогноза (не выдуманная).
            val weatherCode = reward.weatherCode
            if (weatherCode != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = KochSpace.s)
                ) {
                    WeatherIcon(
                        iconType = WmoHelper.getIconType(weatherCode, isDay = true),
                        size = 20.dp
                    )
                    Spacer(Modifier.width(KochSpace.xs))
                    Text(
                        text = WmoHelper.getDescriptionRu(weatherCode),
                        style = KochType.caption,
                        color = LootWarmSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(KochSpace.m))
            // Приз-додеп: карточка честно подписывает, что попытка вернулась.
            if (reward.grantsExtraAttempt) {
                ExtraAttemptNote()
                Spacer(Modifier.height(KochSpace.s))
            }
            RarityPlaque(rarity = reward.rarity, big = true)
        }
    }
}

/**
 * Строка «+1 попытка» на карточке приза-додепа: оранжевая плашка с плюсом.
 * Оранжевый акцент читается на карбоне и не конкурирует с плашкой редкости.
 */
@Composable
private fun ExtraAttemptNote() {
    Row(
        modifier = Modifier
            .clip(KochShape.pill)
            .background(LootOrangeDark.copy(alpha = 0.18f))
            .border(1.dp, LootOrangeDark.copy(alpha = 0.65f), KochShape.pill)
            .padding(horizontal = KochSpace.m, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AddCircle,
            contentDescription = null,
            tint = LootOrangeLight,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "+1 попытка",
            style = KochType.chip,
            color = LootOrangeLight,
            maxLines = 1
        )
    }
}

/** Плашка редкости: цвет — `rarity.colorHex`, контраст на тёмном карбоне ≥ 4.5:1. */
@Composable
private fun RarityPlaque(rarity: LootRarity, big: Boolean) {
    val rarityColor = Color(rarity.colorHex)
    Box(
        modifier = Modifier
            .clip(KochShape.pill)
            .background(rarityColor.copy(alpha = 0.20f))
            .border(1.dp, rarityColor.copy(alpha = 0.70f), KochShape.pill)
            .padding(
                horizontal = if (big) KochSpace.m else KochSpace.s,
                vertical = if (big) 6.dp else 3.dp
            )
    ) {
        Text(
            text = rarity.titleRu.uppercase(),
            style = (if (big) KochType.chip else KochType.microCaps)
                .copy(letterSpacing = 1.4.sp),
            color = rarityColor,
            maxLines = 1
        )
    }
}

/** Счётчик попыток: «Осталось попыток: N» либо «Попытки закончились». */
@Composable
private fun AttemptsLine(attemptsLeft: Int) {
    val out = attemptsLeft <= 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = KochSpace.xs),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CardGiftcard,
            contentDescription = null,
            tint = if (out) LootWarmTertiary else LootOrangeLight,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (out) "Попытки закончились" else "Осталось попыток: $attemptsLeft",
            style = KochType.caption,
            color = if (out) LootWarmTertiary else LootWarmSecondary,
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------
// История дропов — аккордеон, закрытый по умолчанию
// ---------------------------------------------------------------------------

/**
 * Подменю «История дропов»: свёрнутое состояние НЕ рендерит список вообще (только
 * строка с шевроном). Раскрытое — до 20 последних наград во внутреннем скролле,
 * чтобы панель не растягивалась на весь экран.
 */
@Composable
private fun LootHistoryAccordion(history: List<LootReward>) {
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "loot_history_chevron"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.07f))
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KochRadius.input))
            .clickable(role = Role.Button) { expanded = !expanded }
            .padding(horizontal = KochSpace.xs, vertical = KochSpace.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "История дропов",
            style = KochType.titleSection,
            color = LootWarmWhite,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "Свернуть историю" else "Развернуть историю",
            tint = LootOrangeLight,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = chevronRotation }
        )
    }

    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(animationSpec = tween(220), expandFrom = Alignment.Top) +
            fadeIn(animationSpec = tween(180)),
        exit = shrinkVertically(animationSpec = tween(180), shrinkTowards = Alignment.Top) +
            fadeOut(animationSpec = tween(120))
    ) {
        if (history.isEmpty()) {
            Text(
                text = "Пока пусто — откройте сундук, и дропы появятся здесь.",
                style = KochType.caption,
                color = LootWarmTertiary,
                modifier = Modifier.padding(
                    start = KochSpace.xs,
                    end = KochSpace.xs,
                    bottom = KochSpace.m
                )
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 212.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = KochSpace.m),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                history.take(LootboxState.MAX_HISTORY).forEach { reward ->
                    LootHistoryRow(reward)
                }
            }
        }
    }
}

/** Строка истории: полоска редкости, название и редкость. Компактно, до 20 строк. */
@Composable
private fun LootHistoryRow(reward: LootReward) {
    val rarity = Color(reward.rarity.colorHex)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KochRadius.input))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = KochSpace.m, vertical = KochSpace.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 26.dp)
                .clip(CircleShape)
                .background(rarity)
        )
        Spacer(Modifier.width(KochSpace.m))
        Text(
            text = reward.title,
            style = KochType.caption,
            color = LootWarmWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(KochSpace.s))
        RarityPlaque(rarity = reward.rarity, big = false)
    }
}

// ---------------------------------------------------------------------------
// Кнопка «ОТКРЫТЬ»
// ---------------------------------------------------------------------------

/**
 * Главная кнопка лутбокса: высота ≥ 56 dp, оранжевый градиент
 * (`#FF8A3D → #FFB74D`), тень, тёмный текст (контраст на градиенте > 7:1) и
 * масштаб 0.98 при нажатии. Неактивное состояние — серый карбон, без тени.
 */
@Composable
private fun LootboxOpenButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val active = enabled && !loading
    val scale by animateFloatAsState(
        targetValue = if (pressed && active) 0.98f else 1f,
        label = "loot_open_scale"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation = if (active) 18.dp else 0.dp, shape = KochShape.pill)
            .clip(KochShape.pill)
            .background(
                if (active) {
                    Brush.horizontalGradient(listOf(LootOrangeDark, LootOrangeLight))
                } else {
                    Brush.horizontalGradient(listOf(Color(0xFF3B332B), Color(0xFF2A231D)))
                }
            )
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
                color = Color(0xFF2A1405),
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                text = text,
                style = KochType.button.copy(
                    fontSize = 17.sp,
                    letterSpacing = 1.4.sp
                ),
                color = if (active) Color(0xFF2A1405) else LootWarmTertiary,
                maxLines = 1
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Реклама за попытку
// ---------------------------------------------------------------------------

/**
 * Вторичная кнопка под «ОТКРЫТЬ»: «Смотреть рекламу → +1 попытка».
 * Карбоновая капсула 44 dp с оранжевой рамкой — заметно, но не спорит с главной CTA.
 * Показывается, пока попыток меньше пяти и бонусный запас за рекламу не выбран.
 */
@Composable
private fun LootAdAttemptButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(KochShape.pill)
            .background(
                if (pressed) Color(0xFF2E2219) else Color(0xFF1E1712)
            )
            .border(1.dp, LootOrangeDark.copy(alpha = 0.55f), KochShape.pill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = KochSpace.m, vertical = KochSpace.s),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = LootOrangeLight,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Смотреть рекламу → +1 попытка",
            style = KochType.caption.copy(fontSize = 13.sp),
            color = LootOrangeLight,
            maxLines = 1
        )
    }
}

/**
 * Полноэкранный ролик «+1 попытка»: `VideoView` без контролов, жестов и паузы,
 * поверх листа лутбокса. По концу ролика — [onFinished] (начисление попытки и
 * автовозврат в диалог). Системный «назад» — безопасный выход без начисления
 * ([onClosed]), чтобы пользователь не остался заперт в рекламе.
 */
@Composable
private fun RewardAdDialog(
    @AnyRes videoRes: Int,
    onFinished: () -> Unit,
    onClosed: () -> Unit
) {
    Dialog(
        onDismissRequest = onClosed,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AdVideoPlayer(
                videoRes = videoRes,
                onEnded = onFinished,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = KochSpace.m, vertical = KochSpace.x2l)
            )
            Text(
                text = "Реклама",
                style = KochType.microCaps,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(KochSpace.l)
            )
            BackHandler(enabled = true) { onClosed() }
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

/** Крупная иконка приза: погода по WMO-коду, тема, предмет. */
@Composable
private fun RewardIcon(reward: LootReward, tint: Color) {
    val weatherCode = reward.weatherCode
    if (weatherCode != null) {
        WeatherIcon(
            iconType = WmoHelper.getIconType(weatherCode, isDay = true),
            size = 22.dp
        )
        return
    }
    // `else` вместо перебора всех LootRewardType: список типов ведёт data-слой,
    // и удаление/добавление типа не должно ломать сборку UI.
    val icon: ImageVector = when (reward.type) {
        LootRewardType.IPHONE -> Icons.Default.PhoneIphone
        LootRewardType.THEME -> Icons.Default.Palette
        LootRewardType.UMBRELLA_DISCOUNT -> Icons.Default.BeachAccess
        LootRewardType.RUBBER_BOOTS -> Icons.AutoMirrored.Filled.DirectionsWalk
        else -> Icons.Default.CardGiftcard
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(20.dp)
    )
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
