package com.koch.weather.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.koch.weather.data.model.LootReward
import com.koch.weather.data.model.LootboxState

@Composable
fun LootboxDialog(
    lootboxState: LootboxState,
    onOpenLootbox: () -> Unit,
    onDismiss: () -> Unit
) {
    // Rotation animation for spinning box
    val infiniteTransition = rememberInfiniteTransition(label = "loot_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "box_shake"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "box_pulse"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1035),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFAB47BC)),
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
                            color = Color(0xFFAB47BC).copy(alpha = 0.25f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    tint = Color(0xFFE1BEE7),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Погодные Лутбоксы",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Не повезло с погодой? Повезет тут!",
                                fontSize = 11.5.sp,
                                color = Color(0xFFCE93D8)
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

                // Interactive Chest / Box Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF381460), Color(0xFF1D0B33))
                            )
                        )
                        .border(1.dp, Color(0x40BA68C8), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val boxModifier = if (lootboxState.isOpening) {
                            Modifier
                                .scale(scale * 1.15f)
                                .rotate(rotation * 2f)
                        } else {
                            Modifier.scale(scale)
                        }

                        Icon(
                            imageVector = if (lootboxState.isOpening) Icons.Default.Casino else Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = boxModifier.size(if (lootboxState.isOpening) 54.dp else 62.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (lootboxState.isOpening) "Открываем ящик удачи..." else "Сундук Утешения #2025",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Шанс на iPhone 16 Pro Max: 0.001%",
                            color = Color(0xFFFFD54F),
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Last Reward Announcement Card
                val reward = lootboxState.lastReward
                if (reward != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(reward.rarity.colorHex).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(reward.rarity.colorHex)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = Color(reward.rarity.colorHex),
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(36.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(reward.rarity.colorHex)
                                    ) {
                                        Text(
                                            text = reward.rarity.titleRu.uppercase(),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = reward.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = reward.subtitle,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp
                                )
                                if (reward.weatherForecastTomorrow != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Прогноз: ${reward.weatherForecastTomorrow}",
                                        color = Color(0xFFFFE082),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // History of opened lootboxes
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item(key = "history_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "История дропов:",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Открыто: ${lootboxState.totalOpened}",
                                color = Color(0xFFCE93D8),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (lootboxState.history.isEmpty()) {
                        item(key = "history_empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Вы еще не открывали сундуки сегодня.\nЖмите кнопку ниже!",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(lootboxState.history.take(10), key = { it.id }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x15FFFFFF), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                                    Text(item.subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 10.5.sp)
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(item.rarity.colorHex).copy(alpha = 0.3f)
                                ) {
                                    Text(
                                        text = item.rarity.titleRu,
                                        color = Color(item.rarity.colorHex),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Spin Action Button
                Button(
                    onClick = onOpenLootbox,
                    enabled = !lootboxState.isOpening,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFAB47BC),
                        contentColor = Color.White
                    )
                ) {
                    if (lootboxState.isOpening) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Крутим рулетку...", fontSize = 14.sp)
                    } else {
                        Text(
                            text = "Открыть лутбокс БЕСПЛАТНО",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
