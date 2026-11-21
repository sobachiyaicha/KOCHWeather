# data-coder — логика и данные, Фаза 5 (items 2, 6, 7, 9, 10, 11, 12, 14)

Дата: 2026-11-14. Ветка/коммит не создавались (коммитит оркестратор).
Окружение: `JAVA_HOME=D:\jdk-17\jdk-17.0.12+7`, `gradle.user.home=D:/.gradle`,
`java.io.tmpdir` → `.local/tmp`, диск C не использовался.

## Итог проверки

```powershell
$env:JAVA_HOME="D:\jdk-17\jdk-17.0.12+7"
cd D:\akcon\weatherAPK-dsh
.\gradlew.bat --console=plain compileDebugKotlin
.\gradlew.bat --console=plain testDebugUnitTest
```

`compileDebugKotlin --rerun-tasks --no-build-cache` (полная пересборка, не «UP-TO-DATE»):

```
> Task :app:generateDebugBuildConfig
> Task :app:generateDebugResources
> Task :app:packageDebugResources
> Task :app:processDebugNavigationResources
> Task :app:parseDebugLocalResources
> Task :app:generateDebugRFile
> Task :app:compileDebugKotlin

BUILD SUCCESSFUL in 44s
7 actionable tasks: 7 executed
```

`testDebugUnitTest` (свежий прогон, `--no-build-cache`, результаты удалены перед запуском):

```
> Task :app:preDebugUnitTestBuild UP-TO-DATE
> Task :app:compileDebugUnitTestKotlin UP-TO-DATE
> Task :app:javaPreCompileDebugUnitTest UP-TO-DATE
> Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:processDebugJavaRes UP-TO-DATE
> Task :app:processDebugUnitTestJavaRes UP-TO-DATE
> Task :app:testDebugUnitTest

BUILD SUCCESSFUL in 12s
26 actionable tasks: 1 executed, 25 up-to-date
```

Счёт по `app/build/test-results/testDebugUnitTest/*.xml`: **33 сьюта, 193 теста,
0 падений, 0 пропусков**. Требование плана «≥160 тестов» выполнено.

Отдельно: один промежуточный прогон упал с `e: Daemon compilation failed ... Kotlin daemon`
(инфраструктурный сбой Kotlin-демона, не ошибка кода). Повторный запуск и затем
принудительная полная пересборка прошли зелёными; ниже — только результаты этих прогонов.

## Что сделано по пунктам

### 11. Валюта и Минск
- `PremiumOffer`: `priceByn/oldPriceByn` → `priceRub = 179`, `oldPriceRub = 899`,
  `priceLabel = "179 ₽"`, `oldPriceLabel = "899 ₽"`, скидка 80 % сохранена.
  Формат рублей — с разрядами через пробел (`formatRub` внутри модели, без зависимости data→ui).
- `UmbrellaItem.priceByn` удалён; витрина берёт `priceRu` (649/990/790/850 ₽).
  Добавлены `DefaultUmbrellas.minPriceRu` / `minPriceLabel` = «649 ₽» (из тех же строк,
  что видит пользователь) — для строки «Зонтики от» и витрины.
- `FlightOffer.priceByn` удалён из модели и из всех 4 офферов. Оффер
  `fl_minsk_msq` заменён на `fl_sochi_aer`: «Россия», `FV-6543`, Санкт-Петербург (LED) →
  Сочи (AER), 09:40 → 12:20, «2 ч 40 мин», `priceRu = 6900`,
  `weatherAtDestination = "Солнечно, +26"`.
- `TaxiRide.priceByn`, `TaxiRepository.calculatePriceByn` удалены.
- `formatByn` удалён из `ui/design/Formatters.kt`, `formatRuAmount` приведён к рублям.
- Дополнительно (иначе не проходил тест-гард «нет BYN в main»): убраны `FoodItem.priceByn`,
  `FoodOrder.totalPriceByn`, `FoodState.totalCartPriceByn`, вторые ценники в меню
  `FoodRepository` и `totalByn` в симуляции заказа. UI еды BYN не использовал.
- `WeatherRepository.defaultCities` = `DefaultCities.cities`: Санкт-Петербург (первый,
  `isCurrentLocation = true`), Москва, Сочи (43.5855, 39.7231, «Россия»,
  «Краснодарский край»), Дубай.
- Миграция сохранённого списка (`getInitialCities`): устаревшая точка старого дефолта
  (53.9006, 27.5590 — определяется по координатам, название/страна в код не переносятся
  из-за тест-гарда) удаляется, актуальные дефолты добавляются на свои позиции,
  гео-город всегда первый, дубликаты схлопываются; список перезаписывается в
  `saved_cities_json`, только если реально изменился.
- Новый тест-гард `RussianContentGuardTest`: рекурсивно читает `app/src/main` (kt/xml) и
  падает на подстроках `BYN`, `Минск`, `Беларус`. Сейчас — 0 совпадений.
- `EmojiFreeTextTest` НЕ ослаблен; удалена одна строка `texts += it.priceByn`,
  без которой файл не компилировался (поле удалено по требованию).

### 7. «ТЕКУЩЕЕ МЕСТО»
- `CityLocation += val isCurrentLocation: Boolean = false` (`@Serializable`, дефолт —
  старый JSON читается без миграции; проверено тестом).
- `MainScreenUiState += isSelectedCityCurrentLocation: Boolean = false`.
- VM (`MainScreenViewModel`) выставляет флаг только из `selectedCity.isCurrentLocation`:
  при старте, `selectCity(index)`, `selectCity(city)`, `loadWeather(city)`, `addCity`.
  Город из поиска приходит без флага и остаётся `false` (`CityCatalog.planAdd` флаг по
  имени не «доизобретает»).
- Восстановление флага у первого дефолта: `WeatherRepository.normalizeCities` заменяет
  гео-город канонической записью из `DefaultCities` (флаг `true`), остальные — `false`.
- Тесты: `CityCatalogTest.searchCity_isNeverCurrentLocation`,
  `geoCity_keepsCurrentLocationFlag`, `UiPersistenceContractTest.selectedCityCurrentLocation_*`
  и JSON-восстановление с дефолтом.

### 10. Осадки
- `HourlyItem += precipitationMm: Double? = null`, `DailyItem += precipitationMm: Double? = null`
  (с дефолтами — старый кэш читается).
