package com.koch.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GrassLockDialog(
    onGrassUploaded: () -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { /* Blocking dialog */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF131D15),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF4CAF50)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 22.dp, start = 22.dp, end = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Plant / Eco icon
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF2E7D32).copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Пора на улицу!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Вы слишком долго смотрели погоду. Дальнейший просмотр будет доступен только после загрузки фотографии травы.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Upload container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x15FFFFFF))
                        .border(
                            width = 1.5.dp,
                            color = Color(0xFF81C784),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable(enabled = !isUploading) {
                            isUploading = true
                            uploadStatus = "Открываем камеру..."
                            coroutineScope.launch {
                                delay(1000)
                                uploadStatus = "ИИ проверяет подлинность травы..."
                                delay(1200)
                                uploadStatus = "Трава подтверждена!"
                                delay(600)
                                onGrassUploaded()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF81C784),
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = uploadStatus ?: "Загрузка...",
                                color = Color(0xFFA5D6A7),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.Eco, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(34.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Нажмите, чтобы сделать фото",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Подойдет любая зелень во дворе",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        isUploading = true
                        uploadStatus = "Открываем камеру..."
                        coroutineScope.launch {
                            delay(1000)
                            uploadStatus = "ИИ проверяет подлинность травы..."
                            delay(1200)
                            uploadStatus = "Трава подтверждена!"
                            delay(600)
                            onGrassUploaded()
                        }
                    },
                    enabled = !isUploading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E88E5),
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Открыть камеру", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
