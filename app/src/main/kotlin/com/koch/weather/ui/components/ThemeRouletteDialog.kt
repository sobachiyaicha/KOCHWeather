package com.koch.weather.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.koch.weather.data.model.AppThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun ThemeRouletteDialog(
    currentTheme: AppThemeMode,
    availableThemes: Set<AppThemeMode>,
    cityName: String = "Санкт-Петербург",
    currentTemp: Int? = null,
    iphoneImageUrl: String? = null,
    onSelectTheme: (AppThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isSpinning by remember { mutableStateOf(false) }
    var previewTheme by remember { mutableStateOf(currentTheme) }
    val allThemes = remember { AppThemeMode.values().toList() }
    val currentIndex = allThemes.indexOf(previewTheme).coerceAtLeast(0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF1B1B26),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF7E57C2)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF7E57C2).copy(alpha = 0.25f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = Color(0xFFB39DDB),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Поздравляем!",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Рулетка тем 2025 — выберите стиль",
                                fontSize = 11.5.sp,
                                color = Color(0xFFD1C4E9)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3D iPhone Presentation Showcase (Matching refs/congrats.png)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF28253E), Color(0xFF151424))
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left arrow (neon yellow triangle refs/congrats.png)
                        Surface(
                            shape = CircleShape,
                            color = Color(0x35000000),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD600)),
                            modifier = Modifier
                                .size(46.dp)
                                .clickable {
                                    val prev = (currentIndex - 1 + allThemes.size) % allThemes.size
                                    previewTheme = allThemes[prev]
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("◀", color = Color(0xFFFFD600), fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        // Central Phone Mockup
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Phone frame
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF0F0F14),
                                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFB39DDB)),
                                modifier = Modifier
                                    .width(135.dp)
                                    .height(185.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                ) {
                                    // R17: real iPhone photo from Wikimedia + theme-colored
                                    // scrim, so both the device and the chosen theme are visible.
                                    RemotePhoto(
                                        url = iphoneImageUrl,
                                        contentDescription = "iPhone 16 Pro Max",
                                        modifier = Modifier.fillMaxSize(),
                                        shape = RoundedCornerShape(14.dp),
                                        placeholderIcon = Icons.Default.Smartphone,
                                        placeholderTint = Color.White.copy(alpha = 0.75f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        themeAccentColor(previewTheme).copy(alpha = 0.5f),
                                                        Color(0x77000000)
                                                    )
                                                )
                                            )
                                    )

                                    // Mini phone screen elements
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Notch / Island
                                        Box(
                                            modifier = Modifier
                                                .width(36.dp)
                                                .height(7.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.Black)
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Mini capsule
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0x55000000)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = cityName,
                                                color = Color.White,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Mini temperature
                                        Text(
                                            text = currentTemp?.let { "$it°" } ?: "—",
                                            color = Color.White,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )

                                        Spacer(modifier = Modifier.weight(1f))

                                        // Mini card
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0x40000000))
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Mini nav
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(14.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(Color(0x80000000))
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = previewTheme.titleRu,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            val isUnlocked = availableThemes.contains(previewTheme)
                            Row(
                                modifier = Modifier.fillMaxWidth(0.92f),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isUnlocked) {
                                    Text(
                                        text = "Доступно",
                                        color = Color(0xFFA5D6A7),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFFA5D6A7),
                                        modifier = Modifier.size(13.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB74D),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Открывается в лутбоксах",
                                        color = Color(0xFFFFB74D),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        // Right arrow (neon yellow triangle refs/congrats.png)
                        Surface(
                            shape = CircleShape,
                            color = Color(0x35000000),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD600)),
                            modifier = Modifier
                                .size(46.dp)
                                .clickable {
                                    val next = (currentIndex + 1) % allThemes.size
                                    previewTheme = allThemes[next]
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("▶", color = Color(0xFFFFD600), fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Theme Selection Row: real horizontal scroll (LazyRow) plus an end
                // padding, so the last chip ("Изумрудный лес") can always be scrolled into view.
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(allThemes, key = { it.name }) { theme ->
                        val isCurrent = theme == currentTheme
                        val isPreview = theme == previewTheme
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPreview) Color(0xFF7E57C2) else Color(0x20FFFFFF),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isCurrent) 1.5.dp else 0.8.dp,
                                color = if (isCurrent) Color(0xFFFFD54F) else Color(0x25FFFFFF)
                            ),
                            modifier = Modifier.clickable { previewTheme = theme }
                        ) {
                            Text(
                                text = theme.titleRu,
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = if (isPreview) FontWeight.Bold else FontWeight.Normal,
                                softWrap = false,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Roulette Spin & Apply
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (!isSpinning) {
                                isSpinning = true
                                coroutineScope.launch {
                                    val spins = 12
                                    for (i in 0 until spins) {
                                        delay(80L + i * 15L)
                                        previewTheme = allThemes[Random.nextInt(allThemes.size)]
                                    }
                                    isSpinning = false
                                    onSelectTheme(previewTheme)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCE93D8)),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFAB47BC))
                    ) {
                        Text(
                            text = if (isSpinning) "Крутим..." else "Рулетка!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }

                    Button(
                        onClick = {
                            onSelectTheme(previewTheme)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7E57C2),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Продолжить ›",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