- DTO: `HourlyWeatherDto += precipitation: List<Double?>?`,
  `DailyWeatherDto += precipitation_sum: List<Double?>?`.
- `MetNoForecastParser`: `next_1_hours.details.precipitation_amount` → почасовой список;
  сумма за сутки по всем точкам дня → `precipitation_sum` (null, если источник не дал
  ни одного значения).
- `WeatherMapper`: «Сейчас» — `current.precipitation` (fallback на почасовое значение);
  остальные часы — `hourly.precipitation[i]`; дни — `precipitation_sum[i]`. Нет данных →
  `null`, ноль не подставляется.
- Open-Meteo: в запрос добавлены `precipitation` (hourly) и `precipitation_sum` (daily).
  7timer не отдаёт количество осадков — остаётся `null` (не выдумываем).
- Тесты: `MetNoForecastParserTest.precipitationAmounts_reachHourlyAndDailyMm`
  (час с дождём 0.2 мм > 0; сутки 0.2 и 0.7 мм), `missingPrecipitation_keepsNull_notZero`,
  `WeatherMapperTest.precipitationArrays_mapToHourlyAndDailyMm` / `missingPrecipitationArrays_stayNull_notZero`.

### 2. Такси — реальные цены
- `TaxiTariffType`: вместо `basePriceRu/basePriceByn` — `minFareRu, perKmRu, perMinRu`:
  ECONOMY «Эконом» 149/12/12; COMFORT_PLUS «Комфорт+» 205/17/17;
  ULTIMA «Ultima (Business)» 349/30/25 — в KDoc явно помечено, что Ultima/Business —
  оценка по Business (публичного тарифа Ultima в СПб на 14.11.2026 не было).
- Новая чистая математика `data/model/TaxiModels.kt → TaxiPricing`:
  `durationMin = 3 + distanceKm * 2.2`,
  `price = max(minFare, minFare + perKm*max(0, km-1) + perMin*billableMin) * surge`,
  где `billableMin = durationMin - 3`, но **только если поездка выходит за включённый
  километр**. Обоснование отклонения от буквальной записи формулы — ниже, в «Решениях».
  `surge = 1.0 + (trafficScore-3).coerceAtLeast(0) * 0.12`, `null` → 1.0, балл
  ограничен 0..10; итог округляется вверх до рубля.
- `TaxiRepository.calculatePriceRu` делегирует в `TaxiPricing` (одна формула на всех),
  `calculatePriceByn` удалён, симуляция поездки берёт расчётное время маршрута.
- `TaxiState += priceByTariff: Map<TaxiTariffType, Int>`, `durationMinByTariff: Map<...>`
  + удобные `selectedPriceRu` / `selectedDurationMin`. VM пересчитывает их при смене
  точки, тарифа, выбранного города и при обновлении погоды/приходе пробок.
- Тесты: `TaxiRepositoryTest` — 1 км = минимум для всех тарифов; 10 км без пробок
  (эконом 521 ₽, комфорт+ 732 ₽, Ultima 1169 ₽); пробки 9 → surge 1.72 и 897 ₽;
  `null`/0/3 балла → без наценки; время 10 км = 25 мин.

### 12. Совет и факт
- `AiFactsRepository`: 20 фактов (было 9), без эмодзи, сатира без оскорблений.
  `getFactForDay(cityName, dayOfYear)` — детерминированный выбор
  (`seed = city.hashCode() + dayOfYear`, индекс по модулю), `getFactForDay(dayOfYear)`
  — без города. `getNextFact(current)` — случайный из всех, кроме текущего (не по кругу).
  Старые `getDefaultFact()`/`getRandomFact()` сохранены для совместимости.
- VM: начальный факт — `getFactForDay(city, dayOfYear)`; «Другой факт» — `getNextFact`.
- `WmoHelper.getClothingAdvice` больше не единственный текст: вся логика в новом
  `data/model/ClothingAdvisor` — 7 температурных диапазонов × 3 варианта + модификаторы:
  ветер > 8 м/с (3 варианта), дождь (3), снег (3), гроза (3), лёд (3), солнце при ≥ +18°
  (3). Выбор детерминирован от `city.name.hashCode() + dayOfYear` (и сдвигов seed для
  каждого модификатора) — не дёргается при рекомпозиции, меняется по дням и городам.
  `WmoHelper.getClothingAdvice(temp, code)` оставлен тонкой обёрткой для
  `DailyDetailDialog` и старых тестов.
- `WeatherMapper` передаёт в совет температуру, ветер, имя города и день года из
  `lastUpdated`.
- Тесты: `AiFactsRepositoryTest` (≥16, уникальность, детерминизм, соседние дни разные,
  разные города, `getNextFact` никогда не равен текущему) и `ClothingAdvisorTest`
  (10 тестов: детерминизм, 3+ варианта в каждом диапазоне, ветер/дождь/снег/гроза/солнце,
  старый контракт `WmoHelper`).

### 6. Города
- Новый `data/model/CityCatalog`: `isSamePlace` (та же точка в пределах ~2 км ИЛИ то же имя
  в пределах ~5 км), `dedupe`, `planAdd` (существующий город → только выбор; новый →
  список + индекс; при лимите → `null`), `MAX_SAVED_CITIES = 12`.
  Однофамильцы в разных регионах остаются разными городами (тест).
- `MainScreenViewModel.addCity`: план из `CityCatalog`, сохранение `saved_cities_json`,
  новый город сразу выбран (`selectedCity`, индексы), очистка поиска, переход на «Главная»
  и загрузка погоды через `selectCity`. При лимите — честная ошибка в `searchError`,
  список не портится.
- `selectCity(city)` / `loadWeather(city)` ищут город по `CityCatalog.isSamePlace`,
  а не только по имени.
- Тесты: `CityCatalogTest` (7 тестов: поиск → флаг false, дедуп по имени+координатам,
  та же точка под другим именем, однофамильцы регионов, лимит, порядок).

### 14. Лутбоксы — виды погоды по реальному прогнозу
- `LootRewardType.TOMORROW_WEATHER` переименован в `WEATHER_TOMORROW` (дубликат значения
  не заводился). `LootReward += weatherCode: Int?`, `temperatureRangeRu: String?`,
  `dayLabelRu: String = "завтра"`; `weatherForecastTomorrow` сохранён для совместимости.
