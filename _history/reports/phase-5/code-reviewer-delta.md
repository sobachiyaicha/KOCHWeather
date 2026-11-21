# code-reviewer — Фаза 5, дельта-проверка после FAIL

Дата: 2026-11-14, 19:32. Роль: `code-reviewer` (повторная проверка). Код не правился,
файлов проекта не менял (создан только этот отчёт + лог прогона
`_history/reports/phase-5/gradle-delta-test.log`). Диск C не использовался.
HEAD `c07dd19`, все изменения рабочие, незакоммиченные.
Предыдущий отчёт: `_history/reports/phase-5/code-reviewer.md` (FAIL: H1, M1, M2).

## Итог

**PASS.** Все блокирующие пункты предыдущего отчёта закрыты по существу, новых
HIGH/MEDIUM не появилось. Тесты: **37 сьютов / 223 теста, 0 падений** (свежий
принудительный прогон, `BUILD SUCCESSFUL in 4m 10s`, exit 0).

Проверены ровно те файлы, которые правились после прошлого ревью (mtime ≥ 18:57):
`TrafficRepository`, `RussianContentGuardTest`, `TrafficRepositoryTest`,
`MetNoForecastParser(+Test)`, `MainCards`, `MainHeader`, `ThemeVisuals`,
`LootboxReveal`, `MainScreen`, `MainWeatherCards`, `ThemeGalleryScreen`,
`FactOfDayPolicy`, `SavedCitiesWeatherSync(+Test)`, `AiFactsRepository(+Test)`,
`LootboxDialog`, `MainScreenViewModel`. Плюс сверка с HEAD по `TrafficRepository`.

## 1. Закрытие блокеров

**H1 (Минск/BYN) — закрыт.**
`TrafficRepository.kt`: удалены `"минск" to 157`, `"minsk" to 157`, комментарий
`// Coordinate fallback for SPb, Moscow, Minsk` и координатный fallback
`53.7..54.1 / 27.3..27.8 → 157`; `resolveRegionId` возвращает `null` вне покрытия,
`getTraffic` отдаёт `score=null / "Нет данных о пробках" / source="None"`.
Независимый регистронезависимый скан `app/src` (`минск|minsk|byn|беларус`) даёт
совпадения **только в тесте**.
`RussianContentGuardTest`: `contains(marker, ignoreCase = true)`, в маркерах есть
`"BYN"`, `"Минск"`, `"Беларус"`, `"minsk"`; отдельный тест `guard_catchesLowercaseSamples`
падает, если вернуть регистрозависимость; сканируются `.kt` **и** `.xml` под
`src/main` (включая `res/**`). Тест не вакуумный: `assertTrue(files.isNotEmpty())`.
Новый `TrafficRepositoryTest` (3) проверяет и `resolveRegionId`, и `getTraffic`
для координат удалённого fallback (`53.9 / 27.5` → `null`).

**M1 (залипшая вспышка) — закрыт.**
`LootboxDialog`: `flash.snapTo(0f)` при старте эффекта открытия; вспышка идёт
ПАРАЛЛЕЛЬНО крышке (`coroutineScope { launch { flash } ; lid }`), итог
700 + max(240, 70+220) = **990 мс < 1200 мс** задержки VM; `finally` +
`withContext(NonCancellable) { flash.snapTo(0f) }`; в seq-эффекте перед конфетти
`flash.snapTo(0f)`. `NonCancellable` — это Job, а не диспетчер: контекст остаётся
Main, `snapTo` мгновенный, UI-поток не блокируется. Мьютекс `Animatable`
освобождается при отмене `animateTo`, поэтому `snapTo` в `finally` не зависает.
Итог: `flash` строго 0 и при дропе, и при закрытии листа, и при отмене.

**M2 (недостижимый «Другой факт») — закрыт.**
`MainCards.AiFactCard` содержит видимую строку `Refresh + "Другой факт"`
(`clickable(role = Role.Button)`), а тап по блоку тоже ведёт в
`MainScreenViewModel.refreshAiFact()` (`MainScreen.kt:687`). Мёртвой проводки нет:
`onRefreshAiFact` объявлен и используется. Модалка травы переехала на
`combinedClickable(onLongClick = onOpenGrassModal)` того же блока.
Двойного срабатывания нет: вложенный `clickable` потребляет down, а родительский
`detectTapGestures` (`awaitFirstDown(requireUnconsumed = true)`) не начинает жест;
даже если бы начал — оба ведут в один и тот же `onClick`.

**M3 (двойной дроп) — закрыт.**
`openLootbox`: `while (true) { val s = _uiState.value; if (s.lootboxState.isOpening) return;
if (_uiState.compareAndSet(s, s.copy(...isOpening = true))) break }` — замок
выставляется синхронно ДО `launch`; `finally` снимает флаг, но только если он всё
ещё true (`if (!isOpening) state`), поэтому поздний `finally` не сбрасывает замок
нового открытия. `MutableStateFlow.update/compareAndSet` потокобезопасны.
Остаточный риск (не блокирующий): если `launch` придёт в уже отменённый
`viewModelScope`, тело не выполнится и флаг останется true — но это происходит
только после `onCleared`, когда UI уже не существует.

