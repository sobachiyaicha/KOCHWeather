# code-reviewer — финальное ревью Phase 4 (MET Norway / Open-Meteo / 7timer, ANR, R16–R21)

- **Роль:** `code-reviewer` (независимый критик кода). Код не правился, APK не собирался, adb не использовался.
- **Дата:** 2026-11-14, 08:12–08:45 MSK.
- **Границы:** `git diff a16197a..HEAD --stat`, HEAD на момент старта — `e55bccb`, на момент отчёта — **`957d845`** (`docs(shots): add phase 4 run 6 control evidence`). Между `b8c7290` и `957d845` изменений в `app/src` нет (`git diff b8c7290..HEAD -- app/src` пуст), поэтому весь код-ревью относится к `b8c7290`; `957d845` — только docs/shots.
- **Рабочее дерево:** чистых tracked-изменений нет. В `??` — незакоммиченные `shots/fqf_*.png`/`.pb` от feature-qa (run «fqf»), это не код. На старте ревью в дереве были те же UI-правки, которые затем вошли в `b8c7290`; тесты и компиляция ниже относятся к этому коду (Gradle после коммита дал `UP-TO-DATE`).
- **Объём диффа:** 372 файла, `+55371 / −5057`; из них код `app/src` — 120 файлов, `+13359 / −5057`; остальное — `shots/`, `_history/`, docs. Кодовые изменения фазы: слой провайдеров/репозиториев/кэша, ViewModel, экраны «Карта/Список/Поиск/Вид», deep links, фон-фото, R8/baseline profile.
- **Доказательства run 6:** `shots/r6_01_marker_bubble.png`, `r6_02_main_blue_contrast.png`, `r6_03..04`, `shots/logcat_run6.txt`, `shots/build_run6_benchmark.txt` — сняты после `b8c7290`, ANR нет, старт 538 ms.

## ВЕРДИКТ: **FAIL**

Причина: **2 major** (единицы ветра на резервном Open-Meteo; дедлайн 30 с «съедает» резервный 7timer) при отсутствии блокеров. ANR-фикс, гонка MET/Open-Meteo, отмена OkHttp, backoff, кэш R19, deep links, benchmark/R8 и тесты — в порядке; после исправления двух major вердикт может быть PASS.

---

## 0. Блокеры

**Блокеров нет.** ANR холодного старта (blocker прошлого ревью) устранён структурно и подтверждён run 6: `Displayed +538 ms`, `FATAL/ANR` в `shots/logcat_run6.txt` отсутствуют.

---

## 1. MAJOR (severity = major)

### M1. Open-Meteo отдаёт ветер в км/ч, UI показывает м/с (ошибка ×3.6) — R2/R21

- `app/src/main/kotlin/com/koch/weather/data/api/OpenMeteoClient.kt:36-40` — в URL нет `&wind_speed_unit=ms`; Open-Meteo по умолчанию отдаёт `wind_speed_10m` в **km/h** (`current_units.wind_speed_10m: "km/h"`; в документации default — km/h).
- `app/src/main/kotlin/com/koch/weather/data/mapper/WeatherMapper.kt:62,76,103,140` — `windSpeed10m` кладётся в `WeatherDomainData.windSpeed`/`HourlyItem.wind` без пересчёта.
- `app/src/main/kotlin/com/koch/weather/ui/main/MainWeatherCards.kt:110,130,349-350` — UI подписывает «Скорость ветра (м/с)» и рисует `"$it м/с"`; пороги `5/10/15` (`:139-142`) — метровые.
- Следствие: на резервном Open-Meteo (MET недоступен/медленнее) и на старом кэше Open-Meteo ветер завышен в 3.6 раза, «Штормовое предупреждение» вместо «Умеренный ветер». Это данные, которые видит пользователь (R2), и это не «нет данных», а неверная единица.
- Доказательство пути: `shots/logcat_run4b.txt` (`07:35:53`) содержит фактический запрос `...&current=...wind_speed_10m...&hourly=...` без unit-параметра.
- Тесты не ловят: `app/src/test/kotlin/com/koch/weather/data/WeatherMapperTest.kt:34,41` подаёт `22.0` и проверяет только значение/nullable, не единицу.
- **Fix:** добавить `&wind_speed_unit=ms` в оба URL Open-Meteo (`current` и `hourly` — параметр действует на оба) **или** делить на 3.6 в `OpenMeteoProvider`/маппере; добавить unit-тест: km/h-ответ → м/с в домене. MET (`MetNoProvider`) и 7timer (`SevenTimerSymbolMapper.beaufortToMetersPerSecond`) уже в м/с — приводить к одной единице обязательно.

### M2. 7timer-fallback не запускается, когда основные провайдеры «висят» до 30 с