- `LootboxRepository.openLootbox(tomorrow: DailyItem?)`; VM передаёт
  `weather.daily.getOrNull(1) ?: daily.getOrNull(0)`.
- `data/model/LootboxPrizes.kt → LootboxPrizeFactory` (чистая, тестируемая):
  - iPhone 0.001 % оставлен;
  - вес погодного приза 35 % при осадках в прогнозе (`weatherCode in 51..99`), иначе 20 %;
  - если прогноза нет — погодного отрезка нет вообще, остальные веса (темы/зонт/сапоги/кофе)
    автоматически пересчитываются до 100 %;
  - вид приза — по WMO-коду через `WmoHelper.getIconType`/`getDescriptionRu`
    (отдельная таблица кодов не дублируется): дождь, снег, гроза, туман, солнце,
    облачная погода; текст «Поздравляем! Вам выпал дождь на завтра», глагол согласован;
    подзаголовок «от +10° до +16°. Возьмите зонт.»;
  - если реального «завтра» в прогнозе нет и взят сегодняшний день, текст честно говорит
    «на сегодня», `dayLabelRu = "сегодня"`.
- История: `WeatherCache.saveLootboxHistory(json)` / `getLootboxHistory()`
  (ключ `lootbox_history_json`), `LootboxHistoryCodec` (kotlinx.serialization, до 20,
  битый JSON → пустая история). Репозиторий читает/пишет историю при каждом дропе.
- `LootboxState += lastRewardSeq`, `history` уже была; VM применяет `withDrop(...)`:
  seq +1, totalOpened +1, история обрезается до 20. При старте история грузится из DataStore,
  `totalOpened = history.size`.
- Тесты: `LootboxPrizeFactoryTest` (9): дождевой код → «дождь на завтра»; сухой код → не
  дождь; нет прогноза → погодного приза нет ни на одном roll; веса 35/20/0; iPhone 0.001;
  seq/история; JSON-раундтрип и мусор; кап 20.

### 9. Реклама (данные)
- Новый `data/model/AdModels.kt`: `enum class AdAction { PAYWALL, LOOTBOX, UMBRELLA_SHOP, FOOD }`,
  `AdCreative(id, brandRu, headline, body, ctaLabel, action, imageCategory, disclaimerRu)`,
  каталог `AdCatalog` из 4 креативов:
  1. «НейроШкола / Дизайнеры больше не нужны» → PAYWALL, AD_LAPTOP;
  2. «iPhone 16 Pro Max» (шанс 0.001 %) → LOOTBOX, IPHONE;
  3. «Зонтики от 649 ₽» → UMBRELLA_SHOP, без фото;
  4. «Яндекс Еда: Пицца за 30 минут» → FOOD, FOOD.
  У всех — `disclaimerRu`, начинается с «Реклама».
- Новый `data/repository/AdRepository.kt`; ротация `creatives[(dayOfYear + openCount) % size]`
  без Random.
- `MainScreenUiState += adCreative: AdCreative` (дефолт — первый креатив каталога),
  `adImageUrl: String?`. VM: при старте считает креатив дня и URL из `imageUrls`;
  `openAdPromo()` увеличивает счётчик открытий и обновляет креатив/URL.
  `MainScreen` вызывает `viewModel.openAdPromo()` (единственная строка UI).
- Тесты: `AdCatalogTest` (4 креатива/уникальные id/все 4 действия, русский текст без эмодзи,
  disclaimer, ротация детерминирована и сдвигается по дню/открытию, дефолт в state).

## Изменённые/созданные файлы

Созданы (main):
- `data/model/AdModels.kt`, `data/model/CityCatalog.kt`, `data/model/ClothingAdvisor.kt`,
  `data/model/DefaultCities.kt`, `data/model/LootboxPrizes.kt`, `data/repository/AdRepository.kt`.

Изменены (main, data):
- `data/api/OpenMeteoClient.kt`, `data/cache/WeatherCache.kt`, `data/mapper/WeatherMapper.kt`,
  `data/model/FlightModels.kt`, `data/model/FoodModels.kt`, `data/model/LootboxModels.kt`,
  `data/model/PremiumModels.kt`, `data/model/TaxiModels.kt`, `data/model/WeatherModels.kt`,
  `data/model/WmoHelper.kt`, `data/provider/MetNoForecastParser.kt`,
  `data/repository/AiFactsRepository.kt`, `data/repository/FlightRepository.kt`,
  `data/repository/FoodRepository.kt`, `data/repository/LootboxRepository.kt`,
  `data/repository/TaxiRepository.kt`, `data/repository/WeatherRepository.kt`.
- `ui/main/MainScreenViewModel.kt` — моя зона (гео-флаг, такси-карты, лутбокс, факт,
  реклама, добавление города).

Тесты: созданы `AdCatalogTest`, `AiFactsRepositoryTest`, `CityCatalogTest`,
`ClothingAdvisorTest`, `LootboxPrizeFactoryTest`, `RussianContentGuardTest`;
обновлены `MetNoForecastParserTest`, `TaxiRepositoryTest`, `WeatherMapperTest`,
`MainScreenViewModelTest`, `UiPersistenceContractTest`, `EmojiFreeTextTest` (1 строка).

## Какие UI-файлы пришлось тронуть (минимально, без редизайна)

| Файл | Правка | Причина |
|---|---|---|
| `ui/main/MainWeatherCards.kt` | `"29.99 BYN"` → `DefaultUmbrellas.minPriceLabel` | у `UmbrellaItem` больше нет `priceByn`; строка «Зонтики от» |
| `ui/components/UmbrellaShopDialog.kt` | «29.99 BYN» → «649 ₽», CTA «Купить за 649 ₽», убран дубль цены | удалён `priceByn` |
| `ui/components/FlightBookingDialog.kt` | убрана строка с BYN, импорт `formatByn` | `FlightOffer.priceByn` удалён |
| `ui/components/TaxiBottomSheet.kt` | карточка тарифа показывает цену маршрута (`priceByTariff`) и время (`durationMinByTariff`), CTA — `selectedPriceRu`; убраны `basePriceRu/basePriceByn/formatByn` | у тарифа больше нет фиксированной цены; это же требование item 2 |
| `ui/components/PaywallDialog.kt` | комментарий KDoc: 5.99 BYN → 179 ₽/899 ₽ | тест-гард «нет BYN в main» |
| `ui/design/Formatters.kt` | удалён `formatByn`, комментарий к `formatRuAmount` → рубли | item 11 |
| `ui/main/MainScreen.kt` | `onOpenAdPromo` вызывает `viewModel.openAdPromo()` | чтобы VM обновлял креатив при открытии листа (item 9) |
| `ui/main/MainHeader.kt` | **НЕ тронут** | см. открытые вопросы: подпись должна переключать ui-coder |

