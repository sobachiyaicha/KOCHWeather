# code-reviewer — Phase 4 (MET Norway / Open-Meteo / geocoding chain, full-screen tabs, deep links, photos)

- **Роль:** `code-reviewer` (независимый критик кода). Код не правился, APK не собирался, adb не использовался.
- **Дата ревью:** 2026-11-14, 03:28–03:47 MSK.
- **Границы фазы:** `git diff a16197a..HEAD`, HEAD = **`ce45a66`** (`docs(shots): add phase 4 run 2 device evidence, ANR trace and benchmark build log`). Коммит `ce45a66` добавляет только `shots/` и `_history/reports/phase-4/device-qa.md`; код в HEAD идентичен `4c588c7` (`git diff --name-only 4c588c7..HEAD -- app/src` пуст). Плюс текущее рабочее дерево — **оно НЕ чистое** (см. §0).
- **Артефакты-доказательства:** `shots/anr_trace_run2.txt` (1 547 473 байта, тот же файл, что я читал как `shots/_anr_trace.txt`), `shots/logcat_run2.txt`, `shots/gfxinfo_run2.txt`, `shots/r2_*.png`.

## ВЕРДИКТ: **FAIL**

Причина: **blocker №1 (ANR на холодном старте) в коде HEAD не устранён**, есть blocker №2 (объём dex/классов на критическом пути старта) и 9 major (в т.ч. обход разблокировки тем, выдуманные значения в маппере, непокрытая тестами логика загрузки/кэша).

---

## 0. Состояние дерева на момент ревью (важно для judge)

- На старте ревью (03:28): tracked-дерево чистое, `?? shots/*` — доказательства.
- **03:26** появился `f78f805` (REFERENCE_TRANSCRIPT.md), **03:38** — `ce45a66` (evidence). Код не менялся.
- **03:41:40** рабочее дерево **изменено другим агентом** (не мной): `app/src/main/kotlin/.../data/model/WeatherModels.kt` и `.../ui/main/MainScreenViewModel.kt` (`git diff --stat`: 2 files, +128/−84). Это незакоммиченный WIP, который **меняет вывод по блокеру ANR** (см. §7). Отчёт по находкам ниже — про **HEAD `ce45a66`**; WIP разобран отдельно и требует перепроверки/коммита.
- Из-за этого прогон тестов (03:32) относится к HEAD, а не к WIP-дереву. После коммита WIP тесты и ANR-доказательства **обязаны быть пересняты** (см. §8).

---

## 1. БЛОКЕРЫ ANR (R22) — явный список

### B1. BLOCKER — граф зависимостей ViewModel строится на main-потоке в первой композиции

**Факты (не теория):** `shots/anr_trace_run2.txt:117-143`

```
"main" prio=5 tid=1 Runnable ... schedstat=( 5732174579 323928059 2148 ) utm=541 stm=31
  at com.koch.weather.data.network.NetworkMonitor.<init>(NetworkMonitor.kt:18)
  at com.koch.weather.ui.main.MainScreenViewModel.<init>(MainScreenViewModel.kt:23)
  at com.koch.weather.ui.main.MainScreenViewModel.<init>(MainScreenViewModel.kt:-1)   // @JvmOverloads-обёртка
  ...
  at androidx.lifecycle.viewmodel.compose.ViewModelKt.viewModel(ViewModel.kt:106)
  at com.koch.weather.ui.main.MainScreenKt.MainScreen(MainScreen.kt:2148)
```

Причина ANR из лога (device-qa): `Input dispatching timed out (ActivityRecord ... does not have a focused window)` — окно не получило фокус за 5 с. Главный поток **сжёг 5,41 с user CPU** (`utm=541` при `HZ=100`), к моменту дампа всё ещё находясь в конструкторе ViewModel. При этом в стеке **нет** ни wait/binder/disk-фрейма: это не блокировка, а стоимость класс-лоадинга + жадного создания графа.