- `app/src/main/kotlin/com/koch/weather/data/provider/WeatherProvider.kt:73-76` — общий `withTimeoutOrNull(30_000)` оборачивает `fetchWithFallback`; fallback стоит **внутри** этого блока.
- `:78-112` — `fetchWithFallback` сначала ждёт `race(...)`; `race` (`:114-163`) получает результаты только после того, как каждый провайдер **сам** отработает свой retry.
- `MetNoProvider.kt:61-67,134-135` — при TIMEOUT первый клиент (call 12 с) и сразу второй клиент (call 35 с); `OpenMeteoProvider.kt:24-35` — то же (12 с + 35 с).
- Сценарий: оба основных провайдера дают read-timeout на первой попытке (12 с) → оба уходят в retry → на 30-й секунде `withTimeoutOrNull` отменяет `race` и возвращает `timeoutFailure()`; `fetchWithFallback` не доходит до цикла 7timer. Ровно та ситуация, ради которой 7timer добавлен (RU-сеть, MET/Open-Meteo не отвечают), а он не пробуется.
- `app/src/test/kotlin/com/koch/weather/data/ProviderChainTest.kt:230-243` проверяет timeout **без** fallback; теста «primary висит → fallback обязан запуститься» нет.
- **Fix:** зарезервировать бюджет: гонку основных провайдеров ограничить `withTimeoutOrNull` на 18–20 с, затем 7timer в остатке; либо убрать внутренние retry из провайдеров при вызове из chain (retry — на уровне репозитория); либо запускать 7timer третьим участником после, например, 12 с. Добавить тест: два `delay(20_000)`-провайдера + fallback → `Success(SEVEN_TIMER)`.

---

## 2. MINOR (не блокируют сами по себе, перечислены для исправления)

> Все находки §2 — **severity = minor**, кроме явно помеченных; major только M1/M2 из §1.

### 2.1. ANR / main thread

| file:line | Проблема | Fix |
|---|---|---|
| `ui/design/KochBackground.kt:43`, `ui/components/WeatherImage.kt:88` | Coil `ImageLoader`+`OkHttpClient` создаётся в первой композиции на main (не сеть/диск, но десятки мс на первом кадре). | Инициализировать app-scoped loader в `Application`/IO до первого кадра (или `SingletonImageLoader.setSafe`). |
| `ui/main/MainScreen.kt:74-75` | `SimpleDateFormat` создаётся на каждый вызов из композиции. | `remember { SimpleDateFormat(...) }` или `DateTimeFormatter`-кэш. |
| `data/model/WeatherModels.kt:277,312` + `data/model/TaxiModels.kt:123` | Дефолты `MainScreenUiState` строят `ThemeCatalog.build(7)` и `DefaultTaxiDestinations.items[0]` в конструкторе VM на main. Микро-работа (enum/7 объектов), не ANR, но формально «работа в конструкторе». | Оставить (дешево) либо перенести в `init`-IO; тест конструктора уже есть. |
| `ui/components/CloudVeilOverlay.kt:139-169` | 5 `Brush` (1 linear + 4 radial) аллоцируются на каждый draw; `drawWithCache`/`remember` не используется. | `drawWithCache` с кэшем кистей; снимок `eraseMarks` раз в кадр. |
| `ui/design/KochBackground.kt:68-84` | Скрим-градиент создаётся в композиции при каждом recompose. | `remember(colors)` для `Brush`. |

ANR-регрессов нет: в `MainScreenViewModel.kt:29-50` все тяжёлые зависимости `by lazy`; `init` (`:81-218`) не читает DataStore/сеть на main, все обращения — в `viewModelScope.launch(Dispatchers.IO)`; `runBlocking`/`GlobalScope`/`.first()` в конструкторе отсутствуют (`.first()` есть только внутри `WeatherCache` под `withContext(IO)`); `ProcessLifecycleOwner` — без диска.

### 2.2. Корутины / провайдеры

