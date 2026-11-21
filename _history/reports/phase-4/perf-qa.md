# perf-qa — Phase 4, Run 3 (benchmark-APK, HEAD f0f2b94)

- Дата: 2026-11-14, 05:30 MSK (черновик; device-окно ещё не выделено)
- Устройство: Sony G8342 (`CB512FQDXN`), Android 11 (SDK 30), 1080×1920, Adreno 630
- Установлено: `com.koch.weather`, `versionName 1.0-benchmark` (non-debuggable)
- **Статус отчёта: ЧАСТИЧНЫЙ (аналитическая часть готова, device-замеры по сценариям — ожидание окна).**
  По требованию оркестратора на устройстве параллельно работал `feature-qa`; adb-сценарии не
  запускались, чтобы не получить ложные цифры и не мешать чужому прогону. Таблица сценариев
  будет дозаполнена в выделенное окно (§8), после этого вердикт R18 станет окончательным.

> Код я не правил, APK не переустанавливал, `force-stop` в конце замеров — обязательство (§8).

---

## 1. Таблица «сценарий → p50/p95 → jank → PASS/FAIL»

| Сценарий | p50 | p95 | jank | PASS/FAIL | Источник |
|---|---|---|---|---|---|
| Холодный старт (процесс) | — (метрика не кадровая) | — | 1 кадр-фриз 1103 мс | **FAIL** | `am start -W` 1069/723/675 мс (device-qa Run 3) + `Davey! 1103 ms`, `Skipped 40 frames` (logcat run3) |
| Скролл главной | **11 мс** | **53 мс** | **14.07 %** (28/199) | **FAIL по jank** (p50 формально ≤16) | `shots/gfxinfo_run3_benchmark.txt` |
| Открытие листов (такси/еда/лутбокс/пейволл/рейс) | — | — | — | **НЕ ИЗМЕРЕНО** | окно §8 |
| Карта (osmdroid) | — | — | — | **НЕ ИЗМЕРЕНО** | окно §8 |
| Смена темы (рулетка/галерея) | — | — | — | **НЕ ИЗМЕРЕНО** | окно §8 |
| Загрузка фото (Wikimedia/Coil) | — | — | — | **НЕ ИЗМЕРЕНО** | окно §8 |
| Стирание облаков (динамика) | 16.7 мс медиана интервала кадров записи | 21.9 мс | 9 пауз >33 мс из 204 (4.4 %) | **условно PASS по медиане**, но запись не равна FPS приложения | `shots/dynamics/s3_clouds.mp4` (разбор PTS, §5) |

Пояснения к цифрам скролла (честно, без округления в свою пользу):

- Окно замера — **199 кадров**, это ~3.3 с при 60 Гц; `device-qa` снимал дамп **после `reset` и 7 свайпов** по главной. Одно окно = один сценарий, но **смешанный** (там же могли попасть кадры анимации загрузки/сети). Для чистых p50/p95 по каждому сценарию нужны отдельные окна.
- Гистограмма: **174 из 199 кадров (87.4 %) уложились в ≤16 мс**, но счётчик `Frame deadline missed = 13`, `Janky = 28` — jank считается по пропущенному vsync, а не по длительности кадра.
- **`Number High input latency: 181` из 199** — почти на каждом кадре ввод обрабатывался с задержкой. Для сценария «скролл» это значит: кадры рисуются, но реакция на палец отстаёт. Это и есть пользовательский jank, и он не виден в p50.
- GPU: p50 10 мс, p90 11, p99 12 — **медианный кадр упирается в GPU, а не в UI-поток** (11 мс кадра при 10 мс GPU). Запас до 16.6 мс — 1–2 мс. Любой лишний полноэкранный слой выбивает кадр из бюджета.

Порог для вердикта по сценарию (R18): p50 ≤16 мс **и** jank ≈ 0 **и** 60 FPS. Скролл проходит по p50 и не проходит по jank.

---

## 2. Честность сборки

Что подтверждено документально (отчёт `device-qa` Run 3, `shots/build_run3_benchmark.txt`):