**M4/M5 (из прошлого списка «желательно») — закрыты.**
`ThemeVisuals`: `BitmapFactory.decodeResource` с `inSampleSize = 4` (270×480 ≈ 0.5 МБ),
`LruCache<Int, ImageBitmap>(8)` живёт вне композиции (7 тем помещаются), кеш
переживает уход/возврат элемента в LazyGrid, повторного декодирования нет;
`recycle()` нигде не вызывается — правильно (битмап ещё рисуется Compose, recycle
дал бы `Canvas: trying to use a recycled bitmap`). Ошибка декодирования → откат
на `ThemeBackgroundImage`/градиент, не пустая плашка.
`LootboxReveal`: заголовок от `droppedTheme`, подпись «Смотрите другие темы»,
мокап с полным названием (`maxLines = 2`, `KochType.tab` 10 sp).

## 2. Двойной счёт осадков (MET)

`sumDailyPrecipitation`: из точек дня собираются интервалы (1 ч / 6 ч / 12 ч),
сортируются по длительности (короткие первыми), затем по началу; принимаются
только непересекающиеся. Для реального compact-ответа, где на одной точке есть
и `next_1_hours`, и скользящее `next_6_hours`, часовые интервалы покрывают сутки,
а 6/12-часовые отбрасываются как перекрытые → **суммы по дням не удваиваются**.
Дальние дни (только 6-часовые точки) суммируются целиком; `details: {}` → `null`,
а не `0.0`; нет ни одного значения за сутки → `null`.
Проверено на фикстурах тестов: 14.11 = 0.3 (а не 0.3+1.2), 15.11 = 0.3
(а не 3.3), 16.11 = 2.5, 17.11 = 1.5.
Ряд и индексы: `hourly.precipitation` собирается из тех же `hourlyPoints`
(`map`), `HourlyItem.precipitationMm` берётся по тому же `i`, что и температура;
«Сейчас» — `current.precipitation` (та же текущая точка), дубля часа нет.
`daily.precipitationSum` и `HourlyItem/DailyItem` — параллельные списки:
все `continue` в парсере (нет температуры / не определился WMO) происходят
до добавления в ЛЮБОЙ из списков, поэтому индексы не разъезжаются.
24-часовой ряд теперь строится от текущей точки (`timeMs in [current, current+24h)`),
поэтому не обрывается на полуночи (`hourlySeries_coversNext24Hours_acrossMidnight`).

## 3. SavedCitiesWeatherSync

- Параллельность: `Semaphore(3)`, `withPermit` отпускает permit в `finally`
  (в т.ч. при отмене) — утечек нет; корутин не больше 12.
- Лимит 12: `pendingCities` обрывается на `MAX_CITIES = CityCatalog.MAX_SAVED_CITIES`,
  плюс `cities.take(MAX_CITIES)` в `loadPending` и в VM.
- Дубли не грузятся: `pendingCities` отбрасывает `CityCatalog.isSamePlace`.
- Отмена: `selectTab` (не-LIST), `onAppBackground`, `addCity`, повторный
  `refreshSavedCities` отменяют job; `CancellationException` пробрасывается
  (`catch` только для прочих исключений), поэтому `coroutineScope` завершается
  без «сирот».
- Точечное обновление: `applyLoaded` меняет только строку совпавшего города,
  `_uiState.update` атомарен; каждый callback читает `state.savedCities` из
  лямбды, поэтому параллельные ответы не теряются. Устаревший ответ не затирает
  более свежий (`weather.lastUpdated >= row.updatedAt`).
- Никаких выдуманных данных: `load` возвращает `null` при `Unavailable`, `onLoaded`
  для `null` не вызывается, строка остаётся «Нет данных».
Замечание (не блокер): в `CityListScreen` все строки лежат в ОДНОМ `LazyColumn`
item `cities_group`, поэтому обновление одной строки рекомпозирует группу из ≤12
строк (данные при этом точечные). Стоимость ничтожна.

## 4. FactOfDayPolicy

Детерминизм подтверждён: `getFactForDay` → `ClothingAdvisor.seedFor(city, day)` =
`city.trim().lowercase().hashCode() + day`, `String.hashCode` в JVM специфицирован
и стабилен между запусками; `Random` и время суток не участвуют
(`getRandomFact`/`getNextFact` — только для кнопки «Другой факт»).
Город-якорь = гео-город (`anchorCityName`), порядок списка и выбранный город на
факт не влияют. Новый день: `refreshAiFactForNewDayIfNeeded` на `onAppForeground`
сравнивает `aiFactDayOfYear` и пересчитывает детерминированно; `refreshAiFact`
выставляет `aiFactDayOfYear = dayOfYear()`, чтобы ручной «другой факт» не слетал
до следующего дня. Ограничение (LOW): если приложение не уходит в фон и не
перезапускается, на полуночи факт не сменится до ближайшего `ON_START`.

## 5. Новые UI-правки

