# code-reviewer — дельта-перепроверка M1/M2 (Phase 4)

- **Роль:** `code-reviewer`. Код не правился, APK не собирался, adb не использовался.
- **Дата:** 2026-11-14, 10:24–10:40 MSK.
- **База ревью:** предыдущий отчёт `_history/reports/phase-4/code-reviewer-final.md` (вердикт **FAIL**, 2 major: M1, M2). Полный аудит не повторялся — только M1, M2 и проверка, не внесли ли последние UI/perf-правки регрессий в `data`.
- **HEAD на старте:** `cbef2d1`; **на момент отчёта:** `05ee399` (`fix(ui): revert deferred sheet content that broke automation and taps`). `05ee399` меняет только `ui/design/KochSheet.kt` + docs/shots: `git diff cbef2d1..HEAD -- app/src/main/kotlin/com/koch/weather/data app/src/test` — **пусто**, поэтому ревью data-слоя валидно для обоих.
- **Рабочее дерево:** на момент отчёта чистое (`git status --porcelain` пуст). Видимое на старте ` M ui/design/KochSheet.kt` — это и был revert, закоммиченный в `05ee399` (файл изменён 10:22:26, коммит 10:24:26, т.е. тесты ниже шли уже по финальному содержимому).
- **Исправления:** `8be22d8 fix(data): metric wind units and reserved fallback budget; fix(ui): final audit fixes...`.

## ВЕРДИКТ: **PASS**

**M1 — закрыт. M2 — закрыт.** Блокеров и major нет. Два новых nit в UI-форматировании (не блокируют). Тесты: **27 сьютов / 144 теста / 0 падений**.

---

## 1. M1 — единицы ветра Open-Meteo: **ЗАКРЫТ**

**Было:** URL без `wind_speed_unit`, маппер клал `wind_speed_10m` (km/h по умолчанию у Open-Meteo) в домен как м/с → ошибка ×3.6.

**Стало (обе половины фикса, а не только одна):**

| Что | Где |
|---|---|
| Явные единицы в запросе | `data/api/OpenMeteoClient.kt:40` — `&temperature_unit=celsius&precipitation_unit=mm&wind_speed_unit=ms` (SQL-подобная сборка вынесена в `buildForecastUrl`, `:35`, чтобы её можно было тестировать) |
| Конвертация current | `data/mapper/WeatherMapper.kt:61-62` — `windSpeed10m * windToMetersPerSecond(dto.currentUnits?.windSpeed10m)` |
| Конвертация hourly | `data/mapper/WeatherMapper.kt:112` — фактор из `hourlyUnits`, применяется на `:149` |
| Сама конвертация | `data/mapper/WeatherMapper.kt:212-220` — `km/h|kmh → 1/3.6`, `mph → 0.44704`, `kn|knot → 0.514444`, иначе `1.0` (m/s) |
| DTO единиц | `data/model/WeatherModels.kt:11-12` + `CurrentUnitsDto`/`HourlyUnitsDto` (`:19-26`), `@SerialName("current_units")`/`("hourly_units")` |

**Тесты:**
- `app/src/test/kotlin/com/koch/weather/data/OpenMeteoClientTest.kt:10-18` — URL содержит `wind_speed_unit=ms`, `temperature_unit=celsius`, `precipitation_unit=mm` и **не** содержит `kmh`/`km/h`. **PASS**.
- `app/src/test/kotlin/com/koch/weather/data/WeatherMapperTest.kt:86-97` — km/h-ответ: `22.0/3.6 = 6.111` м/с и для current, и для hourly (`±0.001`), включая второй час `20 → 5.556`. **PASS**.
- `WeatherMapperTest.kt:99-110` — m/s-ответ не пересчитывается повторно. **PASS**.

**Проверка сквозной согласованности единицы (главный риск «чинили одно место»):**
- MET Norway уже в м/с: `data/provider/MetNoForecastParser.kt:81,103,117` передаёт `wind_speed` как есть (MET отдаёт м/с).
- 7timer: `SevenTimerForecastParser.kt:76` → `SevenTimerSymbolMapper.beaufortToMetersPerSecond(...)`, т.е. Beaufort → м/с.
- UI подписан и порогован именно в м/с: `ui/main/MainWeatherCards.kt:113` («Скорость ветра (м/с)»), `:133` (`"$it м/с"`), `:142-145` (пороги `<5`, `<10`, `<15` — осмысленны только для м/с).
→ Все три провайдера и UI теперь в одной единице. Контракт закрыт.
- Кэш: пишется доменная модель (уже м/с) — `WeatherRepository.kt:212`; legacy-снапшоты сырого API читаются fallback-веткой `:322` и проходят через тот же `WeatherMapper`, т.е. старый km/h-кэш тоже конвертируется корректно (самолечение).

## 2. M2 — бюджет провайдеров / 7timer: **ЗАКРЫТ**

**Было:** гонка основных провайдеров не была ограничена, 30-секундный общий дедлайн «съедал» 7timer — fallback не успевал запуститься.

**Стало:**

| Что | Где |
|---|---|
| Константы 20 / 8 / 30 с | `data/provider/WeatherProvider.kt:200-202` — `PRIMARY_TIMEOUT_MS = 20_000L`, `FALLBACK_TIMEOUT_MS = 8_000L`, `TOTAL_TIMEOUT_MS = 30_000L` |
| Окно только на гонку основных | `WeatherProvider.kt:83-95` — `withTimeoutOrNull(PRIMARY_TIMEOUT_MS) { race(...) }`, при таймауте синтетический `AllFailed(TIMEOUT)`, затем **обязательный** проход по fallback |
| Отдельное окно fallback | `WeatherProvider.kt:102-117` — `withTimeoutOrNull(FALLBACK_TIMEOUT_MS)` на каждый резервный провайдер, таймаут → честный `TIMEOUT`-фейл, а не зависание |
| Общий дедлайн | `WeatherProvider.kt:74-76` — внешний `withTimeoutOrNull(TOTAL_TIMEOUT_MS)` |
| Бюджет арифметически помещается | 20 + 8 = 28 ≤ 30 |
| Прод-подключение резерва | `WeatherRepository.kt:43-48` — `fallbackProviders = listOf(sevenTimerProvider)`, т.е. фикс не «только в тестах» |