**Код-причина (file:line → что именно):**
- `ui/main/MainScreen.kt:117` — `viewModel: MainScreenViewModel = viewModel()` вызывается из композиции → конструктор (и всё, что он создаёт) исполняется на main при первом кадре.
- `ui/main/MainScreenViewModel.kt:20-31` — **11 не-ленивых** зависимостей в default-args конструктора: `WeatherRepository` (:22), `NetworkMonitor` (:23), `WeatherCache` (:24), `SubscriptionRepository` (:25), `LootboxRepository` (:26), `TaxiRepository` (:27), `FoodRepository` (:28), `FlightRepository` (:29), `AiFactsRepository` (:30), `ImageRepository` (:31); плюс `@JvmOverloads` порождает 12 конструкторов.
- `ui/main/MainScreenViewModel.kt:34-50` — начальное состояние вызывает `repository.defaultCities`, `aiFactsRepository.getDefaultFact()`, `foodRepository.getRestaurants()`, `flightRepository.getFlightOffers()`, `imageRepository.getUrlsByCategory()` — т.е. тянет классы каталогов на main.
- `data/repository/WeatherRepository.kt:39-60` — `OpenMeteoClient()`, `MetNoProvider()`, `PhotonGeocodingProvider()`, `NominatimGeocodingProvider()`, `WeatherCache`, 2×`Json`, 2 `ConcurrentHashMap`.
- 4 отдельных `OkHttpClient` строятся жадно: `data/api/OpenMeteoClient.kt:16-22`, `data/provider/MetNoProvider.kt:22` (→ `:131-137`), `data/provider/PhotonGeocodingProvider.kt:27` (→ `:113-118`), `data/provider/NominatimGeocodingProvider.kt:27` (→ `:127-132`).

**Как исправить (приоритет):**
1. Конструктор ViewModel должен быть «пустым»: только `MutableStateFlow` с лёгким начальным состоянием. Тяжёлые объекты — в app-scoped контейнере, создаваемые **на IO** (в `init`-корутине `Dispatchers.IO`) либо `by lazy` с единственной точкой входа с IO.
2. Убрать `@JvmOverloads` и default-args; внедрять зависимости через `ViewModelProvider.Factory` от контейнера.
3. Начальное состояние не должно вызывать репозитории: `cities`/`aiFact`/`restaurants`/`offers`/`imageUrls` заполнять из IO (это уже делает WIP §7).
4. Ничего из `WeatherRepository`/OkHttp не должно создаваться до первого сетевого вызова.

### B2. BLOCKER (вклад в тот же ANR + валидность R18) — 13 dex и 34 МБ иконок на старте, R8 выключен

- `app/build.gradle.kts:94` — `implementation(libs.androidx.compose.material.icons.extended)`. Размер AAR в кэше Gradle: **34,07 МБ** (`material-icons-core` — 0,79 МБ). В коде всего **95** обращений к `Icons.*`.
- Трасса: APK грузит `classes2..classes13.dex` (`anr_trace_run2.txt:11`) — **13 dex-файлов**; промежуточный `mergeExtDexDebug/classes.dex` = **41,32 МБ**, `classes2.dex` = 14,96 МБ, `classes3.dex` = 5,84 МБ (debug-вариант, без R8).
- `app/build.gradle.kts:34, 38, 47` — `isMinifyEnabled = false` у `release`, `debug` **и** `benchmark`; baseline profile отсутствует. Cold start после `pm clear` = верификация/резолв этих классов на main.
- `app/build.gradle.kts:97-98` — `debugImplementation(ui-tooling, ui-test-manifest)` попадают в debug-APK, на котором и снимался ANR/gfxinfo.

**Как исправить:** заменить `material-icons-extended` на `material-icons-core` + 5–10 нужных иконок как `ImageVector`/vector-drawable; включить `isMinifyEnabled = true` (с `proguard-rules`) для `benchmark`/`release`; добавить baseline profile; ANR и R18 мерить на `benchmark`-сборке, а не на debug.

---

## 2. ANR-риски на других экранах (не холодный старт, но тот же класс дефекта)

| # | file:line | severity | Проблема | Как исправить |
|---|---|---|---|---|
| M5 | `ui/components/WeatherMapScreen.kt:62-81` | major | В `remember { }` внутри композиции: `Configuration.getInstance().load(...)` — синхронное чтение SharedPreferences с диска, затем `MapView(context)` (тайл-кэш, менеджеры, потоки). Первое открытие таба «Карта» = диск-I/O на main; экран пересоздаётся при каждом переключении таба, `remember` не сохраняется. | `Configuration.load` — один раз на IO до навигации (или в Application); `MapView` создавать в `AndroidView(factory = …)`; хранить `MapView` на уровне экрана/активити, а не в `remember` пересоздаваемого композабла. |
| M6 | `ui/components/WeatherImage.kt:77` | major | `remember(context) { WikimediaImageLoader.get(context) }` строит `OkHttpClient` + `coil.ImageLoader` в композиции на main; disk-cache Coil материализуется на первом запросе в вызывающем потоке. | App-scoped `ImageLoader` (создать на IO/в контейнере), либо `SingletonImageLoader.setSafe`; в композиции только получать готовый инстанс. |

