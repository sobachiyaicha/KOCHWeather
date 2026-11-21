# code-reviewer — Фаза 5 (14 замечаний пользователя)

Дата: 2026-11-14. Роль: `code-reviewer`. Код не правился, коммитов нет.
HEAD `c07dd19`, все изменения рабочие и незакоммиченные (47 изменённых + 17 новых файлов).
Диск C не использовался.

## Итог

**FAIL.** Логика данных и UI по 13 из 14 пунктов реализована честно: BYN и прежний
город из UI/дефолтов убраны, цены премиума/такси/офферов пересчитаны, осадки в мм
доведены от парсера до графика, лутбокс больше не применяет тему сам, реклама
ротируется детерминированно, навигация живёт в одном `Scaffold`, фоны тем — реальные
локальные фото 1080x1920. Тесты: **34 сьюта / 198 тестов, 0 падений** (свежий
принудительный прогон, `BUILD SUCCESSFUL`).

Блокируют приёмку:

1. **item 11 закрыт не полностью:** «Минск» остался в `app/src/main` (внутренний
   маппинг трафика), а новый тест-гард `RussianContentGuardTest` регистрозависим и
   поэтому даёт ложно-зелёный результат (см. HIGH-1).
2. **item 8/14:** залипающая вспышка в анимации лутбокса — после дропа `flash`
   остаётся на ~0.2–0.4 и белое свечение висит до закрытия листа (MEDIUM-1).
3. **item 12:** `refreshAiFact()` недостижим из UI, `onRefreshAiFact` — мёртвый
   параметр, кнопки «Другой факт» нет, хотя отчёт data-coder утверждает обратное
   (MEDIUM-2).

Все три правки локальные (1–2 файла), после них вердикт можно переводить в PASS.

## Что проверено

Дифф `git diff`/`git status --short` прочитан по всем 47 файлам, новые файлы — целиком.
Отдельно:

1. **Данные/миграции (item 11).** Регистронезависимый grep по `app/src/main/**`;
   `PremiumOffer` 179/899 ₽, `UmbrellaItem.priceRu`/`minPriceLabel`, оффер Сочи
   (FV-6543, 09:40/12:20, 2 ч 40 мин, 6900 ₽), `DefaultCities` (СПб/Москва/Сочи/Дубай),
   `WeatherRepository.getInitialCities`/`normalizeCities`, `CityLocation.isCurrentLocation`
   (дефолт + JSON старого кэша, `UiPersistenceContractTest`).
2. **Осадки (item 10).** `MetNoForecastParser` (`next_1_hours.precipitation_amount`,
   суточная сумма), `WeatherMapper` (индексы `precipitation`/`precipitation_sum`,
   «Сейчас» из `current.precipitation`), DTO/URL Open-Meteo, 7timer → null,
   `HourlyWeatherCard`/`PrecipitationChart`/`DailyDetailDialog`.
3. **Такси (item 2).** `TaxiPricing` (minFare/perKm/perMin, `billableMin` только при
   km > 1, surge null/0..10, ceil, без деления на ноль), пересчёт `priceByTariff`
   в VM при старте, смене города, точки, тарифа, приходе прогноза и пробок;
   `TaxiBottomSheet` не показывает фиксированных цен.
4. **Лутбокс (items 8, 14).** Веса (0.001/35|20/15/20/18 + кофе до 100), текст
   «выпал/выпала/выпало», WMO→вид погоды, отсутствие прогноза → нет погодного приза,
   `openLootbox(DailyItem?)`, история `lootbox_history_json` (кап 20, битый JSON),
   `lastRewardSeq`/`seenSeq`, вибрация (VibratorManager/`VibrationEffect`/`hasVibrator`),
   отсутствие `Context`-утечки, отсутствие `Random` в композиции, `Painter`-конфетти.
   **Проверено главное:** `openLootbox()` не трогает `currentTheme`/`setCurrentTheme`.
5. **Реклама (item 9).** 4 креатива, `(dayOfYear + openCount) % 4`, `openAdPromo()` при
   открытии листа, живой роутинг всех 4 `AdAction` в `MainScreen`, `disclaimerRu`.
6. **Навигация (item 3).** Один `Scaffold` (`contentWindowInsets = 0`), один
   `KochNavBar`, отсутствие двойных `navigationBarsPadding`, `BackHandler` только для
   не-HOME вкладок, hoisted `LazyListState`, повторный тап по активному табу,
   `DisposableEffect` osmdroid (`onResume`/`onPause`/`onDetach`).
7. **Анимация (item 5).** `AnimatedContent` только вокруг шапки и `HourlyWeatherCard`,
   `SizeTransform(clip = false)`, 280 мс, `LazyListState` не пересоздаётся.