`BitmapFactory` — см. п.1 (M4): утечки нет, повторного декодирования нет,
`recycle` не нужен и опасен.
`KochType.tab` (10 sp) — новый стиль, используется только в новых мокапах
(`LootboxReveal`), другие экраны не затронуты.
`MainHeader` — диапазон отдельной строкой `КочType.caption` + `end = 56.dp`
под повёрнутую температуру; геометрия `rotate(-15°)` не тронута.
`MainWeatherCards` — «Осадки» без дождя подписаны просто «Осадки»; звёзды и число
выводятся из ОДНОЙ величины (`halfStarsOf`): 3.5 → 3 полных + половинка,
подпись «3.5». Логика `full/half` на `roundToInt` корректна.
`MainScreen` — `useDarkStatusIcons = veilVisible`, эффект живёт только на HOME
(см. остаточное замечание 6).

## 6. Регрессии и общий скан

- `TODO/FIXME/XXX/HACK` — 0 в новом коде (6 совпадений по `app/src` — это
  `toDouble`, `getByName`-подобные подстроки, не маркеры).
- Эмодзи — независимый скан всех `.kt` на U+1F000–U+1FAFF/пиктограммы: **0**.
- `@Ignore` / `assertTrue(true)` / `fail()` — 0. Новые тесты исполняются и
  проверяют реальные ветки (`TrafficRepositoryTest` не ходит в сеть для города
  без покрытия, поэтому не флакует).
- `AndroidManifest`: `android.permission.VIBRATE` на месте, манифест проходит
  `processDebugMainManifest`/`processDebugManifestForPackage`.
- Компилятор: предупреждений в исходниках нет (deprecated `DirectionsWalk` ушёл).
- Новых мёртвых параметров правки не добавили: `onRefreshAiFact`, `droppedTheme`,
  `onLongClick`, `applyLoaded`, `pendingCities`, `loadPending(onLoaded)` используются.

## Остаточные (НЕ блокирующие) замечания

1. **M6 из прошлого отчёта не правился** (был в блоке «желательно»):
   `WeatherRepository.normalizeCities` по-прежнему на каждом старте добавляет
   4 дефолта и фильтрует «устаревшую точку» (53.9006, 27.5590) — город,
   добавленный пользователем в этих координатах, исчезнет после рестарта, а
   список может вырасти больше 12. Код вне дельты, на приёмку не влияет.
2. Модалка травы теперь достижима **только долгим нажатием** на блок AI-факта.
   Это осознанное решение (комментарий в коде), но `feature-qa`/`visual-critic`
   должны знать жест, иначе R11 зафиксируют как «недостижимо».
3. MET: 12-часовой интервал, частично перекрытый часовыми, отбрасывается целиком
   (недооценка возможна только в смешанных дальних днях, двойного счёта нет).
4. `TrafficRepository.apiKey` — неиспользуемый параметр конструктора (был и до
   правки); `isLoadingSavedCities` никогда не ставится в `true`
   (`CityListScreen.isLoading` всегда false) — тоже до правки.
5. `EmojiFreeTextTest` по-прежнему сканирует `data/**` + `MainScreenViewModel.kt`,
   но не `ui/components` (мой независимый скан чист).
6. Статус-бар: `LaunchedEffect` живёт только на HOME; если уйти с главной, пока
   видна облачная вуаль, тёмные иконки останутся на других вкладках до возврата.
7. Тестовые пробелы: нет unit-теста на `openLootbox` (атомарный замок) и на
   `refreshSavedCities` в VM — проверено только чтением кода.

## Прогон тестов

```
$env:JAVA_HOME="D:\jdk-17\jdk-17.0.12+7"
cd D:\akcon\weatherAPK-dsh
.\gradlew.bat --console=plain testDebugUnitTest --rerun-tasks
```

- `:app:compileDebugKotlin`, `processDebugMainManifest`, `:app:testDebugUnitTest` —
  все выполнены, **BUILD SUCCESSFUL in 4m 10s**, exit code 0.
- По свежим `app/build/test-results/testDebugUnitTest/TEST-*.xml` (19:26):
  **37 сьютов, 223 теста, 0 падений, 0 ошибок, 0 пропусков**.
- Новые/расширенные сьюты в прогоне: `TrafficRepositoryTest` 3,
  `RussianContentGuardTest` 2, `SavedCitiesWeatherSyncTest` 10,
  `FactOfDayPolicyTest` 5, `AiFactsRepositoryTest` 8,
  `MetNoForecastParserTest` 11.
- Побочное: во время прогона параллельно шла другая Gradle-сборка («Detected
  multiple Kotlin daemon sessions»), из-за чего Kotlin-даемон один раз упал на
  битом инкрементальном кеше (`lookups.tab.keystream`); Gradle автоматически
  переключился на компиляцию без демона, сборка и тесты завершились успешно.
  Это не дефект кода, но `device-qa` стоит знать: одновременные сборки в одном
  checkout могут чинить кеш.

## Вердикт

**PASS.** H1, M1 (ранее обозначенный как H2/M2 в задании), M2 и M3 закрыты;
новых блокирующих дефектов нет; 223 теста зелёные; отмеченные выше пункты —
не блокирующие и относятся к ранее известным остаткам.
