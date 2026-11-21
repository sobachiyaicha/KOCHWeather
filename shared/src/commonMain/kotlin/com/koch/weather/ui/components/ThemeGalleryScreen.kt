package com.koch.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.koch.weather.data.model.AppThemeMode
import com.koch.weather.data.model.ThemeCatalogItem
import com.koch.weather.ui.theme.KochBorderWidth
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/**
 * R15: полноэкранный «Вид» — каталог тем (refs/congrats.png, 1:24).
 *
 * Отдельной «Рулетки тем» здесь больше НЕТ (item 4 фазы 5): рулетка живёт внутри
 * `LootboxDialog`, поэтому у экрана нет ни карточки-перехода, ни колбэков
 * `onOpenRoulette`/`onBack` — навигация только нижним баром.
 *
 * Превью карточки — локальный кадр темы (`AppThemeMode.previewKey`, фолбэк —
 * `backgroundKey`), декодированный уменьшенной копией (`ThemePreviewImage`),
 * а не полным кадром 1080×1920: «Вид» показывает узнаваемое фото темы,
 * но без пика памяти ~58 МБ на карточки (M4 код-ревью Фазы 5).
 */
@Composable
fun ThemeGalleryScreen(
    themes: List<ThemeCatalogItem>,
    currentTheme: AppThemeMode,
    background: Brush,
    attribution: String?,
    attributionNote: String? = null,
    onSelectTheme: (AppThemeMode) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GalleryHeader(
                title = "Оформление",
                subtitle = "Темы выпадают в лутбоксах"
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                // Отступ снизу под навбар приходит из Scaffold (`bottomContentPadding`):
                // собственный navigationBarsPadding здесь дал бы двойной инсет.
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = KochSpace.xl,
                    top = 2.dp,
                    end = KochSpace.xl,
                    bottom = KochSpace.x2l
                ),
                horizontalArrangement = Arrangement.spacedBy(KochSpace.m),
                verticalArrangement = Arrangement.spacedBy(KochSpace.m)
            ) {
                items(themes, key = { it.theme.name }) { item ->
                    ThemeCard(
                        item = item,
                        isCurrent = item.theme == currentTheme,
                        onSelect = { onSelectTheme(item.theme) }
                    )
                }

                attribution?.takeIf { it.isNotBlank() }?.let { text ->
                    item(key = "attribution", span = { GridItemSpan(maxLineSpan) }) {
                        DataSourceFooter(text, note = attributionNote)
                    }
                }
            }
        }
    }
}

/**
 * Шапка страницы «Вид»: 28/700 + подпись 12. Кнопки «назад» нет — навигация идёт
 * нижним баром (item 3), поэтому шапка живёт здесь, а не в общем `KochPageHeader`
 * с обязательным `onBack`.
 */
@Composable
private fun GalleryHeader(title: String, subtitle: String?) {
    val colors = LocalKochColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                start = KochSpace.xl,
                end = KochSpace.xl,
                top = KochSpace.m,
                bottom = KochSpace.m
            )
    ) {
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
}

@Composable
private fun ThemeCard(
    item: ThemeCatalogItem,
    isCurrent: Boolean,
    onSelect: () -> Unit
) {
    val colors = LocalKochColors.current
    val shape = RoundedCornerShape(KochRadius.card)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceGlass)
            .border(
                width = if (isCurrent) KochBorderWidth.focus else KochBorderWidth.hairline,
                color = if (isCurrent) colors.accent else colors.border,
                shape = shape
            )
            .clickable(enabled = item.isUnlocked, role = Role.Button, onClick = onSelect)
    ) {
        Column {
            // Превью: локальный (уже размытый) фон темы + мини-карточка и «Aa».
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .clip(RoundedCornerShape(topStart = KochRadius.card, topEnd = KochRadius.card))
            ) {
                ThemePreviewImage(
                    theme = item.theme,
                    modifier = Modifier.fillMaxSize()
                )
                // Затемнение под белый текст поверх светлых фото.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0x33000000), Color(0x77000000))))
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(KochSpace.s),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.55f))
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "Aa",
                            style = KochType.titleSheet,
                            color = Color.White
                        )
                        Spacer(Modifier.width(KochSpace.s))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.22f))
                        )
                    }
                }
                if (!item.isUnlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x66000000))
                    )
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Тема закрыта",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.align(Alignment.Center).size(22.dp)
                    )
                }
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(KochSpace.s)
                            .size(22.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(colors.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Текущая тема",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = KochSpace.m, vertical = KochSpace.s)) {
                Text(
                    text = item.titleRu,
                    style = KochType.bodyStrong,
                    color = colors.textPrimary,
                    // Две строки: «Неоновый киберпанк» не обязан влезать в одну.
                    maxLines = 2
                )
                Spacer(Modifier.height(2.dp))
                // Бейдж статуса: светлая пилюля + тёмный текст (#2E7D32 / #8A5A00) — контраст ≥4.5:1
                // на любом превью, в отличие от светло-зелёного текста прямо на карточке.
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color(0xE6FFFFFF))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (item.isUnlocked) Icons.Default.Check else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (item.isUnlocked) Color(0xFF2E7D32) else Color(0xFF8A5A00),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = if (item.isUnlocked) "Доступно" else "В лутбоксах",
                        style = KochType.caption,
                        color = if (item.isUnlocked) Color(0xFF2E7D32) else Color(0xFF8A5A00),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