8. **Города (item 6).** `CityCatalog` (`isSamePlace`, `dedupe`, `planAdd`, лимит 12),
   `addCity` (сразу выбирает и грузит), поиск «В списке», лимит с честной ошибкой.
9. **Советы (item 12).** `ClothingAdvisor` (7 диапазонов × 3 варианта + ветер/дождь/
   снег/гроза/лёд/солнце, seed = город + день), `AiFactsRepository.getFactForDay`,
   `getNextFact`, 20 фактов.
10. **Фоны (item 1).** `ThemeBackgrounds` на все 7 тем, `drawable-nodpi` = 7 JPEG
    1080x1920 (158–257 КБ, суммарно 1.49 МБ), визуальная проверка кадров (реальные фото:
    аврора, закаты, небо, лес, Марс, Осака), `painterResource` вместо Coil, скримы,
    `ThemeGalleryScreen` без рулетки, `ATTRIBUTIONS.md`.
11. **Общее.** Нет TODO/FIXME в новом коде, нет эмодзи (независимый скан всех `.kt`
    на U+1F000–U+1FAFF и пиктограммы — 0), нет `assertTrue(true)`/`@Ignore`, нет
    заглушек, логов с токенами; `AndroidManifest` с `VIBRATE` проходит
    `processDebugManifest`; мёртвый код инвентаризован.

## Найденные проблемы

### HIGH

**H1. «Минск» не удалён полностью, тест-гард ложно-зелёный (item 11).**
- `app/src/main/kotlin/com/koch/weather/data/repository/TrafficRepository.kt:47` —
  `"минск" to 157,`
- `…TrafficRepository.kt:48` — `"minsk" to 157,`
- `…TrafficRepository.kt:89` — комментарий `// Coordinate fallback for SPb, Moscow, Minsk`
- `…TrafficRepository.kt:92` — координатный fallback `53.7..54.1 / 27.3..27.8 → 157`
  (это координаты того же города).

В UI упоминания нет (там всё чисто), но DoD плана: «Никаких упоминаний
Беларуси/Минска в `app/src/main` и данных (тест на это добавить)».
`app/src/test/kotlin/com/koch/weather/data/RussianContentGuardTest.kt:14` содержит
`"Минск"` с заглавной, а `:30` делает `text.contains(marker)` — регистрозависимо,
поэтому тест зелёный при живых `"минск"`/`"minsk"` в main. Это ровно тот случай,
когда «зелёный тест» скрывает незакрытый пункт.
**Исправить:** удалить обе записи, комментарий и координатный fallback (или оставить
только fallback без названия); сделать гард регистронезависимым
(`text.contains(marker, ignoreCase = true)` и добавить `"minsk"`), перезапустить
`testDebugUnitTest`. После этого гард должен падать, пока записи не удалены.

### MEDIUM

**M1. Лутбокс: вспышка «зависает» после дропа (item 8).**
`ui/components/LootboxDialog.kt:141-151` — единый `LaunchedEffect(isOpening)`:
тряска 700 мс → крышка 240 мс → вспышка 70 мс вверх → **300 мс вниз** (итого 1310 мс).
VM выдаёт награду через `delay(1200)` (`MainScreenViewModel.kt:781`), при этом
`withDrop` ставит `isOpening = false`. Смена ключа отменяет корутину эффекта посреди
затухания; `flash` остаётся ~0.2–0.4, а `LootboxDialog.kt:312-332` продолжает рисовать
белый радиальный блик — «залипшее» свечение до закрытия листа.
`LaunchedEffect(lastRewardSeq)` (`:155-168`) сбрасывает `lid`, но не `flash`.
**Исправить:** в seq-эффекте добавить `flash.snapTo(0f)` (или `flash.animateTo(0f)`),
и/или выровнять `delay` в VM на время анимации (1350–1400 мс). Проверка — device-qa
кадром сразу после дропа.

**M2. «Другой факт» недостижим, мёртвая проводка (item 12).**
`ui/main/MainScreen.kt:455` объявляет `onRefreshAiFact`, `:202` передаёт
`viewModel.refreshAiFact()`, но внутри `MainScreenContent` параметр нигде не
используется (grep: только объявление и передача). `MainScreenViewModel.kt:767-772`
(`refreshAiFact`) не вызывается ни из UI, ни из тестов. `AiFactCard`
(`ui/main/MainCards.kt:61-96`) умеет только `onClick`, а он в `MainScreen.kt:684-687`
открывает модалку травы. Кнопки «Другой факт» нет. Отчёт data-coder-logic при этом
пишет «VM: … «Другой факт» — getNextFact» — фактически недостижимо.
**Исправить:** либо добавить в `AiFactCard` кнопку «Другой факт» → `onRefreshAiFact`,
либо удалить параметр и `refreshAiFact()` и честно убрать пункт из отчёта. Проверить
тестом на VM/UI-контракт, если решите оставить.