Примечание: `CloudVeilOverlay` **не участвует в холодном старте** (`isCloudLocked=false` в `WeatherModels.kt:277`, поднимается таймером через 15 с, `MainScreen.kt:665-667`) — в ANR он не виноват, но даёт jank (см. m6).

---

## 3. MAJOR

### M1. Рулетка тем обходит разблокировку (R6/R15)
- `ui/components/ThemeRouletteDialog.kt:277` — `isUnlocked` вычисляется и используется **только для подписи**.
- `:380-391` — «Рулетка!» крутит `allThemes = AppThemeMode.values()` и применяет `onSelectTheme(previewTheme)`.
- `:408-411` — «Продолжить» применяет `previewTheme` **без проверки** `availableThemes`.
- Следствие: закрытые темы (CYBERPUNK, BLOODY_SUNSET, AURORA_BOREALIS) применяются бесплатно, хотя `ThemeGalleryScreen.kt:143` их блокирует, а `LootboxRepository` — единственный легальный источник (`ThemeModels.kt:20-30`). Это ещё и бизнес-логика в composable.
- **Fix:** пул прокрутки = `availableThemes`; для закрытой темы «Продолжить» заблокировать/показать «Открывается в лутбоксах»; выбор темы перенести в VM/репозиторий.

### M2. Выдуманные значения вместо «нет данных» (R21)
- `data/mapper/WeatherMapper.kt:75` — `humidity = current.relativeHumidity2m ?: 0` (**0 % влажности** — выдумка).
- `data/mapper/WeatherMapper.kt:76` — `windSpeed = current.windSpeed10m ?: 0.0`.
- `data/mapper/WeatherMapper.kt:140` — `wind = hourlyWinds.getOrNull(i) ?: 0.0`.
- `data/provider/MetNoForecastParser.kt:115` — `MetNoSymbolMapper.toWmo(point.symbolCode) ?: currentWmo` (чужой код погоды для часа).
- `data/provider/MetNoForecastParser.kt:117` — `it.windSpeed ?: 0.0` при комментарии «никаких выдуманных значений» выше.
- **Fix:** сделать `humidity: Int?`, `windSpeed: Double?`, `HourlyItem.wind: Double?`, `weatherCode` nullable/`popKnown`-подобный флаг; в UI показывать «—»/«нет данных».

### M3. Слишком долгое ожидание и первый запуск «в ошибку»
- `data/provider/WeatherProviderChain.kt:60-85` — последовательный перебор без общего дедлайна; таймауты провайдеров 15 с connect / 15 с read / 20 с call (`OpenMeteoClient.kt:16-22`, `MetNoProvider.kt:131-137`) → до **40 с** до честного «нет данных».
- Геокодинг-цепочка Photon→Nominatim→Open-Meteo с теми же таймаутами (`PhotonGeocodingProvider.kt:113-118`, `NominatimGeocodingProvider.kt:127-132`) → до **60 с** на один поиск.
- `data/repository/WeatherRepository.kt:180` — перед отдачей прогноза синхронно ждётся `trafficRepository.getTraffic(city)` (HTTP 4 с connect + 4 с read, `TrafficRepository.kt:120-128`, без кэша при первом запуске) → +8 с к отображению **погоды**, хотя пробки к погоде не относятся.
- Доказательство с устройства: run 2, первый запуск после `pm clear` закончился шторкой «MET Norway недоступен: Read timed out.; Open-Meteo недоступен…» (`_history/reports/phase-4/device-qa.md:145-153`, `shots/r2_04_clouds_cleared.png`).
- **Fix:** общий бюджет загрузки (например 10–12 с), трафик — параллельно/после показа прогноза, ошибка сети — с автоповтором (1 попытка) и возможностью показать кэш.

### M4. Отмена корутины не отменяет HTTP-запрос
- Блокирующие вызовы внутри `withContext(Dispatchers.IO)`: `MetNoProvider.kt:72-73`, `OpenMeteoClient.kt:45` и `:78`, `PhotonGeocodingProvider.kt:41`, `NominatimGeocodingProvider.kt:51`.
- `ui/main/MainScreenViewModel.kt:305` (`loadJob?.cancel()`) и `:773` (`searchJob?.cancel()`) корутину отменяют, но `Call` продолжает жить до таймаута: при быстром вводе пользователь плодит до N×3 параллельных запросов, старые висят в IO до 20 с (и только результат отбрасывается проверкой `searchQuery`).
- **Fix:** `suspendCancellableCoroutine` + `invokeOnCancellation { call.cancel() }`, либо OkHttp `enqueue` + `await`; в цепочках — проверка `ensureActive()` между провайдерами.

### M5/M6 — см. таблицу §2.