## Решения и отклонения (важно для code-reviewer/judge)

1. **Формула такси и тест «1 км = минимум».** Требование даёт одновременно
   `durationMin = 3 + km*2.2`, `price = max(minFare, minFare + perKm*max(0,km-1) + perMin*durationMin)`
   и тест «1 км → минимум». При 1 км `durationMin = 5.2`, поэтому буквальная формула даёт
   `149 + 12*5.2 = 211 ₽`, а не минимум. Официальное условие «минимум 149 ₽ за 3 мин + 1 км»
   означает, что первые 3 минуты и первый километр уже включены. Реализовано:
   повременная часть начисляется только сверх включённого километра
   (`minFare + perKm*max(0, km-1) + perMin*(duration-3)`, если `km > 1`), поэтому
   1 км = ровно `minFare`, 10 км = 521 ₽ (эконом). Surge применяется к итоговой сумме
   (1 км в 9 баллов = 257 ₽ — это корректно, наценка умножает всю поездку). Если оркестратор
   настаивает на буквальной формуле — это одна функция `TaxiPricing.calculatePriceRu`
   и два теста.
2. **Лимит городов.** В коде лимита не было. Добавлен `CityCatalog.MAX_SAVED_CITIES = 12`,
   применяется только к добавлению новых; существующие города выбираются всегда.
   «Лимит не ломать» трактовано как «не дать списку расти безгранично».
3. **Старый дефолтный город удалён по координатам**, а не по названию/стране: тест-гард
   запрещает сами подстроки в `app/src/main`, поэтому в коде нет названия удалённого города.
   Миграция одноразовая, перезаписывает `saved_cities_json` только при изменении.
4. **`TOMORROW_WEATHER` → `WEATHER_TOMORROW`** — переименование вместо добавления второго
   значения с тем же смыслом; использований, кроме репозитория, не было.
5. **Погодный приз «на сегодня».** Требование: VM передаёт `daily[1] ?: daily[0]`. Если
   завтрашнего дня нет, сегодняшнюю погоду нельзя выдавать за завтрашнюю — текст и
   `dayLabelRu` говорят «на сегодня». При обычном прогнозе — «на завтра» (тест на «дождь на завтра»).
6. **7timer**: количество осадков не отдаёт — `precipitationMm = null`, ноль не выдумываем.
7. **`EmojiFreeTextTest`**: удалена единственная строка `texts += it.priceByn` (без неё тест
   не компилируется после удаления поля). Проверки эмодзи не ослаблены.
8. **BYN во FoodModels** не был перечислен в задаче, но без его удаления не проходит
   тест-гард «нет BYN в `app/src/main/**`»; UI еды использовал только рубли.

## Открытые вопросы / что осталось ui-coder

1. `ui/main/MainHeader.kt` по-прежнему рисует жёстко «ТЕКУЩЕЕ МЕСТО». Нужно:
   `if (uiState.isSelectedCityCurrentLocation) "ТЕКУЩЕЕ МЕСТО" else "ВЫБРАННЫЙ ГОРОД"`.
   Данные и флаг готовы, тесты на флаг и на «город из поиска → false» зелёные.
2. `AdPromoDialog`/`AdBannerCard` ещё показывают старый контент. Данные готовы:
   `uiState.adCreative` (brand/headline/body/cta/action/disclaimer) и `uiState.adImageUrl`;
   CTA должен маршрутизироваться по `adCreative.action`
   (PAYWALL → пейволл, LOOTBOX → лутбокс, UMBRELLA_SHOP → витрина, FOOD → Еда/шторка).
3. `LootboxDialog` ещё не использует `history`/`lastRewardSeq`/погодный текст и не
   показывает конфетти/вибрацию — это item 8 у ui-coder. В `LootReward` уже есть
   `weatherCode`, `temperatureRangeRu`, `dayLabelRu`, `subtitle` с практическим советом.
4. `HourlyWeatherCard` (вкладка «Осадки») ещё не читает `HourlyItem.precipitationMm`;
   контракт: есть мм → столбики, есть только `popKnown` → %, иначе «Нет данных об осадках».
5. `TaxiBottomSheet` рисует цену маршрута из state, но визуально это старый лист;
   если ui-coder переделывает карточки — брать `taxiState.priceByTariff[tariff]`
   и `durationMinByTariff[tariff]`, не считать в composable.
6. `DailyDetailDialog` вызывает `WmoHelper.getClothingAdvice(temp, code)` без города/дня —
   работает, но для полного детерминизма по городу лучше звать
   `ClothingAdvisor.adviceFor(..., cityName, dayOfYear)`.

## Границы соблюдены

- `refs/`, `refs_src/`, `_history/reports/phase-4/`, `ACCEPTANCE.md` не изменялись.
- Коммит не делался; `assembleDebug`/`assembleBenchmark` не запускались, adb не вызывался.
- В рабочем дереве параллельно присутствуют файлы другого агента (data-coder B):
  `ATTRIBUTIONS.md`, `data/model/ThemeBackgrounds.kt`, `res/drawable-nodpi/` — я их не
  создавал и не менял; они участвовали в компиляции и не мешают.

---

# Правка после Фазы 5: лутбокс больше не применяет тему сам

Дата: 2026-11-14. Файл: `app/src/main/kotlin/com/koch/weather/ui/main/MainScreenViewModel.kt`,
функция `openLootbox()`. Коммит не делался, APK не собирался, adb не вызывался.

## Причина

`openLootbox()` одновременно разблокировал тему и применял её:
`currentTheme = newTheme ?: state.currentTheme` + `cache.setCurrentTheme(...)`.
Из-за этого фон менялся сразу после дропа, карточка приза в `LootboxDialog` уже
показывала «Применена» (ветка `theme == currentTheme`), экран «Поздравляем!» выглядел
как «тема применена», а кнопка «Применить» (`onApplyTheme = { viewModel.setTheme(it) }`
в `MainScreen.kt:390`) становилась бессмысленной.

