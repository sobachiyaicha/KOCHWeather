# ui-coder (часть 1) — навигация, шапка, города, анимация. Фаза 5

Дата: 2026-11-14. Роль: `ui-coder`, часть 1 из 2 (items 3, 7, 5, 6-ui, 1-ui, 9-ui, 4-ui
+ карта и удаление scaffolding-androidTest).
Второй `ui-coder` параллельно менял лутбокс/рекламу/фон/галерею (его файлы я не трогал).

## Итог проверки

```powershell
$env:JAVA_HOME="D:\jdk-17\jdk-17.0.12+7"
cd D:\akcon\weatherAPK-dsh
.\gradlew.bat --console=plain compileDebugKotlin
.\gradlew.bat --console=plain testDebugUnitTest
```

- `compileDebugKotlin` — **BUILD SUCCESSFUL** (единственное предупреждение в дереве —
  deprecation `Icons.Filled.DirectionsWalk` в `LootboxDialog.kt`, файл части 2, не мой).
- `testDebugUnitTest` — **BUILD SUCCESSFUL**, по `app/build/test-results/testDebugUnitTest/*.xml`:
  **33 сьюта, 193 теста, 0 падений, 0 пропусков**.
- Оба прогона сделаны на общем дереве уже с изменениями части 2
  (`AdPromoDialog`/`AdBannerCard`/`LootboxDialog`/`ThemeGalleryScreen`/`KochSkyBackground`,
  удалённый `ThemeRouletteDialog`); сигнатуры совпали, компиляция зелёная.

## Что сделано

### 3. Навбар не пропадает ни на одной вкладке (главное)
- `MainScreen` теперь содержит **единственный** `Scaffold`:
  `containerColor = Color.Transparent`, `contentWindowInsets = WindowInsets(0,0,0,0)`,
  `bottomBar = Box(navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) { KochNavBar(...) }`.
- В `content` — `when (uiState.selectedTab)`:
  `HOME → MainScreenContent`, `MAP → WeatherMapScreen`, `LIST → CityListScreen`,
  `SEARCH → CitySearchScreen`, `THEMES → ThemeGalleryScreen`.
- `MainScreenContent` больше не создаёт `Scaffold` и не рисует бар. Принимает
  `bottomContentPadding: Dp` (из `innerPadding.calculateBottomPadding()`) и кладёт его
  в `LazyColumn` через `.padding(bottom = bottomContentPadding)`.
- Для не-HOME вкладок контент лежит в `Box(Modifier.padding(bottom = bottomContentPadding))`
  поверх фонового `TabBackground(themeColors.gradientBrush)` — лента/списки/галерея не уезжают
  под бар, а полоса под баром не остаётся прозрачной (тот же градиент темы).
- Карта: `WeatherMapScreen` получает `Modifier.padding(bottom = bottomContentPadding)`.
- `BackHandler(enabled = selectedTab != HOME)`: SEARCH → `clearSearch()`, затем `setTab(HOME)`.
  Системный «назад» на карте/списке/поиске/виде больше не закрывает приложение.
- Повторный тап по активной «Главной»: `homeListState.animateScrollToItem(0)`
  (state поднят в `MainScreen`). Тап по активной не-главной вкладке — no-op.
- `listState` вынесен из `MainScreenContent` в `MainScreen`; при уходе на другую вкладку
  он сохраняется и главная по-прежнему открывается сверху (старое поведение run 4b).
- Облачная вуаль: нижний инсет теперь ровно `bottomContentPadding` (высота бара из Scaffold,
  включая `navigationBarsPadding`), а не прежние «nav inset + 88 dp». Вуаль не накрывает бар —
  тапы по табам в состоянии «нет данных» работают; `veilCooldownUntil` и свайп-логика
  (`veilCooldownUntil`, `dragAccumulator`) не менялись.

### 7. «ТЕКУЩЕЕ МЕСТО» только для гео-города
- `GoldenCapsuleHeader` += `isCurrentLocation: Boolean`; подпись:
  `if (isCurrentLocation) "ТЕКУЩЕЕ МЕСТО" else "ВЫБРАННЫЙ ГОРОД"` (`KochType.microCaps`).
- `MainScreenContent` передаёт `uiState.isSelectedCityCurrentLocation`.
- Иконка-навигатор слева подсвечена (alpha 1.0), когда выбран гео-город, иначе alpha 0.45;
  клик по ней по-прежнему ведёт к городу с индексом 0.
- Геометрия температуры (`rotate(-15f)`, `offset(-12, -24)`) не тронута.

### 5. Анимация смены города
- `AnimatedContent(targetState = uiState.selectedCityIndex)` вокруг **шапки** и вокруг
  **`HourlyWeatherCard`** (отдельные обёртки в своих `item`, весь `LazyColumn` не анимируется).