### M7. Рекомпозиции: нестабильные модели и Brush в каждой композиции
- `data/model/WeatherModels.kt:111-130` (`WeatherDomainData`) и `:227-313` (`MainScreenUiState`) содержат `List<…>` → Compose считает их **unstable**; `MainScreenContent(uiState)` и все карточки (`HourlyWeatherCard`, `TenDayForecastCard`, `ServicesGrid`…) не skippable. Любое изменение любого поля (ввод в поиске `:772`, рейтинг, таймер облаков) пересобирает домашний экран, который в этот момент даже не виден.
- `ui/main/MainScreen.kt:205` и `:411` — `getThemePalette(uiState.currentTheme)` вызывается в композиции **дважды за рекомпозицию** и каждый раз создаёт новые `CustomThemeColors` + `Brush.verticalGradient` (`ui/theme/Theme.kt:21-101`); меняется identity → `Modifier.background(brush)` перезаписывается.
- `ui/main/MainScreen.kt:112` — `SimpleDateFormat` создаётся на каждый вызов `formatWeatherTime()` из композиции.
- **Fix:** `@Immutable` на модели UI (списки только заменяются, что уже соблюдается) либо `kotlinx.collections.immutable`; `remember(uiState.currentTheme) { getThemePalette(...) }`; `remember { SimpleDateFormat(...) }`; при необходимости — отдельные `StateFlow`/`derivedStateOf` вместо одного большого `uiState`.

### M8. Ключевая логика не покрыта тестами (R21/R22)
- `app/src/test/.../MainScreenViewModelTest.kt:15-219` — тестируются только `MainScreenUiState`, enum'ы и статические репозитории. Сам `MainScreenViewModel` (876 строк: offline→CACHED→LIVE, `selectCity`, cloud timer, premium, поиск) не инстанцируется ни разу.
- `WeatherRepository` (кэш-политика MET 30 мин/3 ч, failover, миграция старого кэша, «нет данных»), `MetNoProvider` (HTTP-ошибки), `WeatherCache` не покрыты.
- `EmojiFreeTextTest.kt:65-87` сканирует только `data/` + `MainScreenViewModel.kt`, UI-пакеты не сканирует (эмодзи там и нет, но тест это не гарантирует).
- **Fix:** вынести загрузку в тестируемый use-case (`WeatherLoadCoordinator`) с инъекцией `WeatherProvider`, `Clock`, `WeatherCache`-интерфейса; тесты на: свежий MET-кэш → без сети; протухший кэш → сеть; обе сети упали → CACHED с `reason`; пустой кэш → Unavailable; отмена/дедупликация запросов.

### M9. `CloudVeilOverlay` — тяжёлый слой (jank, не ANR)
- `ui/components/CloudVeilOverlay.kt:61` — `graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)` на весь экран (лишний full-screen буфер каждый кадр).
- `:198-232` — `Brush.verticalGradient(listOf(...))` аллоцируется **на каждый draw**; 23 `drawCircle`.
- `:107-120` — на каждый кадр перерисовываются все `eraseMarks` (до 220) `BlendMode.Clear` + 23 круга + градиент = до 243 draw-операций; `:89-91` — `removeAt(0)` по одному.
- `:176` — текст с процентом читает `wipeProgress` в той же композиции (перекомпоновка на каждый drag).
- **Fix:** `drawWithCache`/`remember` для градиента и «облаков», маска-`Path`/`ImageBitmap` вместо 220 кругов, ограничить частоту обновления текста, подсказку/прогресс вынести из `Canvas`.

---

## 4. MINOR (не блокирует PASS, но перечислить)

**Мёртвый код / неиспользуемое API**
- `data/repository/ImageRepository.kt:17-30` — `getUrls`, `getPrimaryUrl`, `getAllImages`, `userAgent` не используются в main (только тесты).
- `data/cache/WeatherCache.kt:152` — `getLastUpdatedTime` не используется.
- `data/deeplink/DeepLinks.kt:97` — `yandexMapsRoute` не используется в main.
- `data/repository/LootboxRepository.kt:108-111` — `getThemeCatalog`, `getHistory` не используются.
- `ui/main/MainScreenViewModel.kt:233` — `loadWeather(city)` не используется.
- `data/model/WeatherModels.kt:308-309` — `citySummaries`, `isLoadingCitiesWeather` не используются; `:171` — `BottomTab.CITIES` (алиас).
- `data/model/EmojiTextGuard.kt:19` — `findAll` не используется.
- `data/repository/TrafficRepository.kt:23` — `apiKey` не используется (URL `export.yandex.ru/bar/reginfo.xml` ключ не требует); `app/build.gradle.kts:29` — `TRAFFIC_API_KEY` только пишется в BuildConfig и нигде не читается → мёртвая конфигурация.
- `ui/components/CityListScreen.kt:78` — ветка `isLoading && cities.isEmpty()` недостижима: `isLoadingSavedCities` нигде не выставляется в `true`.