| file:line | Проблема | Fix |
|---|---|---|
| `ui/main/MainScreenViewModel.kt:356-369` | `onAppBackground` отменяет `retryJob/loadJob/trafficJob`, но не `searchJob`, `taxiJob`, `foodJob`: поиск может продолжать сеть до 12 с в фоне. | Отменять `searchJob` (и симуляции) в фоне; `preloadCachedWeather` (`:390-411`) сделать отслеживаемым job. |
| `ui/main/MainScreenViewModel.kt:417` | `isConnected` снимается один раз до 30-секундного запроса; при смене сети классификация ошибки/retry может быть устаревшей. | Перечитывать `isCurrentlyConnected()` после результата или доверять `NetworkMonitor`-состоянию. |
| `data/repository/TrafficRepository.kt:120-140` | Блокирующий `HttpURLConnection` без `disconnect()`, не отменяется `trafficJob.cancel()`, держит IO до 4 с. | OkHttp + `executeAsync` (как у погоды) либо `disconnect()` в `finally`. |
| `data/cache/WeatherCache.kt:60-66` и аналогичные `catch (_: Exception)` (`:80-87,89-95,98-108,110-116,118-124,126-142,148-161,163-170,172-178,180-187,189-195`) | Ловится и проглатывается `CancellationException` (это `Exception`); отмена DataStore-операций не пробрасывается. | `catch (e: CancellationException) { throw e }`, затем `Exception`. |
| `data/network/NetworkMonitor.kt:25-28` | `onLost` любой сети = «нет сети»: при Wi-Fi+LTE потеря одной сети даёт ложный offline/баннер. | `registerDefaultNetworkCallback` или учёт набора сетей; `onCapabilitiesChanged` с `VALIDATED`. |
| `data/network/NetworkMonitor.kt:58-65` | Проверяется `NET_CAPABILITY_INTERNET`, а не `VALIDATED`; captive portal считается онлайн. | Проверять `VALIDATED` (с fallback на INTERNET для старых API). |
| `data/repository/WeatherRepository.kt:96-102` | `getSavedCitiesWeather` делает `dataStore.data.first()` на каждый город; вызывается после каждой загрузки/ретрая. | Один `first()` и разбор всех городов. |
| `data/repository/WeatherRepository.kt:186-230` | MET-кэш провайдера (`fromProviderCache`, `:197-201,219-226`) возвращается как `WeatherLoadResult.Live` (`:224`), т.е. R19-кэш помечен LIVE (в футере есть note «из кэша провайдера»). Формально кэш, по R19 должен быть CACHED. | Возвращать `Cached` с `reason=CACHE_POLICY` и note, либо явно задокументировать, что это «live в пределах политики MET». |
| `ui/main/MainScreenViewModel.kt:340-353` | На `ON_START` нет обновления при `weatherState==LIVE`: после многочасового фона показывается старый прогноз как LIVE; ручного refresh на LIVE нет (баннер-кнопка есть только у CACHED/UNAVAILABLE, `MainScreen.kt:481-537`). Скрытый обход — тап по выбранной точке города (`MainHeader.kt:162-174`). | На foreground: если `now - lastUpdated > minRefreshIntervalMs` — `fetchWeatherQuietly(forceRefresh=true)`; либо pull-to-refresh. |
| `ui/main/MainScreenViewModel.kt:375-386` | Авто-retry в foreground не ограничен числом попыток: после 60 с он повторяется бесконечно, пока приложение видимо. В фоне не спамит (это главное), но батарею/сеть в foreground тратит. | Добавить cap (например, 10 попыток) или экспоненту до 5 минут. |

### 2.3. Compose

| file:line | Проблема | Fix |
|---|---|---|
| `ui/main/MainWeatherCards.kt:213-217` | «В среднем сегодня: 3.5 ★» захардкожено; `MainScreenUiState.rating` обновляется `rateWeather` (`MainScreenViewModel.kt:608-623`), но не отображается. R3: средняя оценка не меняется после голоса. | Печатать `uiState.rating` (формат `%.1f`) вместо константы; либо убрать `rating` из state. |
| `ui/components/FlightBookingDialog.kt:147` | `items(seats)` без `key`. | `key = { it }` (места уникальны). |
| `ui/components/CityListScreen.kt:80-87` | Ветка `isLoading && cities.isEmpty()` недостижима: `isLoadingSavedCities` нигде не ставится в `true` (`MainScreenViewModel.kt:206` только `false`). | Либо выставлять флаг при `refreshSavedCities`, либо убрать ветку/флаг. |
| `ui/main/MainScreen.kt:112-160` | `MainScreenContent` рендерится только для HOME, но `ProvideKochColors` дублируется на `MainScreen` (`:109`) и внутри `MainScreenContent` (`:383`); лишний слой CompositionLocal. | Оставить один провайдер (на уровне `MainScreen`). |
| `ui/main/MainScreen.kt` (нет `BackHandler`), `ui/components/WeatherMapScreen.kt:81` | `BackHandler` есть только у карты. На «Список/Поиск/Вид» системный back завершает Activity, а не возвращает на главную. | Добавить `BackHandler(onBack = onBack)` в `CityListScreen`, `CitySearchScreen`, `ThemeGalleryScreen`. |
| `ui/components/TaxiBottomSheet.kt:158`, `ui/components/ThemeGalleryScreen.kt:185,214` | `TaxiTariffType.values().toList()`/`themePreviewBrush(...)` создают объекты/кисти в композиции на каждый recompose. | `remember { ... }`/кэш по теме. |
| `ui/main/MainScreen.kt:681-684` | Высота нижнего бара для вуали захардкожена (`88.dp`); при смене высоты бара/инсетов табы снова может накрыть. | Использовать реальную высоту бара (передать из Scaffold/`KochSize`). |

Положительно: `@Immutable` на `MainScreenUiState`/`WeatherDomainData`/`KochColors` и моделях; палитры кэшируются (`ui/theme/Theme.kt:184-235`); `CloudVeilOverlay` — offscreen только на 24 % высоты и один `drawPath` с Clear; `WeatherMapScreen` `Configuration.load` вынесен в IO; `AsyncImage` без crossfade (`WeatherImage.kt:55`, `KochBackground.kt:55`); ключи списков в основном стабильные.

### 2.4. Честность данных и R19/R21

