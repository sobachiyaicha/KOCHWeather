# KOCHWeather — «Погода 2025»

Android-приложение-сатира на погодные сервисы: живые карточки (Дубай, лутбоксы, такси, еда),
премиум-пейволл, реклама, выпадение тем из лутбоксов, жест «прогони облака пальцем»,
модалка «про траву», нижний таб-бар на 5 разделов.

## Стек

- Kotlin + Jetpack Compose + Material3, `minSdk 24`, `targetSdk 34`, `compileSdk 36`
- kotlinx.serialization, OkHttp, DataStore, Coil, osmdroid (карта OSM)
- AGP 9.0.1, Kotlin 2.3.20, Compose BOM 2026.03.01, Gradle wrapper 9.1.0
- Шрифт Inter (OFL) в `res/font`, кириллица проверена

## Данные (работает из РФ без VPN и ключей)

| Задача | Основной | Резерв |
|---|---|---|
| Прогноз | **MET Norway** `api.met.no/weatherapi/locationforecast/2.0/compact` (свой User-Agent, кэш-политика 30 мин / 3 ч) | **7timer** `www.7timer.info` (8 с), затем **Open-Meteo** (в части сетей РФ IP недоступен) |
| Осадки | MET: почасовые мм из `next_1_hours`, суточные суммы из `next_1_hours` + `next_6_hours` + `next_12_hours` без двойного счёта | Open-Meteo `precipitation` / `precipitation_sum`; 7timer осадков не отдаёт — честное «нет данных» |
| Геокодинг | **Photon** | Nominatim → Open-Meteo geocoding |
| Карта | OSM-тайлы через osmdroid | — |
| Фото | Wikimedia Commons (500 px превью, свой User-Agent) | плейсхолдер Coil |
| Фоны тем | 7 локальных JPEG 1080×1920 (Wikimedia, предварительный блюр), `res/drawable-nodpi` | градиент темы |

Порядок загрузки: гонка MET/Open-Meteo (20 с) → 7timer (8 с) → снимок из DataStore → честное «нет данных».
Синтетических температур нет: если данных нет — «Нет данных», кэш помечается как кэш с временем.
Авто-retry с backoff (5→20→40→60 с), триггеры: восстановление сети, foreground, таймер; кнопка «Повторить» сбрасывает backoff.
«Список» догружает погоду для сохранённых, но ещё не открытых городов (до 3 параллельно, с отменой).

## Сборка

```powershell
$env:JAVA_HOME = "D:\jdk-17\jdk-17.0.12+7"
.\gradlew.bat assembleDebug        # debug
.\gradlew.bat assembleBenchmark    # release-подобная: R8+shrink, profileable, подпись debug
.\gradlew.bat testDebugUnitTest    # 37 сьютов / 229 тестов
```

- Всё на диске D:: `gradle.user.home=D:/.gradle`, `org.gradle.java.home` → D:, `java.io.tmpdir` → `.local/tmp`.
- Сборка и установка на устройство — только через `device-qa`; кодеры гоняют `compileDebugKotlin` и `testDebugUnitTest`.
- Baseline profile: `app/src/main/baseline-prof.txt` (в APK `assets/dexopt/baseline.prof`).
- `local.properties`: `sdk.dir=D:\Android\Sdk`. `TRAFFIC_API_KEY` читается из `local.properties` (не задан → пустая строка).

## Архитектура