**Данные/логика**
- `ui/main/MainScreen.kt:1731` — жёстко зашито «В среднем сегодня: 3.5 ★»; `MainScreenUiState.rating` (`WeatherModels.kt:294`) обновляется в `rateWeather` (`MainScreenViewModel.kt:474-489`), но **нигде не отображается** (среднее всегда 3.5). Либо выводить `rating`, либо убрать состояние.
- `data/network/NetworkMonitor.kt:18-56` — `callbackFlow` с общим `NetworkRequest(INTERNET)`; `onLost` любого интернета даёт `false` (`:25-28`), `onCapabilitiesChanged` не обрабатывается, при Wi-Fi+LTE потеря одной сети = ложный «нет сети» (R19). Использовать `registerDefaultNetworkCallback` или учитывать набор сетей. `:63` — лог на каждый вызов.
- `data/cache/WeatherCache.kt:39-46` + `data/repository/WeatherRepository.kt:63-64` + `ui/main/MainScreenViewModel.kt:830` — кэш и дедупликация городов по **имени**: «Москва» (Россия) и «Moscow» (Idaho) — разные, а два одноимённых города в разных странах перезапишут прогноз друг друга. Использовать key = `name|lat|lon`.
- `ui/main/MainScreenViewModel.kt:120-123` — `initialIndex` через `indexOfFirst(...).coerceAtLeast(0)`: если сохранённого города нет в списке, `selectedCity` = один город, а индекс = 0 → неверные точки карусели.
- `ui/main/MainScreenViewModel.kt:358-390` — при ошибке уже загруженные в этой сессии live-данные переподписываются как «Сохранённый прогноз»/`CACHED` (это не DataStore-кэш). Лучше отдельное состояние «данные устарели».
- `data/repository/WeatherRepository.kt:93-99` — `getSavedCitiesWeather` делает `DataStore.data.first()` **на каждый город** (N полных чтений). Прочитать prefs один раз и разобрать все города.
- `data/provider/MetNoForecastParser.kt:120-141` — суточные min/max для «Сегодня» считаются только по точкам timeseries (MET отдаёт с текущего часа) → днём min завышен. Использовать `next_6_hours/next_12_hours.details` (min/max) для сегодняшнего дня.
- `data/provider/MetNoForecastParser.kt:100` и `data/mapper/WeatherMapper.kt:197` — «день» захардкожен как 6..21; в 21:30 будет дневная иконка. Использовать `is_day` источника/астрономические сумерки.
- `data/repository/LootboxRepository.kt:12, 91` — `mutableListOf<LootReward>` мутируется из IO и отдаётся наружу (`getHistory`) без синхронизации.
- `ui/components/GrassLockDialog.kt:108-119, 162-174` — «камера» фейковая: ни `Intent(MediaStore.ACTION_IMAGE_CAPTURE)`, ни чтения фото, ни проверки; два одинаковых блока `delay()`-симуляции. При этом `AndroidManifest.xml:7` объявляет `CAMERA`, который нигде не используется. Либо реализовать, либо честно назвать симуляцией и убрать permission.
- `ui/components/PaywallDialog.kt:228` — покупка по одному клику без подтверждения (device-qa случайно активировал премиум в ходе аудита). WIP §7 добавляет подтверждение, но UI ещё не переключён (см. §7).
- `ui/main/MainScreen.kt:676-754` — `WeatherUnavailableCard` всегда пишет «Проверьте подключение…», даже когда оффлайн-баннер уже это сказал; текст накладывается на подсказку облаков (device-qa `r2_04`).
- `ui/main/MainScreen.kt:446` — `contentPadding bottom = 96.dp` при баре `64.dp + 12.dp` (`:2061`, `:649`); device-qa фиксировал карточки под баром — проверить на `benchmark`-сборке (визуальный дефект, зона `visual-critic`).
- Дублирование строк: цена `5.99/29.99 BYN` в `MainScreen.kt:1011,1018` и `PaywallDialog.kt:181,189`; «Зонтики от 29.99 BYN» (`MainScreen.kt:1953`) — вынести в константы/ресурсы.

