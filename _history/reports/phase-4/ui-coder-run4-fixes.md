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

## 4. Маркер на карте — настоящая причина (run 4b)

**Симптом:** тайлы, капсула и кнопка центра есть, пина нет ни при наличии погоды, ни без неё.

**Причина (подтверждено исходником osmdroid 6.1.x).** Иконка маркера создавалась как
`GradientDrawable`. У такого drawable `getIntrinsicWidth()/getIntrinsicHeight()` равны `-1`,
пока не вызван `setSize()` (мой `setBounds(0,0,42,42)` на intrinsic не влияет). А `Marker.drawAt`
считает прямоугольник именно от intrinsic:

```java
// Marker.java:557-565
final int markerWidth = mIcon.getIntrinsicWidth();   // -1
final int markerHeight = mIcon.getIntrinsicHeight(); // -1
final int offsetX = pX - Math.round(markerWidth * mAnchorU);
mRect.set(offsetX, offsetY, offsetX + markerWidth, offsetY + markerHeight); // вырожденный
RectL.getBounds(mRect, pX, pY, pOrientation, mOrientedMarkerRect);
mDisplayed = Rect.intersects(mOrientedMarkerRect, pCanvas.getClipBounds());
if (!mDisplayed) return;   // маркер не рисуется вообще
```
То есть overlay в списке был, но `drawAt` выходил на проверке видимости — «пина нет, карта живая».

**Вторая проблема (устранена заодно).** Маркер добавлялся из `LaunchedEffect`, а `MapView`
создаётся в `AndroidView.factory` уже после первой композиции: первый проход эффекта видел
`mapView == null` и выходил, повторный запуск зависел от перезаписи state во время композиции.

**Фикс (`WeatherMapScreen.kt`):**
- иконка — `BitmapDrawable` из `Canvas`: белое кольцо + точка `accent`, размер
  `24dp * density` px, `setBounds(0,0,size,size)`; intrinsic size = размеру битмапа;
- маркер создаётся и добавляется **внутри `AndroidView.factory`** (`view.overlays.add(marker)`
  + `view.invalidate()`), поэтому не зависит от порядка «состояние → эффект»;
- `MapStateHolder` (`view`, `marker`) — обычный класс без snapshot-state; эффект смены
  города/погоды двигает уже существующий маркер (`position/title/snippet`), а если его нет —
  создаёт заново и вызывает `invalidate()`;
- `setAnchor(ANCHOR_CENTER, ANCHOR_CENTER)`, `position = GeoPoint(city)`;
- подпись без погоды — «Нет данных» (капсула и snippet маркера).

**Проверка офлайн:** маркер рисуется независимо от тайлов и сети — координаты берутся из
`CityLocation`, поэтому его видно и в состоянии «Нет данных».

