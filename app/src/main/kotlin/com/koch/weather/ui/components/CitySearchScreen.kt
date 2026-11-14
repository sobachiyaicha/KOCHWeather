package com.koch.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.koch.weather.data.model.CityLocation
import com.koch.weather.ui.design.KochCard
import com.koch.weather.ui.design.KochEmptyState
import com.koch.weather.ui.design.KochListRow
import com.koch.weather.ui.design.KochPageHeader
import com.koch.weather.ui.theme.KochSpace
import com.koch.weather.ui.design.KochTextButton
import com.koch.weather.ui.theme.KochRadius
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/**
 * R14: полноэкранный поиск города. Поле без рамки в покое, результаты — одна группа строк,
 * координаты (отладочная информация) убраны.
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
    val colors = LocalKochColors.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            KochPageHeader(
                title = "Поиск города",
                subtitle = "Название, регион или страна",
                onBack = onBack
            )

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KochSpace.xl, vertical = KochSpace.s)
                    .heightIn(min = 48.dp)
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        text = "Введите название города",
                        style = KochType.body,
                        color = colors.textPrimary.copy(alpha = 0.5f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.textPrimary.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Очистить",
                            tint = colors.textPrimary.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onQueryChange("") }
                        )
                    }
                },
                textStyle = KochType.body,
                singleLine = true,
                shape = RoundedCornerShape(KochRadius.input),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedContainerColor = colors.textPrimary.copy(alpha = 0.14f),
                    unfocusedContainerColor = colors.textPrimary.copy(alpha = 0.12f),
                    focusedBorderColor = colors.accent.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = colors.accent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() })
            )

            Spacer(Modifier.height(KochSpace.s))

            when {
                isSearching -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = KochSpace.x2l),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = colors.textPrimary.copy(alpha = 0.8f),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(KochSpace.s))
                        Text(
                            text = "Ищем города...",
                            style = KochType.sub,
                            color = colors.textPrimary.copy(alpha = 0.8f)
                        )
                    }
                }

                errorMessage != null -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = KochSpace.xl)
                            .clip(RoundedCornerShape(KochRadius.panel))
                            .background(colors.accentDanger.copy(alpha = 0.16f))
                            .padding(KochSpace.l)
                    ) {
                        Column {
                            Text(
                                text = "Не удалось выполнить поиск",
                                style = KochType.bodyStrong,
                                color = colors.textPrimary
                            )
                            Text(
                                text = errorMessage,
                                style = KochType.sub,
                                color = colors.textPrimary.copy(alpha = 0.85f)
                            )
                            KochTextButton(
                                text = "Повторить",
                                onClick = { onQueryChange(query) },
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }

                query.isBlank() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        KochEmptyState(
                            icon = Icons.Default.Search,
                            title = "Начните вводить название",
                            subtitle = "Поиск идёт по всему миру и не требует ключей."
                        )
                    }
                }

                results.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        KochEmptyState(
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
                            start = KochSpace.xl,
                            end = KochSpace.xl,
                            bottom = KochSpace.x2l
                        )
                    ) {
                        item(key = "results_group") {
                            KochCard(padding = PaddingValues(vertical = KochSpace.xs)) {
                                results.forEachIndexed { index, city ->
                                    KochListRow(
                                        title = city.name,
                                        subtitle = city.locationLine(),
                                        showDivider = index != results.lastIndex,
                                        onClick = { onSelectCity(city) },
                                        leading = {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = colors.accent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailing = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = "Выбрать город",
                                                tint = colors.textTertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