**Архитектура/гигиена**
- Размеры: `ui/main/MainScreen.kt` — 2146 строк, `ui/main/MainScreenViewModel.kt` — 876, `ThemeRouletteDialog.kt` — 432, `FlightBookingDialog.kt` — 427, `FoodBottomSheet.kt` — 410, `TaxiBottomSheet.kt` — 408. Разбить по карточкам/секциям; `MainScreen` держит 22 лямбды-колбэка.
- `@JvmOverloads` на ViewModel (`MainScreenViewModel.kt:20`) — 12 конструкторов ради DI-удобства; заменяется фабрикой.
- `WeatherCache` (`data/cache/WeatherCache.kt:52-158`) проглатывает **все** исключения пустыми `catch` — повреждение DataStore не видно ни в логах, ни в UI.
- 4 независимых `OkHttpClient` с одинаковыми настройками (см. B1) — один общий клиент на приложение (меньше потоков/соединений/классов).

---

## 5. Позитивные факты (чтобы не звучало однобоко)

- Эмодзи: **0** (проверил весь `app/src` + ресурсы: только типографика `° ★ ◀ ▶ →`, разрешённая refs/`EmojiTextGuard.kt:9-17`).
- `TODO`/`FIXME`/`HACK` в `app/src`: **0**.
- Секретов в отслеживаемых файлах нет; `local.properties` в `.gitignore` и не отслеживается; `TRAFFIC_API_KEY` пустой.
- Слои разделены: `data/provider` (чистые парсеры/цепочки) ↔ `data/mapper` ↔ `ui`; парсеры покрыты JVM-тестами (99 тестов).
- Честные состояния «нет данных» реализованы: `WeatherDataState.UNAVAILABLE`, `SavedCityWeather.tempC == null` → «Нет данных», `HourlyItem.popKnown`, `WeatherSource.NONE`.
- Кэш: снимок полного `WeatherDomainData` в DataStore (`WeatherRepository.kt:199-208`), миграция старого формата (`:293-322`), политика MET Norway вынесена в `MetNoCachePolicy` и покрыта тестами.
- Deep links: чистые функции + `resolveActivity` + `FLAG_ACTIVITY_NEW_TASK` + обработка `ActivityNotFoundException` (`DeepLinks.kt:55-148`, `DeepLinkAndroid.kt:22-27`, `DeepLinkLauncher.kt:19-30`), в UI есть подтверждение (`TaxiBottomSheet.kt:282-319`).
- `savedInstanceState`-независимое состояние (всё в VM), `collectAsStateWithLifecycle`, `BackHandler` на полноэкранных экранах, стабильные `key` в списках (`CityListScreen.kt:118`, `CitySearchScreen.kt:206`, `ThemeGalleryScreen.kt:108`).

---

## 6. Тесты (обязательный прогон)

Команда (из корня проекта):

```
$env:JAVA_HOME="D:\jdk-17\jdk-17.0.12+7"
$env:TEMP=$env:TMP="D:\akcon\weatherAPK-dsh\.local\tmp"
.\gradlew.bat --console=plain testDebugUnitTest
```

Результат (**03:32, HEAD `ce45a66` = код `4c588c7`**): **BUILD SUCCESSFUL in 17s**, `:app:compileDebugKotlin UP-TO-DATE`, `:app:compileDebugUnitTestKotlin` и `:app:testDebugUnitTest` — executed.

- **20 сьютов, 99 тестов, падений 0, пропущено 0** (парсинг `app/build/test-results/testDebugUnitTest/*.xml`).
- По сьютам: WeatherMapperTest 13, MainScreenViewModelTest 13, DeepLinksTest 9, WeatherErrorsTest 7, GeocodingChainTest 6, MetNoForecastParserTest 5, ThemeCatalogTest 5, EmojiFreeTextTest 4, ImageCatalogTest 4, NominatimGeocodingParserTest 4, ProviderChainTest 4, TaxiDestinationTest 4, MetNoCachePolicyTest 3, MetNoSymbolMapperTest 3, PhotonGeocodingParserTest 3, SavedCityWeatherTest 3, WmoHelperTest 3, TaxiRepositoryTest 3, OpenMeteoSerializationTest 2, WeatherSnapshotTest 1.
- Компиляция main-кода подтверждена (`compileDebugKotlin` — UP-TO-DATE на актуальных исходниках).
- **Важно:** прогон сделан до правок рабочего дерева (§0/§7). После коммита WIP прогон повторить; `testDebugUnitTest` не покрывает ViewModel/Repository/провайдеров (M8), поэтому «зелёные тесты» не являются доказательством отсутствия регрессий в загрузке погоды.

---

## 7. Незакоммиченный WIP рабочего дерева (03:41:40) — влияет на блокер ANR

