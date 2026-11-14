# Фаза 5 — бриф для ui-coder (интерфейс)

Читать вместе с `_history/reports/phase-5/plan.md`. Контракт данных и VM делает `data-coder`
(модели, репозитории, `MainScreenViewModel`), его отчёт — `_history/reports/phase-5/data-coder-logic.md`.
Тебе — `ui/**` (кроме `ui/main/MainScreenViewModel.kt`), `AndroidManifest.xml` (разрешение VIBRATE).

Правила: только Material Icons, никаких Unicode-эмодзи; все тексты по-русски; никаких заглушек
и «мёртвых» кнопок; `refs/` и `refs_src/` не менять; не коммитить; не собирать APK и не дёргать adb;
проверка — `compileDebugKotlin` + `testDebugUnitTest`.

---

## 3. Навбар не исчезает ни на одном экране

Сейчас `MainScreen` рендерит HOME через `MainScreenContent` (внутри свой `Scaffold` с `KochNavBar`),
а MAP/LIST/SEARCH/THEMES — отдельными полноэкранными composable без бара, со своей кнопкой «назад».

Сделать:
- Один `Scaffold` на уровне `MainScreen`: `containerColor = Transparent`,
  `contentWindowInsets = WindowInsets(0,0,0,0)`, `bottomBar = { Box(navigationBarsPadding().padding(horizontal=20.dp, vertical=12.dp)) { KochNavBar(...) } }`.
- Контент `when (selectedTab)`: HOME → лента (без собственного бара и без второго Scaffold),
  MAP → `WeatherMapScreen`, LIST → `CityListScreen`, SEARCH → `CitySearchScreen`, THEMES → `ThemeGalleryScreen`.
- Высота бара прокидывается вниз через `innerPadding.calculateBottomPadding()` — списки/карты/галерея
  не должны уходить под бар. Для `MapView` — `Modifier.padding(bottom = bottomPadding)`.
- Из `KochPageHeader` на этих экранах убрать `onBack` (навигация — только бар). Кнопку «назад»
  системным жестом/`BackHandler` оставить: на не-HOME вкладке `BackHandler { viewModel.setTab(HOME) }`.
- Тап по активному табу — скролл главной вверх (если HOME) или без действия.
- Не сломать облачную вуаль: её инсет считается от высоты бара — проверить, что табы нажимаются
  в состоянии «нет данных» (дефект прошлой фазы).

## 7. «ТЕКУЩЕЕ МЕСТО» только для гео-города

`MainHeader`: подпись `ТЕКУЩЕЕ МЕСТО` показывать только если `uiState.isSelectedCityCurrentLocation`,
иначе — `ВЫБРАННЫЙ ГОРОД` (тот же `microCaps`). Иконка-навигатор слева остаётся, но ведёт к гео-городу
и подсвечивается только когда он выбран.

## 5. Анимация смены города

Свайп по ленте уже переключает город (`onNextCity`/`onPrevCity`), но переход мгновенный.
- Обернуть шапку (`CityHeader`/`MainHeader`) и первый блок в `AnimatedContent`:
  `targetState = uiState.selectedCityIndex`, `transitionSpec` — slide по направлению (next → слева,
  prev → справа) + fade, 260–300 мс, `SizeTransform(clip = false)`.
- Направление определить по сравнению target/initial (`targetState > initialState` → next).
- Не анимировать весь `LazyColumn`: только шапку и карточку условий (иначе jank).
- Добавить лёгкий «overscroll»-эффект на границе списка городов не нужно.

## 6. Добавление города из списка

`CityListScreen`:
- в шапке кнопка «+» (`KochIconButton`, `Icons.Default.Add`) → `onAddCity`;
- последняя строка списка — «Добавить город» с плюсом и подписью «Поиск по названию» → `onAddCity`;
- `MainScreen`: `onAddCity = { viewModel.setTab(BottomTab.SEARCH) }`.
`CitySearchScreen`: у результата справа кнопка/иконка «+» («Добавить»), у уже сохранённого — «В списке»
(неактивна). После добавления — тост/снек «Город добавлен» и возврат в список (или остаться в поиске —
решить по UX, но без «мёртвых» состояний).

## 13. Оценка погоды без «Развернуть»

В `HourlyWeatherCard` удалить строку «Развернуть/Свернуть», `ratingExpanded` и скрытый блок.
Оставить: «Оцените погоду», «В среднем сегодня: 3.5 ★» (и «Ваша оценка: N ★» после выбора) и звёзды.
Никакой кнопки-обманки.

## 10. Осадки

- Вкладка «Осадки»: вместо ряда «—» — столбики мм/ч (`Canvas`, высота = mm/maxMm, 12–24 часа,
  подпись под каждым 3-м часом), заголовок «Осадки, мм/ч», сумма за период «Σ 4.2 мм».
  Цвет — `colors.accent`; нет данных → честный `KochEmptyState` «Нет данных об осадках».
- Если у источника есть только проценты (`popKnown = true`) — показывать % (как сейчас) и
  дополнительно полосу вероятности.
- `DailyDetailDialog`: вместо «Вероятность осадков / Нет данных» показывать «Осадки: 4.2 мм»
  (из `DailyItem.precipitationMm`), и «Вероятность: N %» только если `popKnown`.