| file:line | Проблема | Fix |
|---|---|---|
| `data/provider/MetNoForecastParser.kt:127-141` | Суточные min/max для «Сегодня» считаются только по точкам от текущего часа до конца дня; утренний минимум теряется, днём min завышен. Метка «Сегодня» без оговорки. | Использовать `next_6_hours/next_12_hours.details` (min/max) или показывать min/max как «остаток дня». |
| `data/provider/MetNoForecastParser.kt:100` + `:59-66` | День/ночь захардкожены `6..21`, а дата/час группируются в **часовом поясе устройства**, а не города; для Дубая/другого TZ иконки/дни сдвигаются. | Передавать TZ города (или использовать `is_day` источника, где он есть); день/ночь по восходу/закату. |
| `data/provider/MetNoForecastParser.kt:114-117` | Если у часа нет `symbol_code`, подставляется `currentWmo` — выдуманная погода для этого часа. | `weatherCode` nullable/флаг `codeKnown`, UI «нет данных». |
| `data/mapper/WeatherMapper.kt:120,132,139` | `pop ?: 0` (спасает `popKnown`), но `weatherCode.getOrNull(i) ?: currentCode` — выдуманный код погоды для часа/дня. | Аналогичный `codeKnown`/«—». |
| `data/model/WmoHelper.kt:27` | Неизвестный WMO-код → «Облачно с прояснениями» вместо «нет данных». | `null`/«Нет данных» для неизвестных кодов. |
| `ui/main/WeatherUiReducer.kt:34-48` | `Cached` всегда ставит `isOffline = true`, даже если кэш отдан по политике при живой сети (`isConnected=true`); `isOffline` почти не читается, но семантика ложная. | Разделить `isFromCache` и `isNetworkOffline`; `isOffline` брать из `NetworkMonitor`. |
| `data/model/WeatherModels.kt:138` | `dataSourceLabel` пишется везде (`WeatherRepository.kt:197`, `WeatherMapper.kt:86`), но ни разу не читается UI — мёртвое поле. | Удалить или показывать в футере. |
| `data/repository/LootboxRepository.kt:50` | Текст награды «персональный реальный прогноз прямо со спутника **Open-Meteo**», хотя источник может быть MET Norway/7timer. | Брать подпись из `WeatherSource`/`weatherSourceNote`. |
| `ui/main/MainHeader.kt:151-160` | Иконка «Текущая геопозиция» (contentDescription) просто выбирает город №0 (СПб); геолокации/разрешения нет. | Либо честно назвать «Санкт-Петербург», либо реализовать геопозицию. |
| `ui/components/GrassLockDialog.kt:58-69` | «Открыть камеру» не открывает камеру: три `delay` и «Трава подтверждена!»; `CAMERA` в манифесте (`AndroidManifest.xml:7`) не используется. | Открывать `ACTION_IMAGE_CAPTURE`/photo picker либо переименовать в симуляцию и убрать permission. |
| `ui/components/PaywallDialog.kt:234-238` | В release-UI видна кнопка «Сбросить подписку (для теста рекламы)». | `if (BuildConfig.DEBUG)` или убрать. |
| `ui/components/PaywallDialog.kt:169-172` | Пейволл обещает «разблокировку всех тем», «мгновенный разгон облаков», «иммунитет к блокировке», но `confirmPremiumPurchase` (`MainScreenViewModel.kt:665-677`) меняет только `isPremiumPurchased/showAds`; темы/облака/трава от premium не зависят. | Либо реализовать эффекты в VM, либо убрать неверные пункты. |
| `ui/components/FoodBottomSheet.kt:206-208` | «Выбранные блюда продолжим оформлять в Яндекс Еде», но состав корзины в deeplink не передаётся (в Яндекс Еду уходит только `eda.yandex://open`). | Честнее: «Откроем Яндекс Еду — соберите заказ там», либо реализовать передачу. |
| `data/provider/SevenTimerForecastParser.kt:85,104-117,119` | Current = ближайшая точка (может быть в будущем); 24 часовых строки повторяют 3-часовые значения; daily может включить день init. Для fallback допустимо, но это синтетическая почасовка, а подпись источника лишь «7timer». | Показывать шаг 3 ч либо note «7timer: шаг 3 ч»; current брать только `<= now`. |

### 2.5. Гигиена / мёртвый код / документация