- `citySwitchTransition()`: направление по `targetState > initialState`
  (next → контент въезжает справа `it/4`, выезжает влево `-it/4`; prev — симметрично),
  `slideInHorizontally + fadeIn` / `slideOutHorizontally + fadeOut` по 280 мс,
  `ContentTransform(..., SizeTransform(clip = false))`.
- Свайп-жест (`detectHorizontalDragGestures`, порог ±60 px, `dragAccumulator`,
  `veilCooldownUntil`) — без изменений, всё так же на внешнем `Box` в `MainScreenContent`.

### 6. Добавление города
- `CityListScreen` += `onAddCity: () -> Unit`:
  - в шапке `KochIconButton(Icons.Default.Add, contentDescription = "Добавить город")` (accent);
  - последняя строка списка (новый `item(key = "add_city")`) — `KochListRow("Добавить город",
    "Поиск по названию", иконка Add)` → `onAddCity`.
- `CitySearchScreen` += `savedCities: List<CityLocation>`; у каждого результата:
  - не сохранён → `KochIconButton(Icons.Default.Add, "Добавить город <название>")` → `onSelectCity`;
  - сохранён (`CityCatalog.contains(savedCities, city)` — та же логика, что у VM:
    точка в пределах ~2 км или имя в пределах ~5 км) → бейдж «В списке», кнопки нет.
  - Старая стрелка `KeyboardArrowRight` убрана; строка по-прежнему кликабельна.
- `MainScreen`: `onAddCity = { viewModel.setTab(BottomTab.SEARCH) }`.
  После добавления VM сам выбирает город и переводит на «Главную» — «мёртвых» состояний нет.

### 1-ui. Локальный фон темы
- `MainScreenContent`: `KochSkyBackground(modifier = matchParentSize, imageRes = uiState.currentTheme.backgroundRes)`
  (расширение `AppThemeMode.backgroundRes` из `data/model/ThemeBackgrounds.kt`).
- Удалён `LaunchedEffect` предзагрузки фото неба через `wikimediaImageLoader` и использование
  `skyBlueImageUrl`/`skySunsetImageUrl`/`skyNorthernImageUrl` (сами геттеры в модели — зона data,
  не трогал).
- Статус-бар: `useDarkStatusIcons = veilVisible || koch.bgTop.luminance() > 0.35f`
  (без `skyPhotoLoaded`/`onPhotoLoaded`).

### 9-ui. Реклама — проброс действий
- В ленте: `AdBannerCard(creative = uiState.adCreative, imageUrl = uiState.adImageUrl,
  onClick = onOpenAdPromo)`; `onOpenAdPromo` = `viewModel.openAdPromo()` (увеличивает счётчик
  ротации) + открытие листа.
- `AdPromoDialog(creative = uiState.adCreative, imageUrl = uiState.adImageUrl,
  onAction = { ... }, onDismiss = { isAdPromoOpen = false })`.
  Роутинг `AdAction`: `PAYWALL → isPaywallOpen`, `LOOTBOX → isLootboxOpen`,
  `UMBRELLA_SHOP → viewModel.openUmbrellaShop()`, `FOOD → viewModel.openFoodSheet()`;
  лист рекламы при этом закрывается.

### 4-ui. Одна рулетка (внутри лутбокса)
- Удалены `isThemeRouletteOpen`, импорт `ThemeRouletteDialog`, блок `if (isThemeRouletteOpen)`
  и `onOpenRoulette` из вызова `ThemeGalleryScreen` (часть 2 удалила и сам файл, и параметр).
- `LootboxDialog` вызывается по новой сигнатуре:
  `lootboxState`, `availableThemes`, `currentTheme`, `onOpenLootbox`, `onApplyTheme`, `onDismiss`.

### Дополнительно от оркестратора
- `WeatherMapScreen`: удалены `onClose`, внутренний `BackHandler` и кнопка-стрелка из шапки;
  в шапке остались город, «Карта · OpenStreetMap · температура»; нижний инфо-блок и атрибуция
  OpenStreetMap сохранены. Добавлен `modifier: Modifier = Modifier` (MainScreen передаёт
  `padding(bottom = bottomContentPadding)`); у нижней карточки убран двойной
  `navigationBarsPadding`. Других вызовов `WeatherMapScreen`/`onClose` в дереве нет.
- Удалён `app/src/androidTest/kotlin/.../ui/main/MainScreenTest.kt` — остатки шаблона
  (`MainScreen(FAKE_DATA)`, «Hello Sample1!»); он не компилировался (нет `ui-test-junit4`
  в androidTest-зависимостях) и ломал `assembleDebugAndroidTest`. Новый instrumented-тест не нужен.

