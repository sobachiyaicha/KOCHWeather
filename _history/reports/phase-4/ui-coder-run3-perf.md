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

## C. Правки по visual-critic run 3 (после перф-прохода)

### P0

| № | Было | Стало |
|---|---|---|
| 1 | Вуаль 45 % высоты (0…864 px) накрывала «Нет данных о погоде» и «Повторить» | `CloudVeilOverlay.kt` — `fillMaxHeight(0.24f)` (~154 dp, только шапка), мягкий нижний край сохранён |
| 2 | Тикеты «Северное сияние» (с замком) и «Медный закат» (плоская заливка) | `ThemeRouletteDialog.kt` — фиксированные **«Оформление Лес»** и **«Оформление Планета Марс»** (`AppThemeMode.FOREST/MARS`), диагональная текстура + пунктирная рамка (`drawBehind` + `PathEffect.dashPathEffect`), замка нет; телефон — `ContentScale.Crop`, кадр 118×160 (портрет); «Доступно» с Material `Check` |
| 3 | Обрезки: «Неоновый киберпан», «Открывается в лутб», «Surf Coffee», «Ultima VIP», «Изумрудн», «1» в часовом ряду, «…приобрести», «Повезет» | «Вид» — заголовок 2 строки, статус «В лутбоксах»; рулетка/еда — `FlowRow`; тариф такси 112 dp + 2 строки; часовой элемент `widthIn(min = 36.dp)`; сервисы 3/4 строки; «Повезёт тут!» в карточке и в лутбоксах |
| 4 | После покупки не видно состояния ПРЕМИУМ; «РЕКЛАМА» не попала в кадры | состояние уже показывается (зелёная капсула «★ ПРЕМИУМ» в `PremiumBanner`, `MainCards.kt`); реклама скрывается **только после** покупки (`showAds=false` — требование R9). Для доказательства device-qa: кадр главной ДО покупки (прокрутить до баннера) и ПОСЛЕ |

### P1

| № | Было | Стало |
|---|---|---|
| 5 | Зонтики после «Вс» | `MainWeatherCards.kt` — индекс ищется по подписи `startsWith("Пн")`, а не `1`; fallback 1 |
| 6 | «В среднем сегодня: 3.5 ★» спрятана под «Развернуть» | средняя оценка видна всегда под заголовком; «Развернуть» раскрывает только строку про свою оценку |
| 7 | Вторичный текст 1.8:1 | `textTertiary` 55 % → 70 % (`0xB3FFFFFF`) во всех темах; `textQuaternary` 55 % |
| 8 | CTA «съедала» 4-ю позицию еды / 4-й рейс | нижний паддинг ленивого контента листа 16 dp; спейсер в конце контента «Билетов» |
| 9 | Маркер карты с иконкой руки | `pinDrawable()` — круг-булавка в цвете акцента с белым кольцом |
| 10 | Медный фон — плоская заливка (Δ≈40) | `KochBackground.kt` — тёплый радиальный блик сверху (на тёмных/тёплых темах alpha 0.22) + виньетка снизу; перепад стал фото-подобным. Когда data-coder даст URL неба — подставим фото |
| 11 | Капсула шапки 88.6 % ширины | `MainHeader.kt` — `fillMaxWidth(0.84f)`; температура не тронута (`rotate(-15f)`, `offset(-12,-24)`) |

## D. Перф-остаток (сделан в этом же окне)

1. **Меню еды → `LazyColumn`.** `KochSheet` получил слот `lazyContent: (LazyListScope.() -> Unit)?`
   (`ui/design/KochSheet.kt`): тот же лист, та же высота/инсеты, но контент — `LazyColumn` с
   `contentPadding` вместо `Column + verticalScroll`. `FoodBottomSheet` переведён на него:
   заголовок и чипы — `item`, позиции меню — `items(menuItems)`, фото компонуются только в вьюпорте.
2. **MapView лениво.** `WeatherMapScreen.kt`: `Configuration.load` — в `withContext(Dispatchers.IO)`
   внутри `LaunchedEffect`; `MapView` создаётся в `AndroidView.factory` только после подготовки
   конфига (и только когда экран скомпонован); `mapView` хранится в `mutableStateOf`, центр/маркер
   обновляются в `LaunchedEffect(mapReady, lat, lon, …)`. `setTilesScaledToDpi` удалён.
   Главная под картой не компонуется — `MainScreen.kt:99` (`if (selectedTab == HOME)`).
3. **Облака one-time на новом контракте.** UI использует `uiState.isCloudVeilVisible`
   (`MainScreen.kt:404, 659`) + локальный `cloudVeilDismissed` (`rememberSaveable` на уровне
   `MainScreen`); персист — `isCloudsCleared` из VM. Жест карусели не просыпается, пока вуаль
   видима (`return@pointerInput`), а облачный слой `consume()`-ит drag.

## E. Сборка

`.\gradlew.bat compileDebugKotlin` → **BUILD SUCCESSFUL in 7s**, 0 warnings
(`.local/build_p3.txt`). `data/**` не менялся.

