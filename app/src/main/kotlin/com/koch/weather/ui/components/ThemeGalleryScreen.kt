package com.koch.weather.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koch.weather.data.model.AppThemeMode
import com.koch.weather.data.model.ThemeCatalogItem

/**
 * R15: полноэкранный «Вид» — каталог тем из лутбоксов и рулетки.
 *
 * Статус «Доступно»/«Открывается в лутбоксах» приходит только из themeCatalog; UI не
 * разблокирует темы сам. Применение доступной темы — через onSelectTheme -> ViewModel.
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
    FullScreenScaffold(
        title = "Оформление",
        subtitle = "Темы из лутбоксов и рулетки",
        background = background,
        onBack = onBack
    ) {
        OutlinedButton(
            onClick = onOpenRoulette,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.2.dp, Color(0xFFFFD600)),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFFFD600)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Casino,
                contentDescription = null,
                tint = Color(0xFFFFD600),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Рулетка тем",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD600)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = 14.dp,
                top = 2.dp,
                end = 14.dp,
                bottom = 28.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(themes, key = { it.theme.name }) { item ->
                ThemeCatalogCard(
                    item = item,
                    isCurrent = item.theme == currentTheme,
                    onSelect = { onSelectTheme(item.theme) }
                )
            }

            attribution?.takeIf { it.isNotBlank() }?.let { text ->
                item(
                    key = "attribution",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    DataSourceFooter(text, note = attributionNote)
                }
            }
        }
    }
}

@Composable
private fun ThemeCatalogCard(
    item: ThemeCatalogItem,
    isCurrent: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0x26FFFFFF),
        border = BorderStroke(
            width = if (isCurrent) 1.6.dp else 0.8.dp,
            color = if (isCurrent) Color(0xFFFFD54F) else Color(0x30FFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.isUnlocked, onClick = onSelect)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(themePreviewBrush(item.theme))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .width(54.dp)
                            .height(9.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0x66FFFFFF))
                    )
                    Text(
                        text = "Aa",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                if (!item.isUnlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x88000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Тема закрыта",
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (isCurrent) {
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = Color(0xFFFFD54F),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "Текущая",
                            color = Color(0xFF1E232A),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = item.titleRu,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item.isUnlocked) Icons.Default.Check else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (item.isUnlocked) Color(0xFFA5D6A7) else Color(0xFFFFB74D),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (item.isUnlocked) "Доступно" else "Открывается в лутбоксах",
                        color = if (item.isUnlocked) Color(0xFFA5D6A7) else Color(0xFFFFB74D),
                        fontSize = 9.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