| file:line | Проблема | Fix |
|---|---|---|
| `app/src/main/res/xml/data_extraction_rules.xml:8` | Единственный `TODO` в `app/src` (шаблон backup) — при этом файл нигде не подключён в манифесте (нет `android:dataExtractionRules`), как и `backup_rules.xml`. | Удалить неиспользуемые xml или подключить и убрать TODO. |
| `data/repository/WeatherRepository.kt:77-78` | `getCachedWeatherFast` не используется. | Удалить. |
| `ui/main/MainScreenViewModel.kt:284-311` | `loadWeather` не используется. | Удалить. |
| `ui/main/MainScreenViewModel.kt:538-544,590-596,629-631,1062-1064` | `toggleHourlyExpanded`, `setHourlyExpanded`, `toggleTheme`, `setRating`, `setCongratsModal` не вызываются; `isHourlyExpanded` (`WeatherModels.kt:292`) не читается UI. | Удалить состояния/методы. |
| `ui/main/MainScreenViewModel.kt:763-783,866-893` | `orderTaxi`/`orderFood` (симуляции заказа) недостижимы из UI; `cancelTaxi`/`cancelFoodOrder` отменяют состояния, которые никто не создаёт. | Удалить симуляции или вернуть кнопку (но R16 уже через deep links). |
| `ui/main/MainScreenViewModel.kt:679-683` | `@Deprecated purchasePremium()` не вызывается. | Удалить. |
| `data/repository/LootboxRepository.kt:12,91,108-111` | `history` — `mutableListOf`, мутируется из IO, `getHistory()` отдаёт копию; `getThemeCatalog`/`getHistory` не используются. | Убрать `history`/методы или синхронизировать. |
| `data/cache/WeatherCache.kt:189-195` | `getLastUpdatedTime` не используется. | Удалить. |
| `data/model/EmojiTextGuard.kt:19` | `findAll` не используется. | Удалить. |
| `data/model/WeatherModels.kt:245,255,328,331-332` | `currentCityIndex`, `isWeatherUnavailable` (пишется, не читается), `isCongratsModalOpen`, `citySummaries`, `isLoadingCitiesWeather` — мёртвое состояние. | Удалить, либо подключить к UI. |
| `NavigationKeys.kt:3-5` | `sealed interface Screen` не используется. | Удалить. |
| `data/repository/TrafficRepository.kt:23` + `app/build.gradle.kts:29` | `apiKey`/`BuildConfig.TRAFFIC_API_KEY` не используются (URL региональный ключ не требует). | Удалить поле и чтение `local.properties`. |
| `data/mapper/WeatherErrors.kt:35-50` | `InterruptedIOException` (call timeout OkHttp) не маппится в TIMEOUT, попадает в общий IOException → «Ошибка сети». | Добавить `is InterruptedIOException` → TIMEOUT. |
| `data/mapper/WeatherErrors.kt:15` | Комментарий «Open-Meteo — единственный источник прогноза» устарел после MET Norway/7timer. | Обновить. |
| `data/provider/MetNoProvider.kt:152-153` | Комментарий про «20-секундный предохранитель», фактический бюджет `WeatherProviderChain` 30 с. | Обновить. |
| `ui/components/CloudVeilOverlay.kt:51` vs `:85` | Комментарий «45 % высоты», код `fillMaxHeight(0.24f)`. | Исправить комментарий. |
| `ui/design/KochBackground.kt:32` vs `:69-70` | Комментарий «20–35 %», фактический скрим 40/30/48 %. | Исправить комментарий. |
| `app/src/main/AndroidManifest.xml:27` | `android:windowSoftInputMode="adjustResize"` стоит на `<application>` — невалидный для application атрибут, игнорируется; поле поиска (`CitySearchScreen`) не гарантирует resize под IME. | Перенести на `<activity>` и/или `imePadding()` в Compose. |
| `ui/components/WeatherMapScreen.kt:122-137` | `DisposableEffect` вызывает `onResume` только на будущее событие; если lifecycle уже RESUMED — `MapView.onResume()` не вызывается (тайлы всё же грузятся, run 6 подтвердил). Нет `onPause` перед `onDetach`. | Вызвать `view.onResume()` при создании, если `currentState >= RESUMED`; `onPause` в onDispose. |
| `ui/main/MainScreenViewModel.kt` (965 строк) | God-ViewModel: 10 lazy-репозиториев + 8 job-хендлов + вся погода/премиум/еда/такси/самолёты/поиск/темы. Сложно тестировать. | Вынести сервисы в отдельные координаторы (WeatherLoadCoordinator, SearchCoordinator, ...). |
| `data/cache/WeatherCache.kt:47-54` + `ui/main/MainScreenViewModel.kt:1017-1049` | Кэш и дедупликация городов по `name` (нижний регистр): два одноимённых города в разных странах перезапишут прогноз друг друга. | Ключ `name|lat|lon`. |
| `ui/main/MainScreenViewModel.kt:100-101` | При уходе в offline `dataSourceLabel` обнуляется, а `weatherAttributionRu` остаётся «Данные: MET Norway (api.met.no)» до следующей загрузки — баннер «Сохранённый прогноз» и футер расходятся. | В ветке offline ставить `cachedAttribution(state.weatherSource)`. |
| `ui/main/MainScreenViewModel.kt:169-171` | Если `lastSavedCity` отсутствует в `initialCities`, `initialIndex` принудительно 0 → карусель/точки указывают на другой город. | `indexOfFirst` без `coerceAtLeast(0)` + добавлять город в список при отсутствии. |
| `ui/components/CityListScreen.kt:155-170` | `item.isOffline` у сохранённых городов всегда `true` (данные из кэша по определению) — метка «сохранённый прогноз» не отличает живой прогноз. | Показывать метку только для DataStore-кэша/офлайна. |

---

## 3. Тесты (обязательный прогон)

Команда (из корня):

```
$env:JAVA_HOME="D:\jdk-17\jdk-17.0.12+7"
$env:TEMP=$env:TMP="D:\akcon\weatherAPK-dsh\.local\tmp"
.\gradlew.bat --console=plain testDebugUnitTest
```