## Дифф по смыслу

Было:

```kotlin
val newTheme = ThemePreferenceCodec.decode(reward.unlockedThemeName)
_uiState.update { state ->
    state.copy(
        availableThemes = unlockedThemes,
        themeCatalog = ThemeCatalog.build(unlockedThemes),
        currentTheme = newTheme ?: state.currentTheme,
        lootboxState = state.lootboxState.withDrop(reward, unlockedThemes)
    )
}
if (newTheme != null) {
    cache.setCurrentTheme(ThemePreferenceCodec.encode(newTheme))
}
```

Стало:

```kotlin
_uiState.update { state ->
    state.copy(
        // Дроп только разблокирует тему. currentTheme не трогаем:
        // фон переключает setTheme() по кнопке «Применить», а не лутбокс.
        availableThemes = unlockedThemes,
        themeCatalog = ThemeCatalog.build(unlockedThemes),
        lootboxState = state.lootboxState.withDrop(reward, unlockedThemes)
    )
}
```

- Удалено присваивание `currentTheme` внутри `_uiState.update`.
- Удалён блок `if (newTheme != null) { cache.setCurrentTheme(...) }`.
- Удалена локальная `newTheme` (после правки не использовалась; лог не добавлял).
- Разблокировка не тронута: `availableThemes`, `themeCatalog` и
  `lootboxState.withDrop(reward, unlockedThemes)` остались как были. Сама тема
  по-прежнему сохраняется в DataStore внутри `LootboxRepository.openLootbox`
  (`cache.unlockTheme`), поэтому после дропа она честно открыта в каталоге.
- Импорт `ThemePreferenceCodec` не осиротел: он по-прежнему нужен в `setTheme`
  (`ThemePreferenceCodec.encode(theme)`); кроме того, импорт `data.model.*` — wildcard,
  отдельной строки для удаления нет.
- `setTheme(theme)` (строки 630–636) не менялся: проверяет `theme in availableThemes`,
  сразу обновляет `currentTheme` и пишет `cache.setCurrentTheme(ThemePreferenceCodec.encode(theme))`.
  После дропа тема уже в `availableThemes`, поэтому «Применить» срабатывает.

## UI и рулетка

`ui/**` не менялся. В `LootboxDialog` две точки, зависящие от «применённости»:

- карточка приза (`LootRewardCard`, строки 780–785): пока тема не равна `currentTheme`,
  кнопка называется «Применить» и зовёт `onApplyTheme`; после нажатия становится
  «Применена». Раньше из-за авто-применения она сразу была «Применена»;
- экран «Поздравляем!» (`ThemeRevealContent`): подпись «Тема применена: ...» показывается
  только при `appliedTheme == preview` (`LootboxDialog.kt:190`), то есть после нажатия
  «Применить»; кнопка активна, если тема есть в `availableThemes`.

После правки ВМ тема попадает в `availableThemes` сразу после дропа (значит, «Применить»
активна), но `currentTheme` и DataStore меняются только в `setTheme`, поэтому фон до
нажатия остаётся прежним.

## Тесты

Детерминированный unit-тест «после `openLootbox()` тема не меняется» без переделки
архитектуры невозможен:

- `MainScreenViewModel` — `AndroidViewModel(Application)`, репозитории и `WeatherCache`
  создаются через `by lazy` внутри VM, внедрения зависимостей нет;
- `openLootbox()` идёт через `viewModelScope` + `Dispatchers.IO`, `delay(1200)`,
  `LootboxRepository` использует `Random` и DataStore;
- в `app/build.gradle.kts` только `junit` и `kotlinx-coroutines-test`, Robolectric/Mockito
  нет; `Application` и DataStore в JVM-тесте недоступны, а тест на `copy()` состояния
  был бы фиктивным и не проверял бы `openLootbox()`.

Поэтому архитектура не переделывалась, фиктивный тест не добавлялся. `MainScreenViewModelTest.kt`
не изменялся. Проверка: ревью кода (дифф выше) + существующие зелёные тесты + ручной
сценарий для device-qa: открыть лутбокс → дождаться дропа темы → убедиться, что фон
экрана не изменился, в каталоге тема открыта → нажать «Применить» → фон сменился →
перезапуск приложения → выбранная тема восстановилась.

## Результат проверки

```powershell
$env:JAVA_HOME="D:\jdk-17\jdk-17.0.12+7"
cd D:\akcon\weatherAPK-dsh
.\gradlew.bat --console=plain compileDebugKotlin
.\gradlew.bat --console=plain testDebugUnitTest
```

`compileDebugKotlin` — `BUILD SUCCESSFUL in 10s` (выполнен `:app:compileDebugKotlin`).
`testDebugUnitTest` — `BUILD SUCCESSFUL in 15s`, `:app:testDebugUnitTest` выполнен.

Счёт по `app/build/test-results/testDebugUnitTest/TEST-*.xml`:
**34 сьюта, 198 тестов, 0 падений, 0 ошибок, 0 пропусков** — как и до правки,
регрессий нет.

## Правки по код-ревью: Минск + двойной тап

Дата: 2026-11-14. Правки по вердикту `code-reviewer` (пункты H1 и M3). Код не
коммитился, APK не собирался, adb не вызывался, диск C не использовался.

### 1) H1 — остатки прежнего города (item 11)

`data/repository/TrafficRepository.kt`:
- удалены записи `"минск" to 157` и `"minsk" to 157` из `cityToRegionMap`;
- удалён координатный fallback `53.7..54.1 / 27.3..27.8 → 157` и упоминание города
  в комментарии; остались только СПб (2) и Москва (213);
- поведение для неизвестного города не изменилось: `resolveRegionId` → `null`,
  `getTraffic` возвращает `score = null`, `description = "Нет данных о пробках"`,
  `source = "None"` (в сеть не ходит). Вызовы проверены: ВМ кладёт `traffic.score`
  в `weather.trafficScore`, `MainWeatherCards` при `null` не рисует `N/10`, а
  показывает «Нет данных о пробках»; такси через `withRoute(..., trafficScore = null)`
  честно считает без наценки за пробки.
- регистронезависимый grep по `app/src/main` (kt+xml) на `byn|минск|minsk|беларус` —
  0 совпадений.