- Метка сегмента при `RAIN`: «Осадки, мм/ч» (или «%» — если мм нет).

## 4. Одна рулетка — внутри лутбокса

- Удалить `ThemeRouletteDialog` как отдельный экран и убрать карточку «Рулетка тем» из
  `ThemeGalleryScreen` (и `onOpenRoulette` из сигнатур/`MainScreen`).
- Экран выпадения темы перенести в лутбокс: `LootboxReveal` с заголовком «Поздравляем!»,
  превью телефона (`PhonePreview`), жёлтые `◀ ▶` `Color(0xFFFFD600)` для листания выпавших тем,
  карточки «Оформление …», кнопка «Применить» и «Продолжить». Переиспользовать
  `PhonePreview`/`YellowArrow`/`ThemeTicket` из старого файла (перенести в `LootboxReveal.kt`
  или внутрь `LootboxDialog.kt`), сам `ThemeRouletteDialog.kt` удалить.
- Смысл: пользователь открывает лутбокс → анимация → «Вам выпала тема …» → «Применить».
  Никаких двух входов в одну механику.

## 8. Лутбокс как лутбокс (+ конфетти и вибрация)

`LootboxDialog` переделать:
- Закрытое состояние: нарисованный сундук (Canvas/Compose: корпус, крышка, замок, блики),
  мягкое свечение, подпись «Сундук Утешения #2025», шанс iPhone.
- Кнопка «Открыть бесплатно» → `isOpening`: тряска сундука 600–800 мс (rotate ±4°, scale 1→1.06),
  крышка приоткрывается, вспышка, затем конфетти (Canvas, 60–90 частиц, гравитация, 1.2 с) и
  вибрация: `Vibrator` + `VibrationEffect.createWaveform(longArrayOf(0,40,60,40), -1)`
  (в манифест добавить `android.permission.VIBRATE`; если API<26 — `vibrate(120)`).
- `LaunchedEffect(lootboxState.lastRewardSeq)` — ровно один выстрел анимации/вибрации на дроп.
- Карточка приза: свечение цвета редкости, крупная иконка (погода/тема/предмет), заголовок,
  подзаголовок, для темы — превью + «Применить», для погоды — «Поздравляем! Вам выпал дождь на завтра».
- История дропов — из `lootboxState.history` (персистентная), последние 5, с цветом редкости.
- Никаких «рандомная хуйня»: у каждого элемента понятное действие.

## 9. Реклама: ротация и понятный CTA

- `AdBannerCard` и `AdPromoDialog` рисуют `uiState.adCreative` (brandRu, headline, body, ctaLabel,
  disclaimerRu) и `uiState.adImageUrl`; CTA по `AdAction`:
  PAYWALL → `onOpenPaywall`, LOOTBOX → `onOpenLootbox`, UMBRELLA_SHOP → `onOpenUmbrellaShop`,
  FOOD → `onOpenFood`.
- Под CTA в листе: мелкий `disclaimerRu` («Реклама. …») и вторичная строка-ссылка
  «Отключить рекламу — Премиум 179 ₽/год» с пояснением, что даёт премиум (без рекламы, все темы).
- Баннер: «РЕКЛАМА» + заголовок креатива + CTA-иконка; фото — из `adImageUrl` соответствующей категории.
- Убедиться, что два разных открытия листа дают разные креативы (ротация из VM).

## 1. Локальные фоны тем

- `KochSkyBackground` += параметр `@DrawableRes imageRes: Int?`; рисовать `Image(painterResource(imageRes),
  contentScale = Crop)` поверх градиента, скрим оставить. Сетевые `sky*ImageUrl` как фон больше не
  использовать; предзагрузку фото неба в `MainScreen` убрать.
- `MainScreenContent`: `imageRes = uiState.currentTheme.backgroundRes` (расширение из
  `data/model/ThemeBackgrounds.kt`, создаёт data-coder).
- `ThemeGalleryScreen.ThemeCard`: превью — локальный блюр-фон темы (`Image(painterResource(...), Crop)`)
  вместо градиента, чтобы «Вид» не выглядел как набор цветных прямоугольников.
- Прозрачность/контраст: белый текст поверх фото уже закрыт скримом — проверить на светлых темах.

## Definition of done

`compileDebugKotlin` и `testDebugUnitTest` зелёные; в отчёте — список файлов, что удалено
(`ThemeRouletteDialog`), какие composable переписаны, какие скриншоты нужно снять `device-qa`
(список ниже), открытые вопросы.

Скриншоты, которые потом снимает device-qa:
`r14_01_navbar_themes`, `r14_02_navbar_list`, `r14_03_swipe_animation` (2 кадра),
`r14_04_city_add`, `r14_05_selected_city_header`, `r14_06_lootbox_closed`,
`r14_07_lootbox_reveal` (с конфетти), `r14_08_ad_creative_a`, `r14_09_ad_creative_b`,
`r14_10_rain_chart`, `r14_11_rating_no_expand`, `r14_12_theme_background`,
`r14_13_no_byn_minsk`, `r14_14_price_taxi`.