Файлы: `data/model/WeatherModels.kt` (+`isPremiumPurchasePending`), `ui/main/MainScreenViewModel.kt` (+128/−84).

Что уже исправлено структурно:
- Убран `@JvmOverloads` и 11 default-args; зависимости — `by lazy` (`repository`, `networkMonitor`, `cache`, 8 репозиториев).
- Начальный `_uiState` больше не зовёт репозитории (только `isLoading`, `weatherState`, `isWeatherUnavailable`).
- В `init` вся тяжёлая инициализация (города, кэш, статические каталоги, картинки) перенесена в `viewModelScope.launch(Dispatchers.IO)` и **до** первого сетевого запроса; `refreshAiFact`, `openFoodSheet`, `selectRestaurant`, `openFlightSheet` больше не блокируют main.
- Пейволл: `requestPremiumPurchase()` → `isPremiumPurchasePending` → `confirmPremiumPurchase()` пишет в DataStore только после подтверждения.

Риски WIP, которые нужно закрыть до коммита:
1. **Кнопка покупки сейчас «мёртвая»:** `PaywallDialog` (`ui/components/PaywallDialog.kt:228`) и `MainScreen.kt:303` всё ещё вызывают `viewModel.purchasePremium()` → deprecated-обёртку → `confirmPremiumPurchase()` → `if (!isPremiumPurchasePending) return` — pending никто не выставляет. Либо перевести UI на `requestPremiumPurchase()` + диалог подтверждения, либо временно оставить старый прямой путь.
2. `by lazy` по умолчанию **synchronized**: если пользователь успеет нажать город/поиск/еду до завершения `init`-корутины, первый доступ к `repository` произойдёт на main и заблокирует UI на время создания `WeatherRepository` (4 OkHttpClient). Инициализировать контейнер строго в IO-корутине и обращаться к нему из main только после готовности (флаг/`StateFlow`), либо `lazy(LazyThreadSafetyMode.NONE)` + дисциплина «только IO».
3. `selectCity` теперь сбрасывает `weather = null` и ставит LOADING до чтения кэша — при переключении на город с кэшем будет мигание спиннера (раньше показывался кэш мгновенно). Компромисс осознанный, но проверить визуально.
4. `openFlightSheet` открывает шторку с пустым `offers` до ответа IO — пустой список на кадр.

**Вывод по WIP:** он снимает основную причину B1 на уровне кода, но **без новой сборки + установки + повторного `pm clear`/`am start` + нового ANR-трейса** блокер закрывать нельзя. И тесты на новое поведение (pending/premium) отсутствуют.

---

## 8. R18 — статус требования (фиксация, без правок)

- Утверждение «нет release-подобного buildType» **устарело**: `app/build.gradle.kts:44-52` объявляет `benchmark` (`initWith(release)`, `isDebuggable=false`, `isProfileable=true`, подпись debug-ключом, тот же `applicationId`, `versionNameSuffix="-benchmark"`). Сборка возможна как `assembleBenchmark`.
- Но: (а) `isMinifyEnabled=false` во **всех** типах сборки (`:34, :38, :47`) — старт/класс-лоадинг не такие, как в проде; (б) baseline profile/macrobenchmark отсутствуют; (в) фактический замер `shots/gfxinfo_run2.txt` снят на **debug-APK** (`app/build/outputs/apk/debug/app-debug.apk`, 03:15:32, `_history/reports/phase-4/device-qa.md:121`) → для R18 невалиден. Jank 32,5 %, p90/p95 = 150 мс при `p50 = 9 мс`.
- Для `perf-qa`: ставить `benchmark`-вариант, снимать `gfxinfo framestats` и startup (`am start -W`) именно на нём; ANR перепроверять на нём же (тогда исчезнет и debug-FpsOverlay).

---

## 9. Проверенные файлы

**Data/provider:** `OpenMeteoClient.kt`, `OpenMeteoExceptions.kt`, `MetNoProvider.kt`, `MetNoForecastParser.kt`, `MetNoModels.kt`, `MetNoCachePolicy.kt`, `MetNoSymbolMapper.kt`, `OpenMeteoProvider.kt`, `WeatherProvider.kt`, `GeocodingProvider.kt`, `PhotonGeocodingProvider.kt`, `NominatimGeocodingProvider.kt`, `OpenMeteoGeocodingProvider.kt`.