`data/RussianContentGuardTest.kt`:
- критерий вынесен в `offendersIn(text, markers)` на
  `text.contains(marker, ignoreCase = true)`; скан файлов использует его же;
- маркеры: `BYN`, `Минск`, `Беларус`, `minsk`;
- добавлен юнит-тест `guard_catchesLowercaseSamples` на строках-образцах (не на
  реальном файле): «поездка в минск», «trip to MINSK», «179 byn», «беларусский
  маршрут» ловятся, «Санкт-Петербург, Москва, Сочи, Дубай» и «Нет данных о пробках» —
  нет. До удаления записей этот гард падал бы на нижнем регистре в `TrafficRepository`.

Новый `data/TrafficRepositoryTest.kt` (3 теста): «Москва» → регион 213; город вне
покрытия (координаты удалённого fallback 53.9/27.5) → `resolveRegionId == null`;
`getTraffic` для него → `score == null`, «Нет данных о пробках», source `None`.

### 2) M3 — двойной тап по лутбоксу

`ui/main/MainScreenViewModel.kt`, `openLootbox()`:
- «замок» ставится синхронно и атомарно **до** `viewModelScope.launch`:
  цикл `read → if (isOpening) return → compareAndSet(state, state.copy(isOpening = true))`;
  второй тап видит `isOpening = true` и выходит, не запуская вторую награду;
- `isOpening = true` больше не выставляется внутри IO-корутины;
- замок снимается в `finally` (только если всё ещё `isOpening`), поэтому отмена
  корутины/ошибка не оставляют кнопку в вечном loading. После `withDrop` флаг уже
  `false` — `finally` это no-op.
- `delay(1200)` и логика награды (`withDrop`, `availableThemes`, `themeCatalog`)
  не менялись; `currentTheme` по-прежнему не трогается.

### Результат проверки

```powershell
$env:JAVA_HOME="D:\jdk-17\jdk-17.0.12+7"
cd D:\akcon\weatherAPK-dsh
.\gradlew.bat --console=plain compileDebugKotlin
.\gradlew.bat --console=plain testDebugUnitTest
```

`compileDebugKotlin` — `BUILD SUCCESSFUL in 13s`.
`testDebugUnitTest` — `BUILD SUCCESSFUL in 19s`.

Счёт по `app/build/test-results/testDebugUnitTest/TEST-*.xml`:
**35 сьютов, 202 теста, 0 падений, 0 ошибок, 0 пропусков.**
(Было 34/198: `RussianContentGuardTest` +1 тест, новый `TrafficRepositoryTest` +3.)

## Правка осадков по факту с устройства

Дата: 2026-11-14. Фаза 5, item 10. Симптом с Sony G8342: в карточке «Осадки» за
24 часа «Σ 0.0 мм» и пустой график, в детали будущего дня «Осадки: Нет данных».
Правка — только data-слой: `MetNoForecastParser.kt`, тесты. `ui/**` не трогался,
`MetNoModels.kt` править не понадобилось.

### Что было

- `Point` знал только `next_1_hours.details.precipitation_amount`; у дальних
  точек MET отдаёт 6-часовые количества (`next_6_hours`) — они игнорировались,
  поэтому суточная сумма за дальние дни = `null` → «Осадки: Нет данных».
- Почасовой ряд строился фильтром
  `points.filter { it.localDate == currentPoint.localDate && it.timeMs >= currentPoint.timeMs }`:
  вечером в нём оставалось 1–3 часа, график рисовал пустые столбики и «Σ 0.0 мм».
- `MetNoModels.kt` (п. 4 задачи) уже содержал `next_6_hours`/`next_12_hours` →
  `MetNoNext.details.precipitation_amount`; добавлений не потребовалось.

### Что стало (`data/provider/MetNoForecastParser.kt`)

1. `Point` + `precipitation6h` / `precipitation12h` — из
   `next6Hours.details.precipitationAmount` / `next12Hours.details.precipitationAmount`.
2. `hourly`: окно `[currentPoint.timeMs, currentPoint.timeMs + 24 ч)` и `take(24)` —
   ряд больше не обрывается на полуночи. `hourly.precipitation` — только
   `next_1_hours` (реальные мм/ч); где поля нет — `null`, 6-часовое значение как
   «мм/ч» не подставляется.
3. `dailyPrecipitation`: суммирует часовые + 6-часовые + 12-часовые значения
   (`sumDailyPrecipitation`), `null` — только если за день не было ни одного
   числа (пустой `details: {}` тоже даёт `null`, а не 0).

Важная поправка к постановке п. 3: «у точки заполнено максимум одно из трёх
полей» неверно для реального compact-ответа. По `shots/fq3_metno_live.json`
(живой ответ с устройства) ближние точки содержат **одновременно** `next_1_hours`
и скользящее `next_6_hours` (например 02:00Z: 1 ч = 0.0, 6 ч = 0.0; 03:00Z:
1 ч = 0.0, 6 ч = 0.8), а с 3-х суток сетка становится 6-часовой. Прямое сложение
всех полей удвоило бы осадки, поэтому сумма считается по **непересекающимся
интервалам**: сначала часовые (1 ч), затем 6/12-часовые только там, где часы ещё
не покрыты. Для дальних дней остаются только 6-часовые точки — они суммируются
полностью.

### Проверка на реальном payload

`shots/fq3_metno_live.json` (СПб, `updated_at 2026-11-14T02:32Z`, Europe/Moscow):
`hourly` = 24 точки 05:00 → 04:00 следующего дня; Σ по 24 ч = 2.2 мм (было
1–3 точки в зависимости от времени суток); `daily.precipitationSum` =
`[2.2, 0.0, 0.0, 2.6, 3.3, 2.4, 0.0, 2.7, 0.0, 0.0]` — дальние дни (17/18/19/21.11)
больше не `null`, при этом ближний день 2.2, а не раздутая сумма скользящих окон.

### Дополнительно: `WeatherMapper` и длина `daily`

Потеря/обрезка `daily` не обнаружена: `WeatherMapper.buildDailyList` идёт по всем
`daily.time`, берёт `precipitationSum.getOrNull(i)`; `MainScreen` передаёт
`weather.daily` в `TenDayForecastCard`, который сам показывает `take(10)` —
столько же, сколько просит `forecast_days=10`. Правок не требовалось.

### Тесты

