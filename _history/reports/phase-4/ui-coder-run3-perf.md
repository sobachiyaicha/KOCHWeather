# ui-coder — правки run 3 + перф-проход

**Дата:** 14.11.2026. **Контекст:** дефекты прогона 3 (visual-critic/device-qa) и отчёт
`_history/reports/phase-4/perf-qa.md` (блокер R18).
**Сборка:** `.\gradlew.bat compileDebugKotlin` → **BUILD SUCCESSFUL in 10s**, 0 warnings
(`.local/build_perf.txt`). `data/**` не менялся.

---

## A. Функциональные дефекты run 3

### 1. Облака возвращались после смены таба/темы и после перезапуска

**Было:** `MainScreen.kt` показывал вуаль по `isCloudLocked && !isCloudsCleared`; латч
«уже стёрто» хранился внутри `MainScreenContent`, который выходит из композиции при уходе с
главной → состояние сбрасывалось, и вуаль показывалась снова.

**Стало:**
- `MainScreen.kt:82-86` — `var cloudVeilDismissed by rememberSaveable { mutableStateOf(false) }`
  на уровне `MainScreen` (переживает смену таба и конфигурации);
- `MainScreen.kt:99-104` — латч и колбэк передаются в `MainScreenContent(cloudVeilDismissed,
  onCloudVeilDismissed)`;
- `MainScreen.kt:667-679` — `val veilVisible = isCloudLocked && !isCloudsCleared && !cloudVeilDismissed`;
  `onCleared` ставит латч И вызывает `onCloudsCleared()` (персистентная запись — из VM).
- Ожидаемое поле VM для «навсегда»: `isCloudsCleared` (уже есть) + `isCloudVeilVisible`
  (data-coder). UI уже корректно ведёт себя, если `isCloudsCleared=true` приходит при старте.

### 2. Свайп стирания облаков листал карусель городов

**Было:** `MainScreen.kt` вешал `detectHorizontalDragGestures` на корневой Box безусловно;
жест стирания доходил до pager.

**Стало:**
- `MainScreen.kt:389-398` — пока вуаль видима, `pointerInput` возвращается сразу
  (`return@pointerInput`), карусель не слушает вообще; ключи `pointerInput` включают
  `isCloudLocked/isCloudsCleared/cloudVeilDismissed`;
- `CloudVeilOverlay.kt:106` — `change.consume()` в `onDrag`;
- `CloudVeilOverlay.kt:89-103` — drag-детектор живёт только в облачной зоне (45 % высоты),
  ниже — прозрачный слой, который гасит тапы, но не слушает drag.

### 3. Обрезки текста

| Где | Было | Стало |
|---|---|---|
| «Вид», название темы | `maxLines=1, softWrap=false` → «Неоновый киберпан» | `maxLines=2` (`ThemeGalleryScreen.kt`) |
| «Вид», статус | «Открывается в лутбоксах», обрезалось | «В лутбоксах» + замок (`ThemeGalleryScreen.kt`) |
| Рулетка, чипы | `LazyRow` резал «Изумрудный лес» краем экрана | `FlowRow` — чипы переносятся, ширина по контенту (`ThemeRouletteDialog.kt`) |
| Рулетка, карточки тем | 1 строка | 2 строки, высота 62 dp |
| Еда, чипы ресторанов | `LazyRow` резал «Surf Coffee» | `FlowRow` (`FoodBottomSheet.kt`) |
| Сервисы 2×2 | заголовок 2 строки / описание 3 → «Повезет…» и «…приобрести» | заголовок 3 / описание 4 (`MainCards.kt`) |

### 4. Пейволл: противоречие «Годовой VIP» ↔ «в месяц»

**Было:** в шаге подтверждения — «5.99 BYN в месяц · списание один раз», в витрине —
«Тариф «Годовой VIP»»; плюс UI-константы, дублирующие цены.
**Стало:** все строки из одного state `PremiumOffer` (data-coder):
`"${offer.priceLabel} ${offer.periodLabel} · ${offer.billingNoteRu}"` → «5.99 BYN за год ·
списание один раз»; витрина показывает `offer.tariffTitleRu`, `offer.discountLabel`,
`offer.oldPriceLabel` (зачёркнуто) и `offer.priceLabel`. Слов «в месяц» в UI больше нет
(`PaywallDialog.kt`).

### 5. Медная тема

- Порядок блоков уже тема-зависимый (`CleanFeedOrder` для BLUE_SKY, `PromoFeedOrder` для
  остальных) — соответствует theme_copper/f_063 и premium_ads 1:57.
