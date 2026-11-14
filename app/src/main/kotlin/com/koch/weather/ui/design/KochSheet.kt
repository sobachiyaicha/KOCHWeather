package com.koch.weather.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.koch.weather.ui.theme.KochBorderWidth
import com.koch.weather.ui.theme.KochScrim
import com.koch.weather.ui.theme.KochShape
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.LocalKochColors

/** Два легальных варианта листа из референса: снизу и инсет-панель во весь экран. */
enum class KochSheetVariant { Bottom, FullScreenInset }

/**
 * Единственный контейнер оверлеев приложения (см. план §2.13, §6).
 *
 * - [variant] `Bottom` — прижат к низу, верхние углы 32, как `refs/grass_lock.png` (2:44);
 *   `FullScreenInset` — отступ 8 dp от краёв, радиус 32, как `refs/congrats.png` (1:24).
 * - [blocking] — лист нельзя закрыть тапом/назад (модалка «Пора на улицу!»).
 * - [footer] — закреплённая CTA; контент скроллится под ней.
 */
@Composable
fun KochSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    variant: KochSheetVariant = KochSheetVariant.Bottom,
    blocking: Boolean = false,
    showHandle: Boolean = true,
    maxHeightFraction: Float = 0.86f,
    hero: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
    footer: (@Composable BoxScope.() -> Unit)? = null
) {
    val colors = LocalKochColors.current

    Dialog(
        onDismissRequest = { if (!blocking) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !blocking,
            dismissOnClickOutside = !blocking,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val capHeight = maxHeight * maxHeightFraction

            // Затемнение 45 %; тап по нему закрывает лист, тап по самому листу — нет.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KochScrim)
                    .then(
                        if (blocking) {
                            Modifier
                        } else {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss
                            )
                        }
                    )
            )

            val isInset = variant == KochSheetVariant.FullScreenInset

            val sheetModifier = if (isInset) {
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = KochSpace.s, vertical = KochSpace.s)
                    .statusBarsPadding()
            } else {
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = capHeight)
                    .align(Alignment.BottomCenter)
            }

            Surface(
                modifier = sheetModifier.pointerInput(Unit) { detectTapGestures { } },
                shape = if (isInset) KochShape.sheetInset else KochShape.sheetTop,
                color = if (isInset) Color.Transparent else colors.sheetSurface,
                border = androidx.compose.foundation.BorderStroke(
                    KochBorderWidth.hairline,
                    colors.borderStrong
                ),
                shadowElevation = 24.dp,
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = if (isInset) {
                        Modifier.fillMaxSize().background(colors.gradient)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                ) {
                    Column(
                        modifier = if (isInset) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier.fillMaxWidth()
                        }
                    ) {
                        if (showHandle) {
                            KochDragHandle(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(top = KochSpace.s)
                            )
                        }

                        // Фото-герой листа: во всю ширину, обрезается верхними углами листа.
                        if (hero != null) {
                            Box(modifier = Modifier.fillMaxWidth()) { hero() }
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = KochSpace.xl)
                                .padding(top = KochSpace.m, bottom = KochSpace.s)
                        ) {
                            content()
                        }

                        if (footer != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(
                                        start = KochSpace.xl,
                                        end = KochSpace.xl,
                                        top = KochSpace.m,
                                        bottom = KochSpace.l
                                    )
                            ) {
                                footer()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Подтверждение действия в том же языке (deep links Яндекс Go/Еда, покупка).
 * Отдельный маленький лист, чтобы не тащить в проект 9 разных AlertDialog.
 */
@Composable
fun KochConfirmSheet(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    loading: Boolean = false,
    errorText: String? = null
) {
    val colors = LocalKochColors.current
    KochSheet(
        onDismiss = onDismiss,
        maxHeightFraction = 0.6f,
        content = {
            androidx.compose.material3.Text(
                text = title,
                style = com.koch.weather.ui.theme.KochType.titleSheet,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = KochSpace.m, bottom = KochSpace.s)
            )
            androidx.compose.material3.Text(
                text = message,
                style = com.koch.weather.ui.theme.KochType.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = KochSpace.m)
            )
            errorText?.let {
                androidx.compose.material3.Text(
                    text = it,
                    style = com.koch.weather.ui.theme.KochType.sub,
                    color = colors.accentDanger,
                    modifier = Modifier.padding(bottom = KochSpace.s)
                )
            }
        },
        footer = {
            Column {
                KochPrimaryButton(
                    text = confirmText,
                    onClick = onConfirm,
                    loading = loading,
                    enabled = !loading
                )
                androidx.compose.foundation.layout.Spacer(
                    Modifier.padding(top = KochSpace.s)
                )
                KochTextButton(
                    text = dismissText,
                    onClick = onDismiss,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

/** Общий градиент верхней подсветки карточек: единственный разрешённый «блик». */
internal fun topHighlightBrush(): Brush = Brush.verticalGradient(
    0f to Color(0x1FFFFFFF),
    0.45f to Color(0x00FFFFFF)
)