**M3. `openLootbox()` допускает двойной дроп при быстром тапе.**
`MainScreenViewModel.kt:777-781`: проверка `if (isOpening) return` стоит **до**
`viewModelScope.launch`, а `isOpening = true` выставляется уже внутри IO-корутины
(`:780`). Два быстрых тапа проходят проверку → два `openLootbox` в
`LootboxRepository.kt:34-36` читают одну и ту же историю и пишут её по очереди;
один дроп теряется в `lootbox_history_json` (плюс лишний `withDrop`).
**Исправить:** выставлять `isOpening = true` синхронно в `openLootbox()` до `launch`
(состояние — `MutableStateFlow`, это безопасно), а внутри снимать в `finally`.

**M4. Галерея тем декодирует 7 полных битмап 1080x1920.**
`ui/components/ThemeVisuals.kt:37-52` + `ThemeGalleryScreen.kt:171-180` используют
`painterResource` на `drawable-nodpi` без сэмплинга: 1080×1920×4 ≈ 8.3 МБ на тему,
до ~58 МБ, пока открыт «Вид» (плюс 8.3 МБ фон на главной). На G8342 это, вероятно,
не OOM, но это новый пик памяти на экране, который трогали ради item 1.
**Исправить/проверить:** отдельные превью 360–540 px для галереи (или Coil/`BitmapFactory`
с `inSampleSize`), замер RSS на устройстве через device-qa/perf-qa.

**M5. Экран «Поздравляем!» врёт после стрелок (items 4, 14).**
`ui/components/LootboxReveal.kt:119-125`: заголовок «Вам выпала тема «${state.preview}»»
привязан к `preview`, который меняют жёлтые стрелки. Пользователь листает на уже
имевшуюся тему и видит «Вам выпала тема», хотя выпала другая (или эта была открыта
раньше). Данные честные (`availableThemes`), текст — нет.
**Исправить:** заголовок оставить на фактически выпавшей теме (передавать `dropped`
отдельно от `preview`), либо при смене превью писать «Тема «X»» / «Оформление «X»».

**M6. Миграция списка городов принудительно возвращает дефолты.**
`data/repository/WeatherRepository.kt:117-136`: на каждом старте `normalizeCities`
фильтрует «устаревшую точку» (`:143-145`) и **добавляет все 4 дефолта**, если их нет
(`:122-129`). Последствия:
- удаление дефолтного города (когда появится UI удаления) не переживёт рестарт;
- у пользователя, добавившего города из поиска, список может вырасти за
  `CityCatalog.MAX_SAVED_CITIES = 12` (12 + дефолты), после чего `addCity` начнёт
  отклонять добавление с текстом «В списке уже 12 городов», хотя их 16;
- пользователь, добавивший тот самый город через поиск, теряет его на следующем
  старте (фильтр по координатам срабатывает всегда, а не один раз).
Идемпотентность и сохранность чужих городов при обычном списке — в порядке
(повторных записей нет: `saved != normalized`), но тестов на `normalizeCities` нет
(функция private + нужен `Context`).
**Исправить:** вынести нормализацию в чистую функцию `CityCatalog.normalize(saved,
defaults)` и покрыть тестами (старый список с удалённым городом, чужие города,
идемпотентность, лимит); добавлять дефолты только при первой миграции/пустом списке,
а не на каждом старте.

### LOW