**Data/cache/repository/model/mapper/deeplink:** `WeatherCache.kt`, `WeatherRepository.kt`, `ImageRepository.kt`, `TrafficRepository.kt`, `TaxiRepository.kt`, `FoodRepository.kt`, `FlightRepository.kt`, `LootboxRepository.kt`, `SubscriptionRepository.kt`, `AiFactsRepository.kt`, `WeatherMapper.kt`, `WeatherErrors.kt`, `WeatherModels.kt`, `LoadResultModels.kt`, `SavedCityWeather.kt`, `ThemeModels.kt`, `ImageModels.kt`, `EmojiTextGuard.kt`, `WmoHelper.kt`, `NetworkMonitor.kt`, `DeepLinks.kt`, `DeepLinkAndroid.kt`.

**UI:** `MainScreen.kt`, `MainScreenViewModel.kt`, `CityListScreen.kt`, `CitySearchScreen.kt`, `ThemeGalleryScreen.kt`, `WeatherMapScreen.kt`, `CloudVeilOverlay.kt`, `WeatherImage.kt`, `FullScreenScaffold.kt`, `DeepLinkLauncher.kt`, `ThemeRouletteDialog.kt`, `PaywallDialog.kt`, `TaxiBottomSheet.kt`, `FoodBottomSheet.kt`, `GrassLockDialog.kt`, `FpsOverlay.kt`, `LocationFormat.kt`, `Theme.kt`, `Color.kt`.

**Конфиг:** `app/build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `app/src/main/AndroidManifest.xml`, `.gitignore`.

**Тесты:** `DeepLinksTest`, `EmojiFreeTextTest`, `GeocodingChainTest`, `ImageCatalogTest`, `MetNoCachePolicyTest`, `MetNoForecastParserTest`, `MetNoSymbolMapperTest`, `NominatimGeocodingParserTest`, `PhotonGeocodingParserTest`, `ProviderChainTest`, `SavedCityWeatherTest`, `TaxiDestinationTest`, `TaxiRepositoryTest`, `ThemeCatalogTest`, `WeatherErrorsTest`, `WeatherMapperTest`, `WeatherSnapshotTest`, `WmoHelperTest`, `OpenMeteoSerializationTest`, `MainScreenViewModelTest`.

**Доказательства:** `shots/anr_trace_run2.txt`, `shots/logcat_run2.txt`, `shots/gfxinfo_run2.txt`, `shots/r2_00_anr_dialog.png`, `shots/r2_04_clouds_cleared.png`, `_history/reports/phase-4/device-qa.md`.

**Не проверял (вне роли/доступа):** реальный запуск на устройстве, adb, сборку APK, визуальное соответствие refs, скриншоты.

---

## 10. Оговорки по доказательствам (для judge)

1. Трасса ANR снята на сборке, у которой `MainScreen.kt` имел **2148** строк (в HEAD — **2146**): `anr_trace_run2.txt:143` (`MainScreenKt.MainScreen(MainScreen.kt:2148)`). Строки `MainScreenViewModel.kt:23` и `NetworkMonitor.kt:18` совпадают с HEAD, поэтому вывод по существу не меняется, но формально APK собран из дерева, отличающегося от HEAD на ~2 строки. После фикса — пересобрать, переустановить и снять **новую** трассу (`pm clear` → `am start` → 5+ с).
2. `shots/gfxinfo_run2.txt` и `logcat_run2.txt` — на **debug**-сборке (R18 не закрывают).
3. `_history/reports/phase-4/feature-qa.md` относится к APK 02:09, в котором не было ни `WeatherMapScreen`, ни `data/deeplink`, ни текущего ViewModel — его PASS/FAIL по R12–R16/R19/R21 к HEAD неприменим (это видно и из шапки отчёта).
4. Отчёт писался при незакоммиченном WIP (см. §7): перед PASS любые выводы §1 должны быть подтверждены заново.

---

## 11. Итог

| Severity | Кол-во | Ключевые |
|---|---|---|
| blocker | 2 | B1 (ANR: граф ViewModel на main), B2 (13 dex / 34 МБ иконок / R8 off) |
| major | 9 | M1 рулетка обходит лутбоксы, M2 выдуманные humidity/wind, M3 40 с до «нет данных» + трафик в критическом пути, M4 отмена не отменяет HTTP, M5 osmdroid на main, M6 Coil на main, M7 рекомпозиции/unstable, M8 нет тестов ViewModel/Repository, M9 тяжёлый CloudVeil |
| minor | ~25 | мёртвый код, ложный offline, ключ кэша по имени, сегодняшний min/max, фейковая камера, размеры файлов, дубли строк |

**Вердикт: FAIL.** Для PASS нужно: (1) закоммитить и перепроверить на устройстве ANR-фикс + отдельно закрыть B2; (2) починить M1–M4 и M7; (3) добавить тесты на загрузку/кэш/ViewModel (M8); (4) перемерить R18 на `benchmark`.