| Проверка | Результат |
|---|---|
| `versionName` | **1.0-benchmark** |
| `isDebuggable` | **false** (флаг DEBUGGABLE в `dumpsys package` отсутствует) |
| Подпись | debug-ключ, пакет тот же `com.koch.weather` (install -r поверх debug) |
| R8 / minify | **выключен** (`app/build.gradle.kts:47`) |
| `isProfileable` | true (`app/build.gradle.kts:48`) |
| FPS-оверлей | выключен (`BuildConfig.DEBUG == false`, `MainScreen.kt:665`) |
| APK | `app-benchmark.apk`, 14 902 701 Б (14.21 МБ), 04:54:55 |
| Установка | `lastUpdateTime 2026-11-14 04:55:23`, затем `pm clear` + `logcat -c` |

**Самостоятельная проверка `dumpsys package | findstr debug` и `am start -W` ×3 мной пока НЕ выполнялась** — это первый пункт окна §8. Сейчас эти строки — пересказ доказательств `device-qa`, а не мой замер; в отчёте это важно, чтобы `judge` не считал их моими.

Важная оговорка о привязке к коду: установленный APK собран из `f0f2b94` (код), HEAD сейчас `72d5f4a` (только docs/shots). В рабочем дереве **есть незакоммиченная правка** `WeatherProvider.kt` (data-coder: гонка MetNo/Open-Meteo вместо последовательных таймаутов) + `ProviderChainTest.kt`. В установленном APK её **нет**. На частоту кадров она не влияет (сетевой слой), но на «холодный старт с сетью» и на первый экран «нет данных» — влияет: цифры §3 относятся к **последовательной** схеме провайдеров.

---

## 3. Холодный старт: 675–1069 мс по `am start -W`, но **первый кадр контента — ~1.1 с**

`am start -W` (benchmark, после `pm clear`, device-qa):

| Запуск | TotalTime | WaitTime |
|---|---|---|
| COLD #1 | 1069 мс | 1072 мс |
| COLD #2 (force-stop) | 723 мс | 728 мс |
| COLD #3 (force-stop) | 675 мс | 678 мс |
| WARM | 235 мс | 240 мс |

`ActivityTaskManager: Displayed com.koch.weather/.MainActivity: +728ms` — окно показано. **Но кадр контента
в это время ещё не нарисован.** В логе — по одному `Davey!` на каждый холодный старт:

```
05:19:33.007 29259 29275 I OpenGLRenderer: Davey! duration=1103ms; Flags=1;
  IntendedVsync=377850534465745, Vsync=377851001132393,
  PerformTraversalsStart=377851020264663, DrawStart=377851544394090,
  SyncQueued=377851600465294, SwapBuffers=377851635698005,
  FrameCompleted=377851638300245, QueueBufferDuration=1913282,
  DequeueBufferDuration=207552
05:19:33.067 29259 29259 I Choreographer: Skipped 40 frames!  The application may be doing too much work on its main thread.
```