Результат: **BUILD SUCCESSFUL** (08:15:58; `:app:compileDebugUnitTestKotlin` и `:app:testDebugUnitTest` executed). Повторный прогон после `b8c7290` — `UP-TO-DATE`, exit code 0; компиляция датирована 08:14:50, т.е. тесты относятся к финальному коду `b8c7290` (в `957d845` изменений кода нет).

- **26 сьютов, 139 тестов, 0 падений, 0 ошибок, 0 пропусков** (`app/build/test-results/testDebugUnitTest/*.xml`).
- Ненулевой exit code первого запуска в PowerShell — артефакт обёртки `Tee-Object`/stderr warning «SDK XML version 4», не падение Gradle/тестов.
- Сьюты: `WeatherMapperTest` 14, `MainScreenViewModelTest` 15, `ProviderChainTest` 10, `WeatherErrorsTest` 9, `DeepLinksTest` 9, `GeocodingChainTest` 7, `ImageCatalogTest` 6, `WeatherRetryPolicyTest` 6, `MetNoForecastParserTest` 5, `ThemeCatalogTest` 5, `WeatherLoadPolicyTest` 5, `EmojiFreeTextTest` 4, `NominatimGeocodingParserTest` 4, `SevenTimerForecastParserTest` 4, `TaxiDestinationTest` 4, `WeatherTimeoutConfigTest` 4, `UiPersistenceContractTest` 4, `MetNoCachePolicyTest` 3, `MetNoSymbolMapperTest` 3, `PhotonGeocodingParserTest` 3, `SavedCityWeatherTest` 3, `SevenTimerSymbolMapperTest` 3, `TaxiRepositoryTest` 3, `WmoHelperTest` 3, `OpenMeteoSerializationTest` 2, `WeatherSnapshotTest` 1.
- Пробелы покрытия (не падения): нет unit-теста на единицы ветра Open-Meteo (M1) и на «primary висит → 7timer» (M2); `MainScreenViewModel` не инстанцируется (AndroidViewModel), проверяется только форма конструктора (`MainScreenViewModelTest.kt:69-89`).

---

## 4. Benchmark / R8 / release (R18)

- `app/build.gradle.kts:44-58` — `benchmark` корректен: `initWith(release)`, `isDebuggable=false`, `isMinifyEnabled=true`, `isShrinkResources=true`, `isProfileable=true`, debug-подпись, тот же `applicationId` (install -r поверх debug), `versionNameSuffix`.
- `release` не сломан: `isMinifyEnabled=false` остался как был, `benchmark` его не меняет; `assembleBenchmark` зелёный (`shots/build_run6_benchmark.txt`, APK 3 122 864 Б), debug тоже (`shots/build_run5_debug.txt`).
- ProGuard: `app/proguard-rules.pro:5-33` — сохранены `MainActivity`, сериализаторы/Companion, `data.model.**`, enum `values/valueOf`, osmdroid. Проверка по `app/build/outputs/mapping/benchmark/mapping.txt`: `PhotonResponse`, `NominatimPlace`, `MetNoResponse`, `SevenTimerResponse` и их `$$serializer` присутствуют в APK (не удалены R8), `WeatherDomainData$$serializer` на месте.
- Baseline profile: `app/src/main/baseline-prof.txt` (26 правил, HSPL) собирается (`mergeBenchmarkArtProfile`/`compileBenchmarkArtProfile`), в APK `assets/dexopt/baseline.prof`; `androidx.profileinstaller` приходит транзитивно (1.4.0), `ProfileInstallerReceiver/Initializer` есть в merged manifest, `profileable` в APK. Замечание: на Android 11 (G8342) профиль ставит именно ProfileInstaller, он есть; на 12+ — платформа.
- `assembleRelease` я не запускал (роль code-reviewer не собирает APK); по конфигурации release идентичен старому, риска от `benchmark` нет.

---

## 5. R16 (deep links) — PASS по коду

- `data/deeplink/DeepLinks.kt:63-123` — корректные app/web URI (yandex go: `start-lat/lon`, `end-lat/lon`, `tariffClass`, `ref`; maps; eda), координаты без Locale-запятой/экспоненты (`:143-145`), валидация диапазонов (`:131-141`).
- `data/deeplink/DeepLinkAndroid.kt:22-27` — `resolveActivity` по кандидатам app→web; `DeepLinkLauncher.kt:19-30` — `FLAG_ACTIVITY_NEW_TASK`, `ActivityNotFoundException`/`SecurityException` → `false`; UI показывает ошибку (`TaxiBottomSheet.kt:217-222`, `FoodBottomSheet.kt:212-215`), а не «заказ создан».
- `AndroidManifest.xml:11-19` — `<queries>` с пакетами Яндекс Go/Карт/Еды и https-VIEW — resolveActivity на Android 11+ работает.
- Ограничение: нативные схемы на устройстве без приложений не проверяются; feature-qa подтвердил веб-фолбэк (`fq3_22`, `fq3_23`, `fq3_29`, `fq3_30`). Веб-URL `https://taxi.yandex.ru/route?...` стоит перепроверить на реальную страницу маршрута (сейчас доказан только переход в Chrome).

