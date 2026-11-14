# ui-coder — точечный перф-проход по hotspots (perf-qa-final)

**Дата:** 14.11.2026. **Сборка:** `compileDebugKotlin` → **BUILD SUCCESSFUL in 7s**, 0 warnings
(`.local/build_perf_final.txt`). Карта, «Еда», палитра, шрифт — не тронуты.

## 1. `MainCards.kt` — убран `IntrinsicSize.Min` (двойной measure на каждом кадре)

**Было** (строки 228/252):
```kotlin
Row(modifier = Modifier.height(IntrinsicSize.Min), …) {
    ServiceCard(…, modifier = Modifier.weight(1f).fillMaxHeight())
```
Intrinsic-проход заставлял измерять карточки дважды на каждом кадре скролла, и это
единственные два intrinsic-места в ленте.

**Стало:**
```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(KochSpace.m)) {
    ServiceCard(…, modifier = Modifier.weight(1f).heightIn(min = 132.dp))
```
Один measure-проход; карточки выровнены по минимальной высоте 132 dp (без intrinsic).
Импорт `IntrinsicSize` и `fillMaxHeight` из грида убраны.

## 2. `MainWeatherCards.kt` — 10 дней без лишних элементов

**Было:** на каждый кадр `dailyList.take(10)` + отдельные `HorizontalDivider` (10 composable-
элементов) + `clip(RoundedCornerShape)` на каждой строке.

**Стало:**
- строки готовятся один раз: `val rows = remember(dailyList) { dailyList.take(10) }`;
- разделитель рисуется в `drawBehind` самой строки (`0.5 dp`, цвет захвачен до лямбды) —
  на 10 элементов меньше в композиции;
- у строки убран per-row `clip(...)` (лишняя аллокация shape на кадр);
- зонтики по-прежнему после «Пн»; секции вне вьюпорта не композятся — их отсекает внешний
  `LazyColumn` на уровне item (карточка дней — один item).

## 3. `MainScreenUiState` — почему не разрезал (честно)

Проверил VM: **секундного тикера нет** — `nextRetryAt` пишется один раз при планировании
повтора и один раз при срабатывании; `isRetrying` меняется только на границах попытки. То есть
флаги не «дёргают» ленту каждый кадр.

Разрезание состояния на «минимальные параметры» без `@Immutable` на моделях
(`WeatherDomainData`, `PremiumOffer`, `MainScreenUiState` живут в `data/model` — зона
data-coder, менять их в этом окне запрещено) не даёт skip: Compose считает такие параметры
нестабильными и всё равно перекомпоновывает. Поэтому пункт сознательно не делал — эффект был бы
нулевым при риске регрессии. Если понадобится, это отдельная задача для data-coder:
`@Immutable` на моделях + `remember`-лямбды в `MainScreen`.

## 4. `KochSheet.kt` — первый кадр листа

**Было:** первый кадр листа включал композицию всего контента (такси — 12 кадров, 150–200 мс).

**Стало:**
```kotlin
var contentReady by remember { mutableStateOf(false) }
LaunchedEffect(Unit) { withFrameNanos { }; contentReady = true }
…
if (contentReady) { Box(weight…) { список/контент + фейд } }
```
Каркас (скрим, поверхность, граббер, hero, CTA) рисуется сразу, контент — следующим кадром.
Инсеты, высота и позиция CTA не менялись. Работает для всех листов, не только такси.

## 5. Смена темы — фон не мигает и не пересоздаёт Canvas

`KochBackground.kt`:
- статус загрузки теперь **по URL**: `val loadedByUrl = remember { mutableStateMapOf<String, Boolean>() }`,
  `photoLoaded = loadedByUrl[imageUrl] == true`. При переключении темы уже загруженное фото
  показывается без сброса в `null`, Canvas облаков не пересоздаётся для загруженных URL;
- `MainScreen.kt`: предзагрузка всех трёх небесных URL при старте
  (`loader.enqueue(ImageRequest…)` в `LaunchedEffect`), поэтому первое переключение темы берёт
  фото из памяти Coil.

## Что может измениться визуально (проверить на кадрах)
1. Сервисные карточки: высота = `max(132 dp, контент)` — в первой паре «Путешествия/Лутбоксы»
   карточки могут быть на пару dp разной высоты (раньше intrinsic выравнивал).
2. Листы: контент появляется через один кадр после каркаса — на 60 Гц это ~16 мс, но на слабом
   устройстве возможен заметный «допрыг» высоты листа; если критично — вернуть `contentReady=true`
   для конкретного листа.
3. Тема: при первом переключении на не закешированное фото виден фолбэк-градиент с облаками
   (без пустого экрана), после загрузки — фото.