- Карусель больше не уезжает сама: причина была в п.2 (жест), `setTheme` city не меняет.
- Шапка: верхний паддинг контейнера 24 → **40 dp** (`MainHeader.kt`) — температура больше не
  подрезается верхней кромкой экрана (было видно на s3_16: «26°» срезано).
- Контент не уходит под навбар: вьюпорт списка укорачивается на высоту бара
  (`MainScreen.kt:429-437`), а не только `contentPadding`.

---

## B. Производительность (perf-qa)

### 1. Палитра и CompositionLocal

**Было:** `Theme.kt:132-190` `derived()` создавал новый `KochColors` на каждый вызов;
`LocalKochColors` — `staticCompositionLocalOf`; любое обновление uiState меняло экземпляр →
перекомпоновка всего дерева на 5 темах из 7.

**Стало:** `Theme.kt`
- `@Immutable data class KochColors` и `@Immutable data class CustomThemeColors`;
- `private val KochColorsCache = ConcurrentHashMap<AppThemeMode, KochColors>()`,
  `fun kochColors(mode) = cache.getOrPut(mode) { ... }` — одна тема → один экземпляр;
- `getThemePalette(mode)` тоже кешируется (`LegacyPaletteCache`), поэтому `Brush` в страницах
  больше не пересоздаётся.

### 2. Лента: лямбды вместо стабильных ключей

**Было:** `MainScreen.kt:534-608` — шесть `@Composable`-лямбд + `listOf(... to ...)` на каждый
проход, список и лямбды пересоздавались.

**Стало:** `MainScreen.kt:64-71` — `private val CleanFeedOrder` и `PromoFeedOrder`
(стабильные списки), `MainScreen.kt:534-577` — `feedOrder.forEach { key -> item(key) { when(key) } }`.
Аллокаций лямбд/списков на рекомпозицию нет.

### 3. Облака

**Было:** полноэкранный offscreen-слой 1080×1920 (GPU Scratch 7.97 МБ), до 220 `drawCircle`
с `BlendMode.Clear` за кадр, `mutableStateListOf` на каждый штамп, `wipeProgress` как state.

**Стало:** `CloudVeilOverlay.kt`
- offscreen-слой только на облачную зону (45 % высоты) → ~3.6 МБ вместо 7.97 МБ;
- стирание — **один `drawPath`** с `BlendMode.Clear` по `Path` из овалов (семплирование каждой
  2-й метки), вместо 220 drawCircle;
- метки — обычный `ArrayList` + `mutableIntStateOf` счётчик перерисовки (снимок состояния не
  аллоцируется);
- `wipeProgress` — обычная переменная (в UI не читается), кап меток 96.

### 4. Карта и главная

**Было:** `MainScreen` компоновал ленту всегда, а карту рисовал поверх → `MapView` с
`Configuration.load` жил одновременно с главной.

**Стало:** `MainScreen.kt:97-104` — `if (uiState.selectedTab == BottomTab.HOME) { MainScreenContent(...) }`;
карта/список/поиск/вид компонируются только когда выбраны. На главной `MapView` больше не
создаётся.

### 5. Фото

**Было:** `WeatherImage.kt` — `SubcomposeAsyncImage` всегда + `crossfade(true)`.

**Стало:**
- быстрый путь — `AsyncImage` с `ColorPainter`-плейсхолдером (без сабкомпозиции);
- `SubcomposeAsyncImage` остался только там, где нужен составной фолбэк (мокап телефона в
  рулетке) — параметр `fallback`;
- `crossfade(false)` в `ImageLoader`.

### 6. Холодный старт

Кеш палитр и отказ от лямбд убирают лишние аллокации в первых композициях; основной вклад
(baseline profile / R8) — зона data-coder.

---

## C. Что осталось (после окна data-coder)

1. **Меню еды → `LazyColumn`**: нужен слот `lazyContent` в `KochSheet` (сейчас `Column +
   verticalScroll`), иначе вложенный lazy-список невалиден. 10 позиций с фото 72 dp.
2. **MapView**: инициализация в `AndroidView(factory)` уже ленива по факту (карта компонуется
   только на своём табе); `Configuration.load` — дисковое IO на main, вынести в IO-подготовку
   вместе с data-coder; `setTilesScaledToDpi` оставил (влияет на резкость тайлов).
3. **`@Immutable` на `MainScreenUiState`** — файл `data/model/WeatherModels.kt`, зона
   data-coder: согласовать, чтобы не конфликтовать.
4. **device-qa**: `assembleDebug` + `benchmark`, холодный старт, `gfxinfo`, свежие скрины
   (облака один раз; свайп не листает город; чипы/статусы целиком; пейволл «за год»;
   медная главная).
