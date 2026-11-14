package com.koch.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun PaywallDialog(
    isPremiumPurchased: Boolean,
    onPurchase: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF0F1A2C),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF42A5F5)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top close icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                // Crown / Star Icon with glowing background
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1976D2), Color(0xFF64B5F6))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Погода ПРЕМИУМ",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Text(
                    text = "Больше советов и эксклюзивных функций",
                    fontSize = 12.5.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Feature items
                val features = listOf(
                    "Полное отключение всех рекламных баннеров",
                    "Разблокировка всех тем оформления (Лес, Марс)",
                    "Мгновенный разгон облаков пальцем",
                    "Иммунитет к блокировке «Пора на улицу»"
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    features.forEach { feature ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E88E5).copy(alpha = 0.25f),
                                modifier = Modifier.size(22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF64B5F6),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = feature,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Price pill. The title, the discount line and both prices get the full width
                // of the pill, so nothing is truncated (device audit_07).
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x25FFFFFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Тариф «Годовой VIP»",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Скидка 80% только сегодня",
                            color = Color(0xFFFFD54F),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "29.99 BYN",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                softWrap = false,
                                textDecoration = TextDecoration.LineThrough,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                            Text(
                                text = "5.99 BYN",
                                color = Color(0xFF64B5F6),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (isPremiumPurchased) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x304CAF50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "★ ПРЕМИУМ УЖЕ АКТИВЕН!",
                                color = Color(0xFFA5D6A7),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = onReset) {
                        Text("Сбросить подписку (для теста рекламы)", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = onPurchase,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Купить подписку Премиум ›",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
