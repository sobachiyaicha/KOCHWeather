# KOCHWeather — «Погода 2025»

Android-приложение-сатира на погодные сервисы, собранное по референс-видео:
живые карточки (Дубай, лутбоксы, такси, еда), премиум-пейволл, рекламный баннер,
рулетка тем, жест «прогони облака пальцем», модалка «про траву», нижний таб-бар.

## Стек

- Kotlin + Jetpack Compose + Material3, `minSdk 24`, `targetSdk 34`, `compileSdk 36`
- kotlinx.serialization, OkHttp, DataStore, Coil, osmdroid (карта OSM)
- AGP 9.0.1, Kotlin 2.3.20, Compose BOM 2026.03.01, Gradle wrapper 9.1.0
- Шрифт Inter (OFL) в `res/font`, кириллица проверена

## Данные (работает из РФ без VPN и ключей)

| Задача | Основной | Резерв |
|---|---|---|
| Прогноз | **MET Norway** `api.met.no/weatherapi/locationforecast/2.0/compact` (свой User-Agent, кэш-политика 30 мин / 3 ч) | **7timer** `www.7timer.info` (8 с), затем **Open-Meteo** (в части сетей РФ IP недоступен) |
| Геокодинг | **Photon** | Nominatim → Open-Meteo geocoding |
| Карта | OSM-тайлы через osmdroid | — |
| Фото | Wikimedia Commons (500 px превью, свой User-Agent) | плейсхолдер Coil |

Порядок загрузки: гонка MET/Open-Meteo (20 с) → 7timer (8 с) → снимок из DataStore → честное «нет данных».
Синтетических температур нет: если данных нет — «Нет данных», кэш помечается как кэш с временем.
Авто-retry с backoff (5→20→40→60 с), триггеры: восстановление сети, foreground, таймер; кнопка «Повторить» сбрасывает backoff.

## Сборка

```powershell
$env:JAVA_HOME = "D:\jdk-17\jdk-17.0.12+7"
.\gradlew.bat assembleDebug        # debug  (~21.5 МБ)
.\gradlew.bat assembleBenchmark    # release-подобная: R8+shrink, profileable, подпись debug (~3 МБ)
.\gradlew.bat testDebugUnitTest    # 144 теста, 27 сьютов
```

- Всё на диске D:: `gradle.user.home=D:/.gradle`, `org.gradle.java.home` → D:, `java.io.tmpdir` → `.local/tmp`.
- Сборка и установка на устройство — только через `device-qa`; кодеры гоняют `compileDebugKotlin` и `testDebugUnitTest`.
- Baseline profile: `app/src/main/baseline-prof.txt` (в APK `assets/dexopt/baseline.prof`).
- `local.properties`: `sdk.dir=D:\Android\Sdk`. `TRAFFIC_API_KEY` читается из `local.properties` (не задан → пустая строка).

## Архитектура

| Путь | Что внутри |
|---|---|
| `data/api`, `data/provider` | Open-Meteo, MET Norway, 7timer, Photon/Nominatim, общая цепочка с бюджетами и отменой |
| `data/repository` | `WeatherRepository` (политика кэша/сети), `WeatherLoadPolicy`, `WeatherProviderChain` |
| `data/cache` | DataStore: `weather_<city>`, `weather_updated_<city>`, `weather_source_<city>`, `current_theme`, `clouds_cleared`, `saved_cities_json`, `is_premium_purchased`, `unlocked_themes_set` |
| `data/deeplink` | Яндекс Go (маршрут по координатам from→to), Яндекс Еда, веб-фолбэк |
| `data/model` | доменные модели (`@Immutable`), `SavedCityWeather`, `PremiumOffer`, `ThemeCatalogItem`, `ImageCatalog` |
| `ui/theme` | `Tokens.kt` (8 кеглей, 6 радиусов, сетка 4…32), `KochColors` (30 семантических ролей), 7 тем |
| `ui/design` | примитивы: `KochSheet`, `KochCard`, `KochButtons`, `KochChip`, `KochListRow`, `KochHeader`, `KochNavBar`, `KochBackground` |
| `ui/main` | `MainScreen`, `MainScreenViewModel`, лента (`CleanFeedOrder` — голубая тема, `PromoFeedOrder` — остальные) |
| `ui/components` | полноэкранные «Карта/Список/Поиск/Вид», облачная вуаль, листы (такси, еда, лутбоксы, билеты, премиум, трава, рулетка, зонты, реклама) |
| `app/src/test` | 27 сьютов / 144 теста: парсеры, мапперы, политика кэша, цепочка провайдеров, deep links, эмодзи-гард, VM |

## Референсы и доказательства

- `refs/*.png` + `refs/SCREENS.md` — кадры и описание экранов, `REFERENCE_TRANSCRIPT.md` — таймлайн речи и событий.
- `refs_src/reference.mp4` — исходное видео (только чтение).
- `shots/` — скриншоты с G8342 (`r7_*`, `r9_*`, `r10_*`, `s4_*`), `gfxinfo*`, `logcat*`, `dynamics/`.
- `_history/reports/phase-4/` — отчёты ролей: `device-qa.md`, `feature-qa-final.md`, `visual-critic-*.md`, `code-reviewer-*.md`, `perf-qa-final.md`, `judge.md`, `ui-coder-*.md`.
- `ACCEPTANCE.md` — матрица R1–R23 со статусами и путями к доказательствам.
- `USER_REQUIREMENTS.md` — свод требований и указаний пользователя (правки, запреты, решения).

## Команда и процесс

Роли: `ui-coder`, `data-coder` (код) → `device-qa` (сборка/устройство) → `feature-qa`,
`visual-critic`, `code-reviewer`, `perf-qa` (проверки) → `judge` (гейт).
Правила — `AGENTS.md`; после фазы стоп до подтверждения, коммит на фазу.

### Известные ограничения

- Сеть к погодным хостам в РФ нестабильна: `api.met.no` периодически рвётся на TLS,
  `open-meteo` может быть недоступен на уровне IP — для этого добавлен 7timer.
- Ядро Sony Xperia XZ1 держит kernel-треды дисплея в D-state, поэтому `loadavg`
  даже в простое ~4; абсолютные jank-цифры — оценка сверху, чистый замер — после ребута.
- `screenrecord` выбрасывает дубли кадров на статике: средний fps из mp4 не показатель,
  источник истины — `gfxinfo`/Choreographer.