`app/src/test/kotlin/com/koch/weather/data/MetNoForecastParserTest.kt` — новая
фикстура `mixedHorizonJson` по реальной структуре compact (часовые точки с
1- и скользящими 6-часовыми значениями, дальше 6-часовые точки с пустым
`next_12_hours.details`) и 4 теста:
- `farDays_dailySumUsesSixHourAmounts_notNull` — 16.11 = 2.5 мм, 17.11 = 1.5 мм,
  и те же числа доходят до домена (`DailyItem.precipitationMm`);
- `hourlySeries_coversNext24Hours_acrossMidnight` — 15 точек, первая 23:00,
  последняя 13:00 следующего дня; 6-часовые 1.2/0.9/0.8 не попали в мм/ч;
- `nearDay_withBothOneAndSixHour_doesNotDoubleCount` — 0.3 и 0.3 мм вместо
  наивных 1.5 и 3.0;
- `emptyDetailsObject_keepsNull_notZero` — точки с `details: {}` дают `null` и в
  почасовке, и в сутках.

### Результат проверки

```powershell
$env:JAVA_HOME="D:\jdk-17\jdk-17.0.12+7"
cd D:\akcon\weatherAPK-dsh
.\gradlew.bat --console=plain testDebugUnitTest
.\gradlew.bat --console=plain compileDebugKotlin
```

`testDebugUnitTest` — `BUILD SUCCESSFUL in 22s`; `compileDebugKotlin` —
`BUILD SUCCESSFUL in 5s`.

Счёт по `app/build/test-results/testDebugUnitTest/TEST-*.xml`:
**35 сьютов, 206 тестов, 0 падений, 0 ошибок, 0 пропусков**
(было 202: `MetNoForecastParserTest` +4; сьютов столько же — тесты добавлены в
существующий класс). APK не собирался, adb не вызывался, коммитов нет.


## Правки по фактам устройства: список городов, факт дня

Дата: 2026-11-14, 19:05-19:17. Основание: `_history/reports/phase-5/device-qa.md` §5,
дефекты D4 и D5 (Sony G8342, Run 14). Правки только в data-слое и
`ui/main/MainScreenViewModel.kt`; `ui/**` (кроме VM), `res/**`, `refs/`, `refs_src/`,
`ACCEPTANCE.md` не трогал. APK не собирался, adb не вызывался, коммитов нет,
диск C не использовался.

### D4. «Список» показывал «Нет данных» для непросмотренных городов

Причина: `refreshSavedCities()` читал только память/DataStore
(`WeatherRepository.getSavedCitiesWeather`), сеть не трогал. Города, которые ещё
не открывали на главной, оставались без прогноза, пока их не выберешь.

Что изменено:

- Новый `data/repository/SavedCitiesWeatherSync.kt` — чистая логика догрузки:
  - `pendingCities()` отбирает только строки без данных (`hasData == false`),
    без дублей по месту, максимум `CityCatalog.MAX_SAVED_CITIES` = 12;
  - `loadPending()` запускает запросы через `Semaphore` не более 3 одновременно,
    ошибка/таймаут/`null` от загрузчика не создают строку с выдуманной погодой;
  - `applyLoaded()` обновляет только совпавшую строку, не пересобирает весь
    список и не затирает более свежие данные старым кэшем.
- `MainScreenViewModel.refreshSavedCities()`:
  1. сразу показывает кэш, как раньше (`savedCities` из памяти/DataStore);
  2. без сети завершается сразу — строки честно остаются «Нет данных»;
  3. в фоне догружает только города без данных через существующую цепочку
     `WeatherRepository.getWeatherData()` (MET Norway → Open-Meteo → 7timer,
     политика кэша сохраняется);
  4. каждый ответ применяется к одной строке `savedCities`.
- Отмена: добавлено поле `savedCitiesJob`; новый вызов `refreshSavedCities()`
  отменяет предыдущий, `selectTab()` при уходе с «Списка», `addCity()` при
  переходе на главную и `onAppBackground()` отменяют догрузку. Параллельных
  дублей одного списка нет.
- Лимит: не более 12 городов и 3 одновременных запросов.

Проверка на устройстве не выполнялась (роль не собирает APK и не вызывает adb):
`feature-qa`/`device-qa` должны открыть «Список» через 2-3 с и убедиться, что у
каждого города либо реальная температура, либо «Нет данных». Логика гарантирует,
что промежуточного пустого состояния нет: пока ответа нет, показывается кэш или
честное «Нет данных».

### D5. Факт «А знали ли вы…» менялся между перезапусками в один день

Причина: сам `AiFactsRepository.getFactForDay(city, dayOfYear)` уже был
детерминирован (string hash + день года, без `Random` и времени суток). Но
`MainScreenViewModel` инициализировал факт от `targetCity.name`, а `targetCity` при
старте — это восстановленный из DataStore «последний город, для которого
сохранился прогноз». После просмотра Москвы/Казани перезапуск стартовал на другом
городе и показывал другой факт, хотя день тот же.

Что изменено:

- Новый `data/model/FactOfDayPolicy.kt`: город-якорь факта — гео-город
  («текущее место»), а не последний открытый; порядок списка и выбранный город
  на якорь не влияют.
- `MainScreenViewModel`: стартовый `aiFact` считается от
  `FactOfDayPolicy.anchorCityName(cities)` + `dayOfYear()`; смена города факт не
  меняет. При возврате из фона на новом календарном дне факт пересчитывается
  детерминированно от якоря и нового дня (`refreshAiFactForNewDayIfNeeded`).
- `refreshAiFact()` — единственный способ получить другой факт в течение дня
  (`getNextFact`, не равен текущему); он же фиксирует текущий день, чтобы
  foreground-проверка не перезаписала выбор пользователя в тот же день.
- `AiFactsRepository.getFactForDay` — без изменений поведения; уточнена
  документация: сид не зависит от времени суток и `Random`.

Тесты:

- `FactOfDayPolicyTest` (5): два «старта» с одним городом и днём дают одинаковый
  факт; восстановленный последний город (Казань) не меняет факт дня; якорь не
  зависит от порядка списка/выбранного города; fallback для пустого списка.
- `AiFactsRepositoryTest` (+2): два разных экземпляра репозитория с одним
  городом и днём дают одинаковый факт; 50 вызовов `getNextFact` не меняют
  `getFactForDay`.
