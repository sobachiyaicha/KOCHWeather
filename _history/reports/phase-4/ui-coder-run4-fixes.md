# ui-coder — P0-фиксы run 4 (табы, карта, «Повторить»)

**Дата:** 14.11.2026. **Сборка:** `compileDebugKotlin --rerun-tasks` → **BUILD SUCCESSFUL in 38s**,
0 warnings (`.local/build_run5_verify.txt`, обычный прогон — `.local/build_run5.txt`).

## 1. Нижний бар не нажимался в состоянии «нет данных»

**Причина.** `CloudVeilOverlay` компоновался в корневом `Box` ПОСЛЕ `Scaffold`, то есть поверх
нижнего бара. Внутри вуали — прозрачный `Box(fillMaxSize).clickable {}`, который гасит тапы.
Пока вуаль видима (а в «нет данных» её никто не стирает), она накрывала и таб-бар: Карта/Вид/
Список/Поиск не реагировали. Это же объясняет «в первые ~1.5 с работает» — до появления вуали.

**Фикс.** `MainScreen.kt`, блок вуали:
```kotlin
val barsHeight = with(density) {
    (WindowInsets.navigationBars.getBottom(density) + 88.dp.roundToPx()).toDp()
}
CloudVeilOverlay(
    onCleared = { onCloudVeilDismissed(); onCloudsCleared() },
    modifier = Modifier.fillMaxSize().padding(bottom = barsHeight)
)
```
Полоса бара (64 dp + 2×12 dp отступа + системный инсет) больше не входит в оверлей — табы
кликабельны всегда, включая состояние «нет данных» и облачную вуаль. Других полноэкранных
перехватчиков в этом состоянии нет: `KochSkyBackground` без ввода, pager слушает только drag.

## 2. На карте не было маркера без погоды

**Причина.** `MapView` создаётся в `AndroidView.factory` уже после первой композиции, а
`LaunchedEffect(mapReady, lat, lon, cityName, currentTemp)` кеировался без `mapView`: первый
проход видел `mapView == null` и выходил, повторного запуска не было.

**Фикс.** `WeatherMapScreen.kt`:
- в ключи `LaunchedEffect` добавлен `mapView` → маркер ставится всегда, по координатам
  `CityLocation`, независимо от наличия погоды;
- подпись капсулы: `"Карта · OpenStreetMap · " + (currentTemp?.let { "$it°" } ?: "Нет данных")` —
  вместо пропуска температуры;
- `snippet` маркера: `"Нет данных"` вместо «Погода».

## 3. «Повторить» не перезапускало загрузку

**Причина.** В `MainScreenViewModel.fetchWeatherQuietly` флаг `isRefreshing` выставлялся в `true`
и **никогда не сбрасывался**. Кнопка `KochPrimaryButton(enabled = !isRefreshing, loading = …)`
после первой попытки навсегда оставалась «Обновляем…» и disabled — визуально «кнопка мертва».

**Фикс.** `MainScreenViewModel.kt` (файл data-coder — правка согласована с ним, помечена в коде):
```kotlin
WeatherUiReducer.apply(state, result, isConnected).copy(isRefreshing = false)
```
Ретрай снова активен после каждого завершения (успех/ошибка/офлайн), остальной UI не блокируется.

## Что проверить на ретесте

1. Без сети: экран «Нет данных о погоде» → тапы по всем 5 табам открывают соответствующие экраны.
2. Та же проверка при поднятой облачной вуали — бар нажимается, облака стираются в верхней зоне.
3. Карта без погоды: маркер по городу есть, подпись «Карта · OpenStreetMap · Нет данных».
4. «Повторить»: спиннер на время попытки, затем снова «Повторить» (не «Обновляем…» навсегда).