Разбор первого кадра (pid 29259, COLD #3, 675 мс по `-W`):

| Фаза | Длительность |
|---|---|
| IntendedVsync → PerformTraversalsStart (ожидание main-потока: class load + JIT) | **486 мс** |
| PerformTraversalsStart → DrawStart (measure/layout + первый draw) | **524 мс** |
| DrawStart → SyncQueued (компиляция display list) | 56 мс |
| Sync → SwapBuffers (рендер) | 35 мс |
| **Итого до готового кадра** | **1103 мс** |
| `QueueBufferDuration` (производитель упёрся в BufferQueue) | 1913 мс |

Второй холодный старт (pid 26816, COLD #1, 1069 мс по `-W`): 509 мс до traversal + 559 мс на draw +
73 мс рендер = **1160 мс**, `QueueBufferDuration` 314 мс. То есть картина **воспроизводимая**, а не разовая.

Что это значит: `-W`-цифры 675–1069 мс — это «окно показано», а **белый/пустой экран до первого контента
живёт ~1.1 с**, и он попадает ровно на бюджет R18. 40–44 пропущенных кадра на старте — это системный jank,
даже если `TotalTime` выглядит нормально.

Причина по коду: на первом кадре впервые исполняется весь Compose-стек (measure/layout/draw), а ART
компилирует его JIT'ом, потому что **профиля приложения нет** (см. §6, P0-1). Именно это лечит baseline profile.

Чем это ещё усугубляется (по коду, не по логу):
- `MainScreen` — один composable на 690 строк, первый кадр строит LazyColumn из 6 item-блоков
  (`MainScreen.kt:416-583`), полноэкранный Canvas неба (`MainScreen.kt:394` → `KochBackground.kt:28-34`),
  нижний бар с `shadowElevation = 12.dp` (`KochNavBar.kt:62`).
- ANR на старте (Run 2) уже вылечен ленивыми зависимостями VM (`MainScreenViewModel.kt:27-46`) — это
  подтверждено отсутствием ANR в Run 3 и не переоткрывается.

---

## 4. Скролл главной: подробности замера

`shots/gfxinfo_run3_benchmark.txt` (benchmark, после `reset` + 7 свайпов):

```
Total frames rendered: 199        Janky frames: 28 (14.07%)
50th: 11 ms   90th: 17 ms   95th: 53 ms   99th: 150 ms
Number Missed Vsync: 6           Number High input latency: 181
Number Slow UI thread: 9         Number Frame deadline missed: 13
50th gpu percentile: 10 ms  90th: 11  95th: 12  99th: 12
GPU Scratch RenderTarget: 7.97 MB (1 entry)   ← полноэкранный offscreen-слой 1080×1920
```

`Scratch RenderTarget 7.97 МБ` — это ровно один экран 1080×1920 ARGB (8.29 МБ). В коде единственное, что
просит полноэкранный offscreen-слой, — `CloudVeilOverlay`:
`graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)` (`CloudVeilOverlay.kt:62`).
Само по себе это не приговор, но в сочетании с `Number Slow UI thread: 9` и `High input latency 181`
говорит: во время окна поверх ленты жил слой, который перерисовывается целиком.

**Осторожно с выводом:** scratch-буфер в дампе — это состояние GPU-кэша **на момент дампа**, а не
доказательство, что слой жил все 199 кадров. Поэтому в §8 велюра будет снята отдельным окном
(«облака видны» vs «облака стёрты»), и тогда цифры станут однозначными.

Тренд по фазам (все, кроме run3, — **debug**, цифры нечестные, приведены только как история):

| Дамп | Сборка | Кадров | jank | p50 | p95 | GPU p50 |
|---|---|---|---|---|---|---|
| `gfxinfo.txt` | debug | 66 | 43.94 % | 8 мс | 69 мс | 4 мс |
| `gfxinfo_audit.txt` | debug | 252 | 39.68 % | 10 мс | 65 мс | 7 мс |
| `gfxinfo_run2.txt` | debug | 126 | 32.54 % | 9 мс | 150 мс | 8 мс |
| **`gfxinfo_run3_benchmark.txt`** | **benchmark** | **199** | **14.07 %** | **11 мс** | **53 мс** | **10 мс** |

Честная интерпретация: **хвост стал короче в 1.2–2.8 раза** (p95 150 → 53), jank упал примерно вдвое,
но **p50 не улучшился, а слегка вырос** (9 → 11 мс), и GPU-медиана выросла (8 → 10 мс). Окна разные,
сценарии разные — это не A/B одной и той же операции. Единственный честный вывод: «release-подобная
сборка убирает длинные хвосты debug-сборки, но не даёт 60 FPS по jank и съедает запас GPU».

---

## 5. Динамика стирания облаков (`shots/dynamics/s3_clouds.mp4`, разбор PTS)

`device-qa` записал `screenrecord --time-limit 10` и указал «204 кадра, 20.4 fps». Я разобрал MP4
по `stts` (time-to-sample), чтобы не путать «кадров/секунда записи» с частотой кадров приложения:

| Метрика записи | s3 (benchmark) | r2 (debug) |
|---|---|---|
| Сэмплов | 204 | 30 |
| Медиана интервала | **16.7 мс** (60 Гц) | 133.3 мс |
| p90 / p95 | 17.1 / 21.9 мс | 827 / 962 мс |
| Паузы >33 мс | **9 из 204 (4.4 %)** | 29 из 30 |
| Средний интервал | 49.0 мс (из-за пауз) | 333 мс |
| Максимальная пауза | **1990 мс** | 1016 мс |

Вывод аккуратный: **пока кадры шли, они шли на 60 Гц** (193 из 204 интервалов в диапазоне 10–20 мс),
но запись содержит 9 провалов, включая ~1.0 с, ~1.5 с и ~2.0 с. «20.4 fps» из отчёта device-qa — это
среднее по 10 с, и оно врёт в пессимистичную сторону. При этом пауза 1.99 с в записи **совпадает** с
`QueueBufferDuration = 1913282` мкс из §3 — то есть это не артефакт энкодера, а реальная остановка
производителя кадров в тот же период сессии.

`r2_clouds.mp4` (debug) для сравнения FPS не годится: 30 сэмплов на 10 с — это голод энкодера/виртуального
дисплея, а не 3 FPS UI; в debug-сборке к тому же работал FPS-оверлей. Использовать эти цифры как
«было/стало» нельзя.

Ограничение метода: `screenrecord` меряет композицию виртуального дисплея + энкодер, а не
`Choreographer` приложения. Для вердикта R18 авторитет — `gfxinfo`; видео — только динамическое
доказательство того, что фризы доходят до пользователя.

---

## 6. Hotspots (file:line) и рекомендации

Приоритет: **P0** — даёт измеримые миллисекунды на текущем железе; **P1** — заметно; **P2** — гигиена.

### P0-1. Baseline profile покрывает только библиотеки, кода приложения в нём нет

Факты (проверено по артефактам, без устройства):
- В APK есть `assets/dexopt/baseline.prof` — **11 747 Б** и `baseline.profm` — 541 Б.
- Исходный `app/build/intermediates/merged_art_profile/benchmark/.../baseline-prof.txt` — **5107 правил**,
  и **0** строк с `com/koch/weather`. Файл начинается с `# Baseline profile rules for androidx.compose.material3`.
- Ни `baselineProfile`-плагина, ни модуля `macrobenchmark`, ни `app/src/main/baseline-prof.txt` в проекте нет
  (grep по `*.kts`/`*.toml` — пусто; `settings.gradle.kts` — только `:app`).

То есть AOT-компилируются Compose/AndroidX, а **`MainActivity`, `MainScreen`, `MainScreenViewModel`,
все ваши composable'ы и `KochSkyBackground`** исполняются JIT'ом на первом кадре — это и есть 486 + 524 мс
из §3. Стартовый профиль — самая крупная одиночная победа по холодному старту на этом устройстве.

Кому: `device-qa` + `ui-coder` (генерация), решение о модуле — за оркестратором.
Как: подключить `androidx.baselineprofile` (AGP 8.2+, `profileinstaller` уже в APK) и сгенерировать
стартовый профиль на G8342 (не на эмуляторе!) с покрытием `MainActivity.onCreate` → первый кадр →
скролл главной, открытие листа, открытие карты. Ожидание: −20…−35 % к времени до первого кадра,
то есть 1.1 с → ~0.7–0.85 с, и минус `Davey`/`Skipped 40 frames` на старте.

### P0-2. R8 выключен, dex 48 МБ в трёх файлах

Факты: `app/build.gradle.kts:34` (`release.isMinifyEnabled = false`), `:47` (benchmark — тоже false).
В APK: `classes.dex` 32.0 МБ + `classes2.dex` 10.3 МБ + `classes3.dex` 5.8 МБ = **48.1 МБ декса**
(сжатый APK 14.2 МБ). Причина раздутия — `material-icons-extended` (`build.gradle.kts:94`),
OkHttp, osmdroid, Coil, kotlinx-serialization — всё без шринка.

Загрузка/верификация 48 МБ декса сама по себе стоит времени на старте (класслоадинг + JIT).
Рекомендация: включить `isMinifyEnabled = true` + `isShrinkResources = true` именно в `benchmark`
(и в `release`), с keep-правилами для kotlinx.serialization и osmdroid. Ожидание: dex ~8–12 МБ,
ускорение старта 100–250 мс, плюс меньше JIT-работы на первых кадрах. Это дешевле baseline profile
по трудозатратам, но **не заменяет** его.
Кому: `device-qa` (сборка) + `code-reviewer` (проверка, что ничего не отвалилось рефлексией).

### P0-3. `CloudVeilOverlay`: полноэкранный offscreen + до 220 кругов `BlendMode.Clear` за кадр

`ui/components/CloudVeilOverlay.kt`:
- `:62` — `graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)` на **весь экран**;
  это отдельный 1080×1920 render target (в дампе — `Scratch RenderTarget 7.97 MB`), который
  композитится каждый кадр, пока вуаль жива.
- `:55` — `mutableStateListOf<Offset>()`; `:83-89` — на каждое drag-событие добавляется до **16** элементов,
  каждый `add` — запись snapshot-состояния → инвалидация `Canvas`.
- `:90-92` — обрезка до 220 элементов (`removeAt(0)` — O(n) сдвиг на каждое событие).
- `:154-163` — на **каждый кадр** до **220** `drawCircle(blendMode = BlendMode.Clear)`; `Clear` требует
  saveLayer/blend и на Adreno — дорогая операция.
- `:108-165` — вдобавок 1 вертикальный градиент + 4 радиальных овала, и всё это внутри `clipRect`.

Итог: пока пользователь стирает облака, кадр стоит «220 Clear-кругов + 5 градиентов + полноэкранный
layer». Это же объясняет, почему медиана GPU 10 мс и почему 14 % кадров промахиваются.

Рекомендации `ui-coder`:
1. Рисовать маску стирания **в один `Path`** (или в `Path` из объединённых кругов) и делать один
   `drawPath(..., blendMode = Clear)` на кадр вместо 220 кругов.
2. Не держать `CompositingStrategy.Offscreen` на весь экран: вуаль занимает только верхние 45 %
   (`veilBottom = size.height * 0.45f`, `:109`) — достаточно `Modifier.drawWithContent` + `saveLayer`
   на область вуали, либо отрисовать облака в `ImageBitmap`/`Picture` один раз и стирать по ней.
3. Хранить точки в обычном `ArrayList` + `mutableIntStateOf(version)`, а не в `mutableStateListOf`;
   добавлять не 16, а 2–4 штампа на событие (шаг 26 dp — избыточен, хватает 40–48 dp).
4. Ограничить историю 80–100 точками (визуально неотличимо, стоимость втрое ниже).

### P0-4. Палитра темы пересоздаётся на каждую рекомпозицию → полное дерево перерисовывается

`ui/theme/Theme.kt`:
- `:132-190` `derived(...)` — **аллоцирует новый `KochColors` (29 полей + `Brush`-геттер) при каждом вызове**
  для FOREST/MARS/CYBERPUNK/BLOODY_SUNSET/AURORA.
- `:219` `LocalKochColors = staticCompositionLocalOf { SkyBlue }`, `:221-224` `ProvideKochColors(mode)`.
- `:51-53` `val gradient: Brush get() = Brush.verticalGradient(...)` — **новый `Brush` на каждое чтение**.
- `:204-216` `getThemePalette()` — новый `CustomThemeColors` + новый `Brush` на каждый вызов;
  вызывается в `MainScreen.kt:151` и `MainScreen.kt:353` (в теле каждого рекомпонуемого экрана).

`staticCompositionLocalOf` не отслеживает читателей: смена значения **рекомпозирует весь content-lambda**.
Для BLUE_SKY/COPPER возвращается константа — значение не меняется. Для остальных пяти тем `derived()`
возвращает **новый экземпляр на каждую рекомпозицию `MainScreen`**, то есть на **каждое** обновление
`uiState` (таймер облаков через 15 с, сетевые колбэки, рейтинг, любой тап) — **всё дерево UI
перекомпоновывается целиком**. Это прямой кандидат в «почему jank не нулевой в покое».

Рекомендации `ui-coder`:
1. Сделать палитры `val`-константами (или `private val Palettes = mapOf(mode to ...)`, посчитанными один раз).
2. `gradient` — предвычисленное поле, а не геттер.
3. `getThemePalette` — `remember(mode) { ... }` на уровне `MainScreen`, а не вызов в теле.

### P0-5. `MainScreenUiState` — один нестабильный объект на 39 полей; лента перекомпонируется целиком

`data/model/WeatherModels.kt:231-319` — `MainScreenUiState` с `List<>`-полями (`cities`, `savedCities`,
`themeCatalog`, `imageUrls`, `searchResults`, `citySummaries`), без `@Immutable`.
`WeatherDomainData:113-134` — то же (`List<HourlyItem>`, `List<DailyItem>`).
`ui/main/MainScreen.kt:330` — `MainScreenContent(uiState: MainScreenUiState, …)`.

Формально в Kotlin 2.3.20 strong skipping включён по умолчанию, и Composable с нестабильными параметрами
пропускается по instance-равенству. Но `_uiState.update { … }` создаёт **новый** `MainScreenUiState` на
каждое событие (`MainScreenViewModel.kt:72, 103, 120, 146, 189`), поэтому пропуска не будет никогда:
каждая смена любого поля перерисовывает шапку, часовой ряд, 10 дней, сервисы, промо и футер.

Плюс `MainScreen.kt:561-583`: список `feedBlocks` из лямбда-блоков создаётся заново в каждом проходе
и передаётся в `item(key = key) { block() }` — лямбды захватывают `weather`/`uiState` (нестабильные),
поэтому не мемоизируются, и содержимое item'ов рекомпонируется вместе с родителем.

Рекомендации `ui-coder` + `data-coder`:
1. `data-coder`: разбить состояние на несколько `StateFlow` по секциям (`weather`, `rating`, `services`,
   `navigation`) или хотя бы на `@Immutable`-подсостояния; для списков — `kotlinx.collections.immutable`
   (`ImmutableList`) либо `@Immutable`-обёртки.
2. `ui-coder`: `MainScreenContent` должен принимать **не весь** `uiState`, а секции; `feedBlocks`
   заменить на `when`-ветки прямо в LazyColumn (без списка лямбд), либо `remember`-мемоизировать блоки
   по ключевым полям.
3. Мелочь, но бесплатная: `LazyColumn` (`MainScreen.kt:416`) уже с ключами — это правильно, не ломать.

### P1-6. Полноэкранные табы рисуются **поверх живой главной**

`MainScreen.kt:96-141` — `MainScreenContent(...)` вызывается всегда, а `when (uiState.selectedTab)`
(`:152-226`) добавляет `WeatherMapScreen`/`CityListScreen`/`CitySearchScreen`/`ThemeGalleryScreen`
**соседними узлами**. Compose не отсекает перекрытый контент: пока открыта карта, каждый кадр
дополнительно рисуются фон-градиент, 5 радиальных облаков и лента главной.

Рекомендация `ui-coder`: не компонить главную, когда открыт полноэкранный таб
(`if (tab == HOME) MainScreenContent(...)`), либо вынести табы в отдельный навигационный слой
(Navigation-Compose/AnimatedContent). Это прямо уменьшит p95 на карте и в списке.

### P1-7. `WeatherMapScreen`: `MapView` создаётся на main-потоке вместе с дисковым I/O

`ui/components/WeatherMapScreen.kt`:
- `:66-83` — `remember { Configuration.getInstance().apply { load(context, …) } ; MapView(context) … }`.
  `Configuration.load` читает SharedPreferences (диск), конструктор `MapView` поднимает tile-провайдер и
  SQLite-кэш тайлов (`SqlTileWriter`) — **всё синхронно в композиции**, то есть на первом кадре карты.
- `:78` `setTilesScaledToDpi(true)` — тайлы масштабируются на GPU (лишняя работа + мыло).
- `:85-98` — `LaunchedEffect(latitude, longitude, cityName, currentTemp)`: на каждое изменение
  `removeAll { it is Marker }` + новый `Marker` + `invalidate()`. При обновлении `uiState` (например,
  `weather.currentTemp` перечитался) маркер пересоздаётся.
- `:110-113` — `mapView.onDetach()` в `onDispose`, но при возврате на карту создаётся **новый** `MapView`.

Рекомендации:
1. `Configuration.load(...)` — один раз в `Application.onCreate` (или в фоне при старте), не в композиции.
2. `MapView` — один экземпляр на процесс (держать в `remember` уровня `MainScreen` или в `ViewModel`),
   тогда повторное открытие карты бесплатно.
3. `setTilesScaledToDpi(false)` (или `true` только для мелких экранов) — проверить визуально с
   `visual-critic`, чтобы не сломать refs.
4. Маркер обновлять точечно (`marker.position = …; mapView.invalidate()`), а не пересоздавать.

### P1-8. `SubcomposeAsyncImage` + `crossfade` + меню еды не ленивое

`ui/components/WeatherImage.kt:80-88` — `SubcomposeAsyncImage` (субкомпозиция на каждое фото: дороже
`AsyncImage`, лишний layout-проход), `:54` `.crossfade(true)` (анимация альфы на каждое появление).
`ui/components/FoodBottomSheet.kt:134-144` — меню еды рисуется **`Column` + `forEach`**, не `LazyColumn`:
при открытии листа одновременно поднимаются все позиции и все их фото.
`data/model/ImageModels.kt:46,53,60,…` — тайлы запрошены в **960 px**, при том что слот фото в меню — 
72 dp (`KochSize.thumb`), то есть ~180 px на 2.5x. Скачиваем в ~5 раз больше, чем нужно.

Рекомендации:
1. `RemotePhoto`: `AsyncImage` вместо `SubcomposeAsyncImage`, `crossfade(false)` для списков, размеры —
   явным `Modifier.size()` (уже есть) и `precision = INEXACT`.
2. URL'ы Wikimedia — на 240 px (`/240px-…`) для 72 dp-слотов и 480–640 px для hero; качество не пострадает.
3. Меню еды — `LazyColumn`; фото-герой листа — грузить после первого кадра (не в первой композиции).

### P2-9. Мелочи, которые складываются

| Место | Проблема | Рекомендация |
|---|---|---|
| `ui/design/KochCard.kt:75` | `topHighlightBrush()` — новый `Brush` на каждую рекомпозицию **каждой** карточки | `private val TopHighlight = Brush…` на уровне файла |
| `ui/design/KochSheet.kt:241` | то же для листов | то же |
| `ui/design/KochSheet.kt:62-70` | каждый лист — отдельное окно `Dialog` + скрим 45 % + `shadowElevation = 24dp` (`:114`): дорогой первый кадр листа | замерить (§8); при провале — рисовать лист внутри основного окна |
| `ui/design/KochNavBar.kt:62` | `shadowElevation = 12.dp` на полупрозрачной пилюле через весь экран | тень в RenderNode на GPU; при статичном баре не критично, но на каждый кадр при скролле — да |
| `ui/main/MainScreen.kt:353` | `getThemePalette()` в теле композиции → новый `CustomThemeColors` + `Brush` | `remember(uiState.currentTheme)` |
| `ui/design/KochBackground.kt:44-61` | 5 радиальных овалов на фоне, перерисовываются при каждой инвалидации Canvas | кэшировать в `drawWithCache`/`ImageBitmap`; фон статичен |
| `ui/components/WeatherIcons.kt:59-74` | `Canvas` на каждую иконку: радиальный градиент + 8 лучей; до 25 элементов в часовом ряду (видны ~6) + 10 дней | ~16 видимых Canvas с градиентами при скролле; можно `ImageVector` вместо Canvas |
| `ui/components/LootboxDialog.kt:58-66` | `rememberInfiniteTransition` (пульс) — перерисовка 60 Гц пока открыт лутбокс | остановить анимацию вне видимости/при закрытии |
| `MainScreen.kt:373-390` | горизонтальный drag на весь экран конкурирует с вуалью и LazyColumn | не перф, но из-за него облака не стираются с первого раза → повторные свайпы (лишние кадры) |

---

## 7. Вердикт R18 — предварительный

| Критерий R18 | Факт | Вердикт |
|---|---|---|
| p50 кадра ≤16 мс | **11 мс** (199 кадров, benchmark) | **PASS** по этому окну |
| 60 FPS | 87.4 % кадров ≤16 мс, но 28 janky, `High input latency 181/199` | **FAIL** |
| release-подобная сборка (`isDebuggable=false`) | benchmark, non-debuggable — подтверждено отчётом device-qa | **PASS** |
| «без системного jank» | jank 14.07 %; на каждом холодном старте `Davey 1.1 с` + `Skipped 40–44 frames`; пауза 1.9 с в записи | **FAIL** |
| По сценариям (листы/карта/тема/фото) | данных нет | **НЕ ПРОВЕРЕНО** |

**Предварительный вердикт по R18: FAIL** (не может быть PASS, пока jank 14 % и фризы 1.1 с на старте
воспроизводятся, и пока 4 из 6 сценариев не измерены). `p50 ≤16 мс` — единственный выполненный пункт,
и он выполнен с запасом 1–2 мс по GPU, то есть хрупко.

Что даст максимум, по убыванию отдачи:

1. **Baseline profile для кода приложения** — лечит ровно тот 1.1 с первого кадра (§3, §6.P0-1).
   Это единственная правка, которая сокращает и «до traversal» (486 мс JIT/класслоадинг), и draw.
2. **R8 + shrink в benchmark/release** — 48 МБ декса → ~8–12 МБ, минус класслоадинг и JIT (§6.P0-2).
   Дешевле профиля, но не убирает JIT Compose-стека.
3. **Убрать полноэкранный `Offscreen` в `CloudVeilOverlay`** — самая дешёвая правка ради GPU-медианы
   (сейчас 10 мс из 16.6). Ожидаемо возвращает 3–5 мс на кадр, пока вуаль на экране.
4. **Закэшировать палитры + разбить `uiState`** — убирает полные рекомпозиции дерева «на ровном месте»
   (5 тем из 7 сейчас перекомпоновывают всё на каждый таймер/сетевой колбэк).
5. **Не компонить главную под картой/списком** — прямо режет p95 на карте.

Чего **не** делать: не отключать анимации и не убирать тени/градиенты ради FPS — это ломает refs
(правило роли). Все правки выше сохраняют визуал; единственный пункт, который нужно согласовать с
`visual-critic`, — `setTilesScaledToDpi(false)` на карте.

---

## 8. Что осталось измерить (план окна замеров)

Порядок и команды (все дампы — в `shots/perf_*.txt`, в конце `am force-stop`):

1. Честность сборки:
   `adb shell dumpsys package com.koch.weather | findstr /i "versionName flags DEBUGGABLE"`;
   `adb shell pm path com.koch.weather`.
2. Холодный старт ×3: `am force-stop` → `adb shell am start -W -n com.koch.weather/.MainActivity`
   (×3) + `adb logcat -d | findstr Davey` после каждого; ожидаю те же ~1.1 с фриза.
3. Скролл главной (чистое окно): `gfxinfo reset` → 8 свайпов `input swipe 540 1500 540 500 250`
   → `gfxinfo` → `shots/perf_scroll.txt`.
4. Листы: по одному `reset` на лист (такси/еда/лутбокс/пейволл/рейс), открытие тапом, 1 свайп внутри,
   `gfxinfo` → `shots/perf_sheet_taxi.txt` и т. д.
5. Карта: `reset` → таб «Карта» → 2 свайпа/зум пинчем (`input swipe` с 2 пальцами невозможно — зум
   через кнопки/двойной тап) → `gfxinfo` → `shots/perf_map.txt`.
6. Смена темы: `reset` → рулетка → «Продолжить» → `gfxinfo` → `shots/perf_theme.txt`; отдельно
   проверить **Изумрудный лес** (не константа в `derived()`) — там ожидаю худший результат.
7. Фото: `reset` → лист еды → дождаться загрузки 4 фото → `shots/perf_photos.txt` + logcat на
   `Bitmap`/`Coil`/`Slow bitmap uploads`.
8. Вуаль: `reset` → дождаться таймера 15 с → 4 свайпа стирания → `gfxinfo` → `shots/perf_veil.txt`.
9. Завершение: `adb shell am force-stop com.koch.weather`, `pidof` пуст.

После окна: дозаполнить §1, пересчитать вердикт §7, обновить заголовок отчёта со «ЧАСТИЧНЫЙ» на итоговый
и отправить результат оркестратору.

---

## 9. Ограничения этого отчёта (BLOCKED-часть)

- **Device-замеры по 5 из 6 сценариев не сняты** — по прямому указанию оркестратора, из-за параллельного
  прогона `feature-qa`. Это **не** PASS и **не** «не проверено навсегда» — это отложенный замер.
- `dumpsys package` и `am start -W` мной **не** выполнялись; цифры §3 взяты из `_history/reports/phase-4/device-qa.md`
  (Run 3) и лога `shots/logcat_run3.txt`, что в отчёте помечено.
- Установленный APK не содержит правку `WeatherProvider.kt` из рабочего дерева; после пересборки
  сетевой тайминг холодного старта может измениться (в лучшую сторону), кадровые метрики — нет.
- Замеры p50/p95 на 199 кадрах статистически тонкие: 1 кадр ≈ 0.5 п.п. jank и может двигать p95 на
  десятки мс. Для вердикта нужны окна ≥400 кадров на сценарий.