- `SavedCitiesWeatherSyncTest` (10): кэш не грузится повторно, лимит 12,
  дедупликация, обновление только одной строки, старый кэш не затирает свежий,
  при `null`/ошибке температура не выдумывается, параллельность ровно 3,
  отмена прекращает колбэки, после догрузки каждая строка — с температурой или
  честным «Нет данных».

### Результат проверки

```powershell
$env:JAVA_HOME="D:\jdk-17\jdk-17.0.12+7"
cd D:\akcon\weatherAPK-dsh
.\gradlew.bat --console=plain compileDebugKotlin --rerun-tasks --no-build-cache
.\gradlew.bat --console=plain testDebugUnitTest --rerun-tasks --no-build-cache
```

- `compileDebugKotlin` — `BUILD SUCCESSFUL in 36s`, 7 задач выполнено.
- `testDebugUnitTest` — `BUILD SUCCESSFUL in 56s`, 26 задач выполнено.

После последней правки (`@Volatile` у поля дня факта, отмена догрузки в
`addCity` и тест «каждая строка с температурой или «Нет данных»») повторный
прогон: `compileDebugKotlin` — `BUILD SUCCESSFUL in 10s`, `testDebugUnitTest` —
`BUILD SUCCESSFUL in 14s`, счёт тестов тот же.

Счёт по `app/build/test-results/testDebugUnitTest/TEST-*.xml`:
**37 сьютов, 223 теста, 0 падений, 0 ошибок, 0 пропусков**
(было 35/206: +2 сьюта, +17 тестов — `SavedCitiesWeatherSyncTest` 10,
`FactOfDayPolicyTest` 5, `AiFactsRepositoryTest` +2).

Инфраструктурная заметка: первый принудительный `compileDebugKotlin` упал на
чужой незакоммиченной правке `ui/components/LootboxDialog.kt`
(`Icons.AutoMirrored.Filled.DirectionsWalk`, receiver mismatch). Эту правку делал
не я; `ui/**` не трогал. После `:app:clean` полный прогон с
`--rerun-tasks --no-build-cache` прошёл зелёным, то есть в актуальном дереве
ошибки нет (сбой был в инкрементальном кэше/параллельной правке).

## D-2 feature-qa: выбранный город переживает перезапуск

Дата: 2026-11-14, 23:18-23:31. Основание: `_history/reports/phase-5/feature-qa.md`,
D-2 (low): выбрал Сочи → `am force-stop` + `am start` → открылась Казань.
Правки только в data-слое и `ui/main/MainScreenViewModel.kt`; APK не собирался,
adb не вызывался, коммитов нет, диск C не использовался.

### Причина

Город при старте брался только из `last_city_json`, а этот ключ пишется в
`WeatherCache.saveForecast()` при каждом сохранении прогноза — то есть там
лежит «последний прогноз», а не последний выбор пользователя. После выбора Сочи
какой-то фоновый прогноз (или предыдущий город) перезаписывал ключ, и
перезапуск открывал Казань. Тема при этом восстанавливалась отдельно
(`ThemeRestorePolicy`), города в персистентном состоянии не было.

### Что изменено

- `data/cache/WeatherCache.kt`: новый ключ `selected_city_json` и методы
  `saveSelectedCity(city)` / `getSelectedCity()`. Город хранится целиком,
  по координатам (как `saved_cities_json`); `last_city_json` не тронут —
  поведение «последнего прогноза» прежнее.
- `data/repository/WeatherRepository.kt`: `saveSelectedCity()` /
  `getSelectedCity()` делегируют в кэш.
- Новый `data/model/SelectedCityRestorePolicy.kt` — startup-политика:
  1. сохранённый выбор, если город есть в актуальном списке
     (`CityCatalog.isSamePlace`, то есть по координатам);
  2. если сохранённого выбора ещё нет (старая установка) — прежнее поведение:
     последний прогноз, если он есть в списке;
  3. иначе гео-город («текущее место»). Город, которого больше нет в списке,
     не возвращается: падаем на гео-город, а не на последний прогноз.
- `MainScreenViewModel`: при старте `targetCity` считается через
  `SelectedCityRestorePolicy.resolve(savedSelected, lastSavedCity, initialCities)`;
  в `selectCity()` выбор сохраняется (`persistSelectedCity`, запуск на Main, запись
  на IO — при быстрых свайпах порядок вызовов сохраняется, последний выбор
  побеждает). Смена города свайпом, из списка и из поиска идёт через
  `selectCity()`, поэтому персистится одинаково.

### Тесты

`UiPersistenceContractTest` +6 (было 6, стало 12):
- `selectedCity_survivesVmRecreation` — выбор сохранён, startup-политика нового
  экземпляра возвращает тот же город (VM в JVM не создать: нужен
  Application/DataStore, поэтому проверяется ровно та политика, которую VM
  применяет при старте);
- `selectedCity_jsonRoundTrip_restoresSameCoordinatesAfterRestart` — JSON-круг
  CityLocation (как в DataStore), восстановление по координатам;
- `selectedCity_winsOverLastForecastCity` — прошлый прогноз не подменяет выбор;
- `selectedCity_missingFromList_fallsBackToGeoCity` — удалённый город → гео;
- `noSavedSelection_keepsLegacyLastForecastBehavior` — старая установка без
  выбора сохраняет прежнее поведение;
- `noSavedSelectionAndNoLastForecast_fallsBackToGeoCity` — пустое состояние → гео.

### Результат проверки

```powershell
$env:JAVA_HOME="D:\jdk-17\jdk-17.0.12+7"
cd D:\akcon\weatherAPK-dsh
.\gradlew.bat --console=plain compileDebugKotlin --rerun-tasks --no-build-cache
.\gradlew.bat --console=plain testDebugUnitTest --rerun-tasks --no-build-cache
```

`compileDebugKotlin` — `BUILD SUCCESSFUL in 2m 7s`; `testDebugUnitTest` —
`BUILD SUCCESSFUL in 2m 0s`. Во втором прогоне Kotlin-демон один раз написал
`Unable to release compile session` (инфраструктурное предупреждение, не ошибка
кода); сборка и тесты завершились успешно.

Счёт по `app/build/test-results/testDebugUnitTest/TEST-*.xml`:
**37 сьютов, 229 тестов, 0 падений, 0 ошибок, 0 пропусков**
(было 37/223: +6 тестов в `UiPersistenceContractTest`).
