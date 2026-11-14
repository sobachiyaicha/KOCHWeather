package com.koch.weather.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koch.weather.data.model.CityLocation
import java.util.Locale

/**
 * R14: полноэкранный «Поиск» города.
 *
 * Живой geocoding через MainScreenViewModel.searchCities; ошибка из uiState.searchError
 * показывается явно, без выдуманных результатов.
 */
@Composable
fun CitySearchScreen(
    query: String,
    results: List<CityLocation>,
    isSearching: Boolean,
    errorMessage: String?,
    background: Brush,
    onQueryChange: (String) -> Unit,
    onSelectCity: (CityLocation) -> Unit,
    onBack: () -> Unit
) {
    FullScreenScaffold(
        title = "Поиск города",
        subtitle = "Название, регион или страна",
        background = background,
        onBack = onBack
    ) {
        val keyboard = LocalSoftwareKeyboardController.current
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .focusRequester(focusRequester),
            placeholder = {
                Text(
                    text = "Введите название города",
                    color = Color.White.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Очистить",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF64B5F6),
                unfocusedBorderColor = Color(0x66FFFFFF),
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() })
        )

        when {
            isSearching -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Ищем города...",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            errorMessage != null -> {
                SearchErrorCard(
                    message = errorMessage,
                    onRetry = { onQueryChange(query) }
                )
            }

            query.isBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    FullScreenEmptyState(
                        icon = Icons.Default.Search,
                        title = "Начните вводить название",
                        subtitle = "Поиск идёт по всему миру и не требует ключей."
                    )
                }
            }

            results.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    FullScreenEmptyState(
                        icon = Icons.Default.LocationOn,
                        title = "Города не найдены",
                        subtitle = "Проверьте написание или попробуйте другой запрос."
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(
                        start = 14.dp,
                        top = 4.dp,
                        end = 14.dp,
                        bottom = 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = results,
                        key = { "${it.name}_${it.latitude}_${it.longitude}" }
                    ) { city ->
                        SearchResultRow(
                            city = city,
                            onClick = { onSelectCity(city) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    city: CityLocation,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0x26FFFFFF),
        border = BorderStroke(0.8.dp, Color(0x30FFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFF64B5F6),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = city.name,
                    color = Color.White,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = city.country,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 11.5.sp,
                    maxLines = 1
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "%.2f, %.2f",
                        city.latitude,
                        city.longitude
                    ),
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Добавить город",
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SearchErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0x33EF5350),
        border = BorderStroke(1.dp, Color(0x88EF5350)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFFFCDD2),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Не удалось выполнить поиск",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Start
            )
            TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                Text("Повторить", color = Color(0xFF90CAF9), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