**L1. MET: суточная сумма может быть неполной.** `MetNoForecastParser.kt:144-148`
суммирует только точки с `next_1_hours`; если в дне часть точек — `next_6_hours`
(дальние дни compact-ответа), сумма считается по части часов, но отдаётся как
суточная. Двойного счёта нет (точки уникальны), при полном отсутствии данных — null.
**Исправить:** если хотя бы одна точка дня без `next_1_hours` — отдавать null или
помечать «частичные данные» (или использовать `next_6_hours`).
**L2. `totalOpened` после рестарта = размер истории (≤20).**
`MainScreenViewModel.kt:231`; счётчик «Открыто: N» занижается. Завести отдельный
ключ счётчика или подписать «из последних 20».
**L3. `LootboxHistoryCodec.decode` теряет всю историю из-за одной битой записи.**
`data/model/LootboxModels.kt:93-100`: `decodeFromString<List<LootReward>>` —
падение одной записи (неизвестный enum) обнуляет все 20. Декодировать поэлементно.
**L4. Мёртвый код (инвентаризация, удалять не обязательно всё).**
`data/model/WeatherModels.kt:380-382` — `skyBlueImageUrl`/`skySunsetImageUrl`/
`skyNorthernImageUrl` в main больше не используются (только `ImageCatalogTest:72-74`);
по заданию — **рекомендую удалить** геттеры и 3 строки теста (сами `ImageCategory.SKY_*`
можно оставить для `ImageCatalog`/иконок рекламы). Также не используются:
`DefaultCities.geoCity` (`DefaultCities.kt:48`), `DefaultCities.isGeoCityName` (`:53-54`),
`ThemeVisuals.themeAccentColor` (`ThemeVisuals.kt:29`), `WmoHelper.getClothingAdvice`
(`WmoHelper.kt:51-52`, только тесты), `AiFactsRepository.getRandomFact/getDefaultFact`
(`AiFactsRepository.kt:39-41`, только тесты).
**L5. Тестовые пробелы.** Нет тестов на `normalizeCities`/миграцию, на
`LootboxRepository.openLootbox` + DataStore-историю (кап/битый JSON проверены только
кодеком), на пересчёт `priceByTariff` в VM, на роутинг `AdAction` в UI. Для JVM-слоя
тесты честные (проверял: ни `assertTrue(true)`, ни `@Ignore`).
**L6. `EmojiFreeTextTest` не сканирует `ui/components`.** `EmojiFreeTextTest.kt:64-77`
покрывает `data/**` + `MainScreenViewModel.kt`; новые `LootboxDialog`,
`AdPromoDialog`, `LootboxReveal`, `ThemeGalleryScreen` вне гарда. Мой независимый
скан всех `.kt` эмодзи не нашёл, но гард стоит расширить на `ui/**`.
**L7. Мелочи.** `LootboxDialog.kt:220-223` — ветка «Тема закрыта» недостижима
(`available` = `availableThemes`, все unlocked). `LootboxDialog.kt:836` —
deprecated `Icons.Filled.DirectionsWalk` (единственное предупреждение компилятора).
`LootboxDialog.kt:356-357` — шанс 0.001 % без «шанс выиграть» рядом с ценой приза
(не ошибка). `RussianContentGuardTest` также не проверяет `res/**` на всякий случай.

## Результат тестов

```
$env:JAVA_HOME="D:\jdk-17\jdk-17.0.12+7"
cd D:\akcon\weatherAPK-dsh
.\gradlew.bat --console=plain testDebugUnitTest --rerun-tasks
```

- `:app:compileDebugKotlin` — выполнен, **BUILD SUCCESSFUL**. Одно предупреждение:
  `LootboxDialog.kt:836` `Icons.Filled.DirectionsWalk` deprecated.
- `:app:processDebugMainManifest` / `processDebugManifestForPackage` — успешно
  (VIBRATE не ломает манифест).
- `:app:testDebugUnitTest` — **BUILD SUCCESSFUL in 4m 8s**, exit code 0.
- По `app/build/test-results/testDebugUnitTest/TEST-*.xml` (свежие, 18:05):
  **34 сьюта, 198 тестов, 0 падений, 0 ошибок, 0 пропусков**.
- Было в HEAD: 27 файлов / 144 `@Test`; сейчас 34 файла / 198 `@Test` — новые тесты
  (AdCatalog 4, AiFacts 6, CityCatalog 7, ClothingAdvisor 10, LootboxPrizeFactory 9,
  ThemeRevealState 5, RussianContentGuard 1 + расширенные MET/Mapper/Taxi/VM/Contract)
  действительно исполняются и не пустые.

## Вердикт

**FAIL** (не блокирующий по объёму работ, но обязательный к правке до повторного
ревью):

1. H1 — удалить `"минск"`/`"minsk"`/комментарий/координатный fallback из
   `TrafficRepository.kt:47-48, 89, 92`; сделать `RussianContentGuardTest`
   регистронезависимым; зелёный прогон после исправления.
2. M1 — сбросить `flash` при дропе (или `delay ≥ 1350`) в `LootboxDialog`/VM.
3. M2 — либо кнопка «Другой факт» с `onRefreshAiFact`, либо удалить мёртвую проводку
   и исправить отчёт.
4. Желательно в этой же правке: M3 (синхронный `isOpening`), M5 (текст reveal),
   M6 (тест/чистая функция миграции), M4 (память галереи — замер device-qa).

Остальные проверки — PASS: BYN/прежний город в UI, данные премиума/зонтов/Сочи,
формула и пересчёт такси, осадки (парсер → маппер → график), веса и тексты лутбокса,
отсутствие авто-применения темы, ротация рекламы и 4 CTA, один `Scaffold`/инсеты/
BackHandler/`LazyListState`, анимация не всего списка, `CityCatalog`, `ClothingAdvisor`,
`getFactForDay`, 7 локальных фонов, отсутствие эмодзи/заглушек/эмодзи-палитры,
чистый прогон 198 тестов.