| Путь | Что внутри |
|---|---|
| `data/api`, `data/provider` | Open-Meteo, MET Norway (1/6/12-часовые осадки), 7timer, Photon/Nominatim, общая цепочка с бюджетами и отменой |
| `data/repository` | `WeatherRepository` (политика кэша/сети), `WeatherLoadPolicy`, `WeatherProviderChain`, `TaxiRepository`, `LootboxRepository`, `AdRepository`, `SavedCitiesWeatherSync`, `AiFactsRepository` |
| `data/model` | доменные модели (`@Immutable`), `DefaultCities`, `CityCatalog`, `TaxiPricing`, `LootboxPrizes`, `ClothingAdvisor`, `FactOfDayPolicy`, `AdCatalog`, `ThemeBackgrounds`, `ImageCatalog` |
| `data/cache` | DataStore: `weather_<city>`, `weather_updated_<city>`, `weather_source_<city>`, `current_theme`, `clouds_cleared`, `saved_cities_json`, `is_premium_purchased`, `unlocked_themes_set`, `lootbox_history_json` |
| `data/deeplink` | Яндекс Go (маршрут по координатам from→to), Яндекс Еда, веб-фолбэк |
| `ui/theme` | `Tokens.kt` (8 кеглей, 6 радиусов, сетка 4…32), `KochColors` (30 семантических ролей), 7 тем |
| `ui/design` | примитивы: `KochSheet`, `KochCard`, `KochButtons`, `KochChip`, `KochListRow`, `KochHeader`, `KochNavBar`, `KochBackground` (локальный фон темы) |
| `ui/main` | `MainScreen` (один `Scaffold` на все вкладки), `MainScreenViewModel`, лента (`PromoFeedOrder` — единый порядок для всех тем), анимация смены города |
| `ui/components` | полноэкранные «Карта/Список/Поиск/Вид», облачная вуаль, листы (такси, еда, лутбоксы с сундуком/конфетти/вибрацией и выпадением тем, билеты, премиум, трава, зонты, реклама) |
| `app/src/test` | 37 сьютов / 229 тестов: парсеры, мапперы, политика кэша, цепочка провайдеров, такси, лутбокс, реклама, города, советы, факт дня, контент-гарды |

## Валюты и цены

- Всё в рублях: премиум **179 ₽/год** (старая 899 ₽), зонты 649–990 ₽, билеты в рублях.
- Такси — тарифы Яндекс Go СПб: Эконом 149 ₽ (3 мин + 1 км) далее 12 ₽/км и 12 ₽/мин,
  Комфорт+ 205 ₽/17/17, Business 349 ₽/30/25 (оценка, помечено в коде). Цена считается под
  выбранный маршрут, при отсутствии данных о пробках — без наценки.

## Референсы и доказательства

- `refs/*.png` + `refs/SCREENS.md` — кадры и описание экранов, `REFERENCE_TRANSCRIPT.md` — таймлайн речи и событий.
- `refs_src/reference.mp4` — исходное видео (только чтение).
- `shots/` — скриншоты с G8342 (`r7_*`, `r9_*`, `r10_*`, `r14_*`), `gfxinfo*`, `logcat*`, `dynamics/`.
- `_history/reports/phase-5/` — отчёты ролей: `plan.md`, `device-qa.md`, `visual-critic*.md`, `feature-qa*.md`, `code-reviewer*.md`, `perf-qa*.md`, `judge*.md`, `ui-coder-*.md`, `data-coder-*.md`.
- `ACCEPTANCE.md` — матрица требований со статусами и путями к доказательствам.
- `USER_REQUIREMENTS.md` — свод требований и указаний пользователя.
- `ATTRIBUTIONS.md` — авторы и лицензии фоновых фотографий (Wikimedia Commons).

## Команда и процесс

Роли: `ui-coder`, `data-coder` (код) → `device-qa` (сборка/устройство) → `feature-qa`,
`visual-critic`, `code-reviewer`, `perf-qa` (проверки) → `judge` (гейт).
Правила — `AGENTS.md`; после фазы стоп до подтверждения, коммит на фазу.

### Известные ограничения

- Сеть к погодным хостам в РФ нестабильна: `api.met.no` периодически рвётся на TLS,
  `open-meteo` может быть недоступен на уровне IP — для этого добавлен 7timer
  (но 7timer не отдаёт количество осадков, поэтому в его сценарии «Осадки» честно пустые).
- Ядро Sony Xperia XZ1 держит kernel-треды дисплея в D-state, поэтому `loadavg`
  даже в простое ~4; абсолютные jank-цифры — оценка сверху.
- `screenrecord` выбрасывает дубли кадров на статике: средний fps из mp4 не показатель,
  источник истины — `gfxinfo`/Choreographer.
- Пробок (реального источника, доступного из РФ без ключа) нет: показывается «Нет данных о пробках».
- Покупка премиума в этой сборке — локальная имитация без биллинга.
- Модалка «Пора на улицу!» вызывается long-press по блоку «А знали ли вы…» (осознанно: обычный
  тап обновляет факт, а блокирующая модалка не должна открываться случайно).