---

## 6. Security / гигиена (R21)

- Секретов нет: `local.properties` в `.gitignore` и не tracked; `TRAFFIC_API_KEY` пустой и не используется; ключей/токенов в `app/src` нет.
- `TODO/FIXME/HACK` в Kotlin и gradle: **0**; единственный реальный TODO — шаблон `data_extraction_rules.xml:8` (файл не подключён).
- Эмодзи: **0** — проверено Python-регэкспом по диапазонам `1F000–1FAFF`, `2600–2604`, `2607–27BF`, `2B00–2BFF`, `FE0F`, `200D`, `1F3FB–1F3FF` по 130 файлам `app/src` (kt/xml/kts/pro/txt). Разрешённая типографика (° ★ ◀ ▶ →) не ловится.
- Заглушки/симуляции, которые видит пользователь: камера травы (`GrassLockDialog.kt:58-69`), «текущая геопозиция» (`MainHeader.kt:151-160`), сброс premium «для теста» (`PaywallDialog.kt:234-238`), симуляции заказа такси/еды в VM — перечислены в minor; в данные погоды фейки не подставляются (nullable + popKnown + UNAVAILABLE).
- Размеры: `MainScreenViewModel.kt` — 965 строк, `MainScreen.kt` — 663, `MainWeatherCards.kt` — 488; после разбиения 2146-строчного `MainScreen` это уже приемлемо, но VM остаётся god-object (minor).

---

## 7. Итог по пунктам задания

| Пункт | Статус | Кратко |
|---|---|---|
| 1. ANR-фикс | **PASS** | Тонкий конструктор, `by lazy`+IO, нет main-диска/сети/`runBlocking`; run 6: 538 ms, ANR нет. Микро: Coil-загрузчик и дефолты state на main. |
| 2. Корутины/провайдеры | **FAIL (M2)** | Гонка/Channel/отмена/Call.cancel/backoff/ProcessLifecycle — корректны; но 7timer не запускается при таймаутах основных провайдеров; плюс minor (поиск в фоне, traffic без отмены, кэш LIVE). |
| 3. Compose | **PASS с minor** | `@Immutable`, кэш палитры, CloudVeil 24 %/один path, MapView-config в IO, AsyncImage без crossfade; minor: рейтинг 3.5, SimpleDateFormat, BackHandler, brushes. |
| 4. Честность данных | **FAIL (M1)** | Единицы ветра Open-Meteo; кэш→CACHED (кроме provider cache→LIVE), nullable/popKnown/«нет данных»/ошибки со временем в основном есть. |
| 5. R16 | **PASS** | resolveIntent app→web, queries, честные ошибки; нативные схемы не проверены без приложений. |
| 6. Безопасность/гигиена | **PASS с minor** | Секретов/эмодзи/заглушек в Kotlin нет; один TODO в неиспользуемом xml; мёртвый код и симуляции — minor. |
| 7. Тесты | **PASS** | 26 сьютов / 139 тестов / 0 падений; пробелы: единицы ветра, primary-timeout→7timer, ViewModel-оркестрация. |
| 8. Benchmark/R8 | **PASS** | R8+shrink+profileable, профайл в APK/profileinstaller, release не изменён, serializers сохранены. |

**Severity:** blocker 0, major 2 (M1, M2), minor ~30.

---

## 8. Проверенные файлы

**Data/provider/api:** `WeatherProvider.kt`, `MetNoProvider.kt`, `MetNoForecastParser.kt`, `MetNoModels.kt`, `MetNoCachePolicy.kt`, `MetNoSymbolMapper.kt`, `OpenMeteoProvider.kt`, `OpenMeteoClient.kt`, `OpenMeteoExceptions.kt`, `OkHttpCall.kt`, `SevenTimerProvider.kt`, `SevenTimerForecastParser.kt`, `SevenTimerModels.kt`, `SevenTimerSymbolMapper.kt`, `GeocodingProvider.kt`, `PhotonGeocodingProvider.kt`, `NominatimGeocodingProvider.kt`, `OpenMeteoGeocodingProvider.kt`.

**Data/repository/cache/model/mapper/deeplink/network:** `WeatherRepository.kt`, `WeatherLoadPolicy.kt`, `WeatherCache.kt`, `TrafficRepository.kt`, `SubscriptionRepository.kt`, `LootboxRepository.kt`, `FoodRepository.kt`, `FlightRepository.kt`, `TaxiRepository.kt`, `AiFactsRepository.kt`, `ImageRepository.kt`, `WeatherMapper.kt`, `WeatherErrors.kt`, `WeatherModels.kt`, `LoadResultModels.kt`, `WeatherRetry.kt`, `SavedCityWeather.kt`, `ThemeModels.kt`, `ImageModels.kt`, `EmojiTextGuard.kt`, `WmoHelper.kt`, `NetworkMonitor.kt`, `DeepLinks.kt`, `DeepLinkAndroid.kt`.