## Изменённые файлы (моя зона)

| Файл | Что |
|---|---|
| `ui/main/MainScreen.kt` | один Scaffold + when по вкладкам, BackHandler, hoisted `listState`, повторный тап «Главная», `AnimatedContent` шапки/часа, локальный фон, реклама, лутбокс, удаление рулетки, вуаль от `bottomContentPadding` |
| `ui/main/MainHeader.kt` | `isCurrentLocation`, подпись «ТЕКУЩЕЕ МЕСТО / ВЫБРАННЫЙ ГОРОД», подсветка гео-иконки |
| `ui/components/CityListScreen.kt` | `onAddCity`, «+» в шапке, строка «Добавить город», без стрелки «назад» |
| `ui/components/CitySearchScreen.kt` | `savedCities`, «+»/«В списке» у результатов, без стрелки «назад» |
| `ui/components/WeatherMapScreen.kt` | без `onClose`/`BackHandler`/стрелки, `modifier` для отступа под бар |
| `ui/design/KochHeader.kt` | `KochPageHeader.onBack: (() -> Unit)? = null` (стрелка только при non-null) |
| `app/src/androidTest/.../MainScreenTest.kt` | **удалён** (scaffolding) |

`MainScreenViewModel.kt`, `data/**`, `refs/`, `ACCEPTED`-файлы, тесты (кроме удалённого
androidTest) не менялись. Часть 2 не правила мои файлы, я не правил её файлы.

## Изменения сигнатур (для code-reviewer/judge)

1. `KochPageHeader(title, subtitle, onBack: (() -> Unit)? = null, modifier, actions)` —
   `onBack` стал nullable с дефолтом; при `null` стрелка не рисуется, заголовок начинается
   с `KochSpace.xl` (выровнен по контенту). Сейчас ни один вызов не передаёт `onBack`.
2. `GoldenCapsuleHeader(weather, citiesCount, selectedCityIndex, isCurrentLocation, onSelectCityIndex, onHeaderClick)`.
3. `CityListScreen(..., onRefresh, onSelectCity, onAddCity)` — `onBack` удалён
   (единственный вызов — `MainScreen`; навигация баром).
4. `CitySearchScreen(..., savedCities: List<CityLocation>, ..., onSelectCity)` — `onBack` удалён,
   добавлен `savedCities`.
5. `WeatherMapScreen(..., conditionRu, modifier: Modifier = Modifier)` — `onClose` удалён.
6. `MainScreenContent(uiState, listState, bottomContentPadding, ...)` — добавлены `listState`
   и `bottomContentPadding`; удалён неиспользуемый `onTabSelected` (бар теперь в `MainScreen`);
   собственного `Scaffold`/`KochNavBar` больше нет.

## Что осталось части 2 / открытые вопросы

1. `KochSkyBackground` по-прежнему объявляет необязательный `onPhotoLoaded` (файл части 2);
   вызовов в дереве не осталось — можно удалить параметр при желании (на компиляцию не влияет).
2. `KochPageHeader.onBack` теперь nullable, но неиспользуем: если часть 2 добавит страницу
   с кнопкой «назад» — передать колбэк; на вкладках он всегда `null`.
3. `ThemeGalleryScreen` часть 2 перевела на собственный `GalleryHeader` и убрала `onBack` —
   мой вызов уже без него.
4. `AdPromoDialog`/`AdBannerCard`/`LootboxDialog`/`ThemeGalleryScreen`/`KochSkyBackground` —
   все вызовы в `MainScreen` уже под новые сигнатуры части 2; при их дальнейшей правке
   сигнатуры ломать только вместе с `MainScreen` (единственный вызов).
5. Скриншоты для `device-qa` по моей зоне: `r14_01_navbar_themes`, `r14_02_navbar_list`
   (бар на «Вид»/«Списке»), `r14_03_swipe_animation` (2 кадра), `r14_04_city_add`
   (шапка списка + «Добавить город», плюс «В списке» в поиске), `r14_05_selected_city_header`
   («ВЫБРАННЫЙ ГОРОД» на добавленном городе), `r14_12_theme_background` (локальный фон),
   отдельно — карта без стрелки «назад» с видимым нижним баром и тапы по табам при вуали.

## Границы соблюдены

- Не коммитил, APK не собирал, adb не вызывал, `refs/`, `refs_src/`, `ACCEPTANCE.md`,
  `data/**`, `MainScreenViewModel.kt`, файлы части 2 не менял.
- Диск C не использовался: Gradle с `gradle.user.home=D:/.gradle` и `java.io.tmpdir` → `.local/tmp`.