**Тесты:**
- `app/src/test/kotlin/com/koch/weather/data/ProviderChainTest.kt:231-249` `hangingPrimary_stillAllowsSevenTimerFallback` — два «висящих» (`delay(60_000)`) основных + `CountingProvider(SEVEN_TIMER)` → `Success(SEVEN_TIMER)`, ровно 1 failed attempt с `TIMEOUT`, fallback вызван 1 раз. Тест не вакуумный: без `PRIMARY_TIMEOUT_MS` внешний 30-секундный дедлайн вернул бы `AllFailed` и fallback не запустился бы. **PASS**.
- `ProviderChainTest.kt:251-261` `totalBudget_reservesTimeForFallback` — пины 20000/8000/30000 и `PRIMARY + FALLBACK <= TOTAL`. **PASS**.
- `ProviderChainTest.kt:263-278` `allHangingIncludingFallback_returnsWithinTotalBudget` — все висят, ответ в пределах 30 с. **PASS**.

**Синтетический фейл с `WeatherSource.NONE`** (`WeatherProvider.kt:88`, `:190`) не протекает в UI как «none недоступен»: `WeatherRepository.kt:342-343` для `NONE` выводит только текст причины. Поведение осознанное, дефектом не считаю.

---

## 3. Регрессии в `data` от последних UI/perf-правок: **нет**

- `git diff --stat 653fc0f..HEAD -- app/src/main/kotlin/com/koch/weather/data` — **пусто**.
- `git diff --stat 8be22d8~1..HEAD -- .../data` — только 4 файла самого фикса M1/M2 (`OpenMeteoClient`, `WeatherMapper`, `WeatherModels`, `WeatherProvider`), `+80/−17`, без посторонних изменений.
- `git diff --stat 653fc0f..HEAD -- .../ui/main`: `MainCards.kt` (+11/−…), `MainScreen.kt` (+14), `MainWeatherCards.kt` (+28/−6) — правки вёрстки/контраста/статусбара; контракт данных не менялся, `MainScreenViewModel.kt` не тронут, возврата к «км/ч» нет.
- `05ee399` (revert sheet) — только UI + docs/shots.

## 4. Новые находки (nit, не блокируют)

| Severity | Место | Проблема | Комментарий |
|---|---|---|---|
| minor/nit | `ui/main/MainWeatherCards.kt:133` | `"$it м/с"` печатает `Double` без округления | В штатном пути Open-Meteo отдаёт `6.1` → «6.1 м/с», нормально; но в защитной km/h-ветке получится «6.111111111111111 м/с». Форматирование досталось «в наследство», после M1 стало заметнее. Предложение: `.roundToInt()` или 1 знак. |
| minor/nit | `ui/main/MainWeatherCards.kt:354` | почасовой ветер `it.toInt()` — усечение (6.9 → 6) | Пре-существующее, к M1/M2 не относится. |

Остальные ~30 minor из `code-reviewer-final.md` не трогались — по правилу приёмки «PASS без blocker/major» они не блокируют.

## 5. Тесты (обязательный прогон)

Команда: `.\gradlew.bat --console=plain testDebugUnitTest` (`JAVA_HOME=D:\jdk-17\jdk-17.0.12+7`, `TEMP/TMP=D:\akcon\weatherAPK-dsh\.local\tmp`).

- **BUILD SUCCESSFUL**, exit code 0 (первый прогон — `26 actionable tasks: 4 executed`; повторный после `05ee399` — `UP-TO-DATE`, код data/test не менялся).
- **27 сьютов, 144 теста, 0 падений, 0 ошибок, 0 пропусков** (`app/build/test-results/testDebugUnitTest/*.xml`).
- Было в финальном ревью: 26 сьютов / 139 тестов. **+1 сьют (`OpenMeteoClientTest`), +5 тестов** — ровно покрытие M1/M2.
- Точечно подтверждено (XML): `ProviderChainTest` 12/12, `WeatherMapperTest` 16/16, `OpenMeteoClientTest` 1/1; целевые кейсы `hangingPrimary_stillAllowsSevenTimerFallback`, `totalBudget_reservesTimeForFallback`, `allHangingIncludingFallback_returnsWithinTotalBudget`, `openMeteoWindKmh_isConvertedToMetersPerSecond`, `openMeteoWindMs_staysMetersPerSecond`, `forecastUrl_requestsExplicitMetricUnits` — **PASS**.
- Пробелы покрытия (не падения): M1/M2 доказаны unit-тестами, runtime-подтверждения с устройства нет; «красивый» вывод ветра в UI и uncommitted-состояние не тестируются.

## 6. Для judge

1. **M1/M2 закрыты кодом и тестами** — можно снимать FAIL по §1 финального отчёта.
2. `code-reviewer` вердикт — **PASS**; вердикт по фазе всё равно требует PASS от `feature-qa` + `visual-critic` (+ `perf-qa` для релиза) и свежих `shots/` на текущем HEAD (`05ee399`): run 9 снимался на `cbef2d1`, после него UI менялся (revert sheet), т.е. визуальные доказательства формально устарели на один UI-коммит.
3. Если judge хочет runtime-доказательство M2 — сценарий с принудительными таймаутами MET/Open-Meteo (или тест выше); скриншотом это не закрывается.
