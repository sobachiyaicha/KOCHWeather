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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.koch.weather.ui.design.KochCard
import com.koch.weather.ui.design.KochPageHeader
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.theme.KochBorderWidth
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/**
 * R15: полноэкранный «Вид» — каталог тем (refs/congrats.png, 1:24).
 * Превью показывает собственный градиент темы, а не одинаковую светлую подложку.
 */
@Composable
fun ThemeGalleryScreen(
    themes: List<ThemeCatalogItem>,
    currentTheme: AppThemeMode,
    background: Brush,
    attribution: String?,
    attributionNote: String? = null,
    onSelectTheme: (AppThemeMode) -> Unit,
    onOpenRoulette: () -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalKochColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            KochPageHeader(
                title = "Оформление",
                subtitle = "Темы из лутбоксов и рулетки",
                onBack = onBack
            )

            // Рулетка — карточка-переход, а не жёлтая outlined-кнопка.
            KochCard(
                modifier = Modifier.padding(horizontal = KochSpace.xl),
                padding = PaddingValues(KochSpace.m),
                onClick = onOpenRoulette
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(colors.accentPromo.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = null,
                            tint = colors.accentPromo,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(KochSpace.m))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Рулетка тем",
                            style = KochType.bodyStrong,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Прокрутите и выберите оформление",
                            style = KochType.caption,
                            color = colors.textTertiary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(KochSpace.m))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .navigationBarsPadding(),
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
            // Превью: собственный градиент темы + мини-карточка и «Aa».
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .clip(RoundedCornerShape(topStart = KochRadius.card, topEnd = KochRadius.card))
                    .background(themePreviewBrush(item.theme))
            ) {
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
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(percent = 50))
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
