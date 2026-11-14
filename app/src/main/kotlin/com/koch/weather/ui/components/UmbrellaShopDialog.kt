package com.koch.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun UmbrellaShopDialog(
    onDismiss: () -> Unit
) {
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var isPurchased by remember { mutableStateOf(false) }

    val colors = listOf(
        "Классический чернейший" to Color(0xFF212121),
        "Небесно-голубой" to Color(0xFF42A5F5),
        "Золотой VIP" to Color(0xFFFFD54F)
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF1E3A5F),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header with close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BeachAccess,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Зонты KOCH Protect",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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

                // Card preview
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x33FFFFFF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BeachAccess,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Автоматический антиштормовой зонт",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "12 карбоновых спиц, влагоотталкивающее покрытие Teflon",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Выберите расцветку:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colors.forEachIndexed { index, (name, color) ->
                        val isSelected = index == selectedColorIndex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color(0x44FFFFFF),
                                    shape = CircleShape
                                )
                                .clickable { selectedColorIndex = index }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Price and action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Специальная цена",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "29.99 BYN",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E676)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "649 ₽",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Button(
                        onClick = { isPurchased = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPurchased) Color(0xFF4CAF50) else Color(0xFF29B6F6)
                        )
                    ) {
                        Text(
                            text = if (isPurchased) "Оформлено!" else "Купить",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isPurchased) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Курьер уже бежит к вам сквозь дождь!",
                        color = Color(0xFFB9F6CA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