**UI:** `MainScreen.kt`, `MainScreenViewModel.kt`, `WeatherUiReducer.kt`, `MainWeatherCards.kt`, `MainHeader.kt`, `MainCards.kt`, `CloudVeilOverlay.kt`, `WeatherMapScreen.kt`, `WeatherImage.kt`, `CityListScreen.kt`, `CitySearchScreen.kt`, `ThemeGalleryScreen.kt`, `ThemeRouletteDialog.kt`, `PaywallDialog.kt`, `TaxiBottomSheet.kt`, `FoodBottomSheet.kt`, `FlightBookingDialog.kt`, `GrassLockDialog.kt`, `DeepLinkLauncher.kt`, `DataSourceFooter.kt`, `KochBackground.kt`, `Theme.kt`, `KochSheet.kt`, `FpsOverlay.kt`, `Navigation.kt`, `NavigationKeys.kt`, `MainActivity.kt`, `AndroidManifest.xml`.

**Конфиг/сборка:** `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/baseline-prof.txt`, `gradle/libs.versions.toml`, `gradle.properties`, `.gitignore`, `app/src/main/res/xml/data_extraction_rules.xml`.

**Тесты:** `MainScreenViewModelTest`, `UiPersistenceContractTest`, `WeatherLoadPolicyTest`, `ProviderChainTest`, `WeatherRetryPolicyTest`, `WeatherTimeoutConfigTest`, `WeatherMapperTest`, `WeatherSnapshotTest`, `WeatherErrorsTest`, `MetNo*Test`, `SevenTimer*Test`, `GeocodingChainTest`, `PhotonGeocodingParserTest`, `NominatimGeocodingParserTest`, `DeepLinksTest`, `EmojiFreeTextTest`, `ImageCatalogTest`, `ThemeCatalogTest`, `SavedCityWeatherTest`, `Taxi*Test`, `WmoHelperTest`.

**Доказательства:** `shots/build_run6_benchmark.txt`, `shots/logcat_run6.txt`, `shots/r6_01..04`, `shots/logcat_run5.txt`, `shots/perf_run5_scroll.txt`, `shots/build_run5_debug.txt`, `shots/logcat_run4b.txt`, `_history/reports/phase-4/device-qa.md`, `feature-qa-run3.md`.

**Не проверял (вне роли/доступа):** реальный запуск на G8342, adb, сборку/установку APK, визуальное соответствие refs, визуальный контраст, полную матрицу ACCEPTANCE (зона judge).

---

## 9. Оговорки для judge (свежесть/приёмка)

1. **HEAD менялся во время ревью:** `e55bccb` (старт) → `b8c7290` (код-фиксы маркера/контраста, 08:16) → `957d845` (docs, 08:26). Код `957d845` идентичен `b8c7290` (`git diff b8c7290..HEAD -- app/src` пуст). Все выводы относятся к `b8c7290`.
2. **Доказательства run 6 сняты после `b8c7290`** (`r6_01_marker_bubble.png` подтверждает info-window, `r6_02` — контраст, `logcat_run6` — нет ANR, 538 ms): свежесть для последнего кода выполнена. Но `shots/fqf_*.png/.pb` (feature-qa run 6) на 08:39 **не закоммичены** — judge должен потребовать коммит или явно приложить их к доказательствам.
3. **R18:** run 5 `perf_run5_scroll.txt` — 301 кадр, jank 62 (20.60 %), p50 7 ms, p90 25, p95 36. p50 ≤16 ms, но 20.6 % jank — вопрос к `perf-qa`/`judge` по критерию «60 FPS, без системного jank»; code-reviewer это не закрывает.
4. **M2 (7timer)** нельзя закрыть скриншотом: нужен сценарий с принудительными таймаутами MET/Open-Meteo (или unit-тест), иначе «7timer не включался» (device-qa run 5) так и останется непроверенным.
5. **M1 (ветер)** не виден на скриншотах, пока источник MET; для доказательства нужен Open-Meteo-ответ/km-h или unit-тест конверсии.

---

## 10. Итоговая таблица

| Severity | Кол-во | Ключевые |
|---|---|---|
| blocker | 0 | — |
| major | 2 | M1 единицы ветра Open-Meteo (км/ч → «м/с»); M2 7timer starved 30-секундным бюджетом и внутренними retry |
| minor | ~30 | поиск не отменяется в фоне; traffic без отмены; проглатывание CancellationException; ложный offline; provider cache → LIVE; нет refresh на foreground; N DataStore-чтений; today min/max; фейковые камера/геопозиция/сброс premium; рейтинг 3.5; BackHandler; мёртвый код; TODO в xml; неиспользуемый `windowSoftInputMode` и т.п. |

**Вердикт: FAIL** (2 major). Для PASS: (1) добавить `wind_speed_unit=ms`/конверсию и тест; (2) зарезервировать бюджет под 7timer и покрыть тестом «primary timeout → fallback»; остальные minor — по возможности, они не блокируют приёмку по правилу «PASS без blocker/major».
