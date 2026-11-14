# ACCEPTANCE — матрица требований KOCHWeather

> Ведёт `judge`. Обновляется после каждой фазы. PASS без пути к доказательству не ставится.
> «Не проверено» и «вне фазы» — не PASS. Исторические PASS прошлых фаз не переносятся
> на текущую: UI мог регрессировать.

## Как пользоваться

- `ID` — стабильный номер требования; не перенумеровывать.
- `Проверяет` — основная роль; `judge` делает выборочный spot-check, а не полный аудит.
- `Доказательство` — путь к скриншоту, логу, отчёту или APK. Без пути статус не PASS.
- Для R12–R15 в `refs/` нет кадров: критерий — состав и поведение, а не пиксель-в-пиксель.

## Итог фазы 4 (judge, 2026-11-14 11:03 MSK)

**Вердикт: REJECT.** Финальный benchmark `44bc6d6` собран 10:50:46 и установлен `-r` в 10:52:46,
но после `adb reboot` телефон ушёл в secure-lock: кадров, logcat и замеров на этой сборке нет.
Доказательства ниже сняты на `05ee399` (r10), `cbef2d1` (r9), `8be22d8` (r7/r8) и `b8c7290` (fqf);
это старше последних правок `dc44ea0` (подсказка облаков + бейдж «Доступно») и `44bc6d6` (тарифы такси).

- PASS ролей: `code-reviewer` — PASS по дельте M1/M2 (144 теста, HEAD `05ee399`); `visual-critic` —
  FAIL по дельте (F-3); `perf-qa` — FAIL по R18; `feature-qa` — PASS только на `b8c7290`.
- Блокеры приёмки: R18 (чистый замер), R10/F-3 (видео), R6 (кадр рулетки после `dc44ea0`),
  R8 (тарифы такси после `44bc6d6`), R22 (logcat финальной сборки), ретест `feature-qa` на финальном APK.
- 7timer: сценарий закрыт кадром `shots/r7_22b_source_7timer.png` на `8be22d8` (футер «Данные: 7timer»);
  пометка BLOCKED в `feature-qa-final.md` устарела, но role-PASS `feature-qa` всё равно нужен на финальной сборке.

## Матрица

| ID | Требование | Критерий приёмки | Проверяет | Доказательство | Статус |
|----|------------|------------------|-----------|----------------|--------|
| R1 | Чистый главный экран | `refs/clean_main.png`: шапка (ТЕКУЩЕЕ МЕСТО, город, температура, состояние, макс/мин), почасовой ряд, 10 дней, порядок блоков | visual-critic | `shots/r9_01_main_blue_status.png`, `shots/r9_02_main_copper_status.png`, `shots/r7_02_scrolled_footer.png` | **BLOCKED**: шапка и порядок — r9_01/r9_02; почасовой ряд и 10 дней только r7_02 (до `cbef2d1`, `TenDayForecastCard`); нужен скролл-кадр на финальном APK |
| R2 | Сегменты условий | `refs/segments.png`: 5 сегментов (Погод. усл., Осадки, Ветер, Одежда, Пробки), контент переключается | visual-critic, feature-qa | `shots/r7_19_segment_rain.png`, `shots/r7_20_segment_wind.png`, `shots/r9_01_main_blue_status.png` | **PASS**: 5 чипов, «Осадки»/«Ветер» переключаются, 4.9 м/с — runtime M1 на `8be22d8`; код сегментов после не менялся |
| R3 | Оценка погоды и зонтики | `refs/rate_stars.png`: 3.5★, подсвечены 3 звезды, строка «Зонтики от 29.99 BYN / Смотреть ›» | visual-critic, feature-qa | `shots/r7_22b_source_7timer.png`, `shots/r7_19_segment_rain.png`, `shots/r7_20_segment_wind.png` | **PASS**: 3.5★, 3 звезды, «Зонтики от 29.99 BYN / Смотреть ›»; `RatingCard`/`UmbrellaRow` после `8be22d8` не менялись |
| R4 | Таб-бар и темы | `refs/bottom_nav.png`, `refs/theme_copper.png`: состав табов, активное состояние, читаемость на голубой и медной теме | visual-critic | `shots/r9_01_main_blue_status.png`, `shots/r9_02_main_copper_status.png` | **PASS**: 5 табов, активная «Главная», читаемость на обеих темах |
| R5 | Путешествия и лутбоксы | `refs/cards_travel.png`: сетка 2×2, тексты RU, белый контрастный текст лутбоксов, нет ухода под бар | visual-critic, feature-qa | `shots/r9_01_main_blue_status.png`, `shots/r9_02_main_copper_status.png` | **PASS**: сетка 2×2, RU-тексты, контраст, без ухода под бар; кадры после `cbef2d1` |
| R6 | Рулетка тем / «Поздравляем!» | `refs/congrats.png`: iPhone, жёлтые стрелки ◀ ▶, чипы тем, «Доступно ✓» не обрезано, «Продолжить» | visual-critic, feature-qa | `shots/r9_03_roulette.png`, `_history/reports/phase-4/visual-critic-delta.md` | **BLOCKED**: раскладка PASS (телефон 338×460 px, стрелки, чипы, «Доступно» над чипами, «Продолжить»), но r9_03 старше `dc44ea0`: старый бейдж 1.27:1; нужен новый кадр |
| R7 | Медная тема | `refs/theme_copper.png`: палитра, карточки, таб-бар, контраст текста | visual-critic | `shots/r9_02_main_copper_status.png`, `shots/r9_05_list_footer.png` | **PASS**: палитра, карточки, таб-бар, контраст; кадры после `653fc0f`/`cbef2d1` |
| R8 | Такси и еда | `refs/cards_taxi_food.png`: карточки 2×2, чипы «Куда едем? / Что желаете?», открытие шторок | visual-critic, feature-qa | `shots/r10_02_taxi.png`, `shots/r10_03_food.png`, `shots/r10_04_flight.png`, `shots/r10_05_flight_from_travel.png` | **BLOCKED**: еда/лутбоксы/билеты — r10_03–r10_05; такси после `44bc6d6` (LazyRow→Row) не снято; риск выхода третьего тарифа за content-width 335.7 dp |
| R9 | Премиум, реклама, AI-факт | `refs/premium_ads.png`: «Чтобы точно узнать погоду, нужно посмотреть в окно», 5.99 BYN, зелёный баннер «РЕКЛАМА» | visual-critic, feature-qa | `shots/r7_03_ad_banner.png`, `shots/r7_10_premium.png`, `shots/r7_11_premium_confirm.png`, `shots/r7_23_after_purchase.png` | **PASS**: реклама целиком, пейволл, премиум без баннера; ad/paywall после `8be22d8` не менялись |
| R10 | Облака пальцем | `refs/cloud_swipe.png`: облака закрывают шапку, жест открывает погоду, подсказка «Прогоните облака пальцем» | visual-critic | `shots/r9_07_veil.png`, `shots/r9_08_after_swipes.png`, `shots/dynamics/r9_clouds.mp4`, `app/src/main/kotlin/com/koch/weather/ui/components/CloudVeilOverlay.kt` | **BLOCKED**: статика r9_07 PASS; фикс F-3 в `dc44ea0` (`onDragEnd` не возвращает hint), но видео снято до фикса и показывает наложение; нужно новое видео |
| R11 | Блокировка «Пора на улицу!» | `refs/grass_lock.png`: тексты, зона загрузки фото, синяя кнопка «Открыть камеру», без крестика | visual-critic, feature-qa | `shots/r7_12_grass.png` | **BLOCKED**: r7_12 на `8be22d8`, после него менялся `KochSheet` (`cbef2d1`/`05ee399`); нужен свежий кадр шторки травы |
| R12 | Экран «Карта» | Нет ref: настоящая карта OSM (osmdroid), центр = координаты города, зум, маркер; работает из РФ без VPN и ключа | feature-qa | `shots/r7_15_map.png`, `shots/fqf_04_map.png`, `shots/fqf_05_map_marker.png`, `shots/fqf_07_pinch.png`, `shots/fqf_provider_curl.txt` | **PASS**: OSM-тайлы, центр СПб, маркер и info-window, зум; curl OSM 200 без VPN/ключа; код карты после не менялся |
| R13 | Экран «Список» | Нет ref: полноэкранный список сохранённых городов с температурой, переход на город | feature-qa, visual-critic | `shots/r9_05_list_footer.png`, `shots/fqf_01_list.png`, `shots/fqf_36.xml` | **PASS**: полноэкранный список, температуры, «Нет данных», футер; r9_05 после `653fc0f`; переход на город — fqf_36 |
| R14 | Экран «Поиск» | Нет ref: полноэкранный поиск, живой geocoding Open-Meteo, выбор города | feature-qa | `shots/r7_17_search.png`, `shots/fqf_02_search.png`, `shots/fqf_02_search_results.png` | **PASS**: полноэкранный поиск, живая выдача Moscow, выбор города; код поиска после не менялся |
| R15 | Экран «Вид» | Нет ref: полноэкранный выбор оформления (темы из лутбоксов + рулетка) | feature-qa, visual-critic | `shots/r7_14_view.png`, `shots/fqf_08_view.png`, `shots/fqf_09_theme_blue_view.png`, `shots/fqf_17_persist.png` | **PASS**: «Оформление», 6 тем, бейджи, применение и персист; `ThemeGalleryScreen` после `8be22d8` не менялся |
| R16 | Deep links | Яндекс Go: маршрут по координатам; Яндекс Еда; веб-фолбэк, если приложения нет | feature-qa | `shots/fqf_19_taxi_sheet.png`…`fqf_23_taxi_chrome.png`, `fqf_25_food_sheet.png`…`fqf_29_food_chrome.png`, `shots/fqf_intents.txt`, `shots/r10_02_taxi.png` | **PASS**: такси/еда, подтверждение, VIEW-intent и веб-фолбэк; intent-код не менялся в `44bc6d6` |
| R17 | Реальные фото | Coil по URL, источники доступны из РФ (Wikimedia Commons), плейсхолдер при загрузке/ошибке | feature-qa, visual-critic | `shots/r7_07_food.png`, `shots/r7_09_flight.png`, `shots/r10_03_food.png`, `shots/r10_04_flight.png`, `shots/fq_sources_device.txt` | **BLOCKED**: фото грузятся, источник доступен, но плейсхолдер при загрузке/ошибке runtime не проверен; `feature-qa` R17 не тестировал |
| R18 | Производительность | p50 кадра ≤16 ms на G8342, 60 FPS, release-подобная сборка (`isDebuggable=false`), без системного jank | perf-qa | `shots/perf_final_scroll.txt`, `shots/perf_run10_scroll.txt`, `shots/perf_run12_scroll_clean.txt`, `_history/reports/phase-4/perf-qa-final.md` | **FAIL**: p50 5–10 мс PASS, но jank 23–27 % на скролле; чистый замер не снят — `perf_run12_scroll_clean.txt` 43 байта, «No process found»; loadavg 4.2–4.8 |
| R19 | Оффлайн | Кэш последнего прогноза в DataStore, баннер «нет сети», скрытие при восстановлении | feature-qa | `shots/r7_21_offline.png`, `shots/r7_22_after_online.png`, `shots/r7_22b_source_7timer.png`, `shots/fqf_39_offline.png`, `shots/fqf_40_offline_footer.png`, `shots/fqf_42_online.png`, `shots/fqf_43_online_footer.png`, `shots/fqf_cache_before_offline.pb`, `shots/fqf_cache_after_online.pb` | **PASS**: баннер кэша → оффлайн → онлайн без перезапуска; 7timer end-to-end; r7_21/r7_22/r7_22b на `8be22d8`, data-слой после не менялся |
| R20 | Города | Санкт-Петербург по умолчанию, затем Москва; поиск и переключение | feature-qa | `shots/fqf_00_launch.png`, `shots/fqf_02_search_results.png`, `shots/fqf_03_moscow.png`, `shots/r7_17_search.png` | **PASS**: СПб по умолчанию, поиск Moscow за 4.2 с, выбор Москвы; код городов после не менялся |
| R21 | Без заглушек и фейков | Нет TODO/заглушек; при отсутствии данных — «нет данных», а не выдуманные цифры | code-reviewer, feature-qa | `shots/fqf_01_list.png`, `shots/fqf_32_dubai.png`, `shots/fqf_35_dubai_traffic.png`, `shots/r9_05_list_footer.png`, `shots/r7_20_segment_wind.png` | **PASS**: «Нет данных» без фейковых цифр; ветер в м/с на `8be22d8`; M1/M2 закрыты кодом и тестами |
| R22 | Сборка и стабильность | `assembleDebug` OK, install на G8342 OK, logcat без крэшей/ANR, `testDebugUnitTest` PASS | device-qa, code-reviewer, judge | `shots/build_run12_benchmark.txt`, `_history/reports/phase-4/device-qa.md`, `shots/logcat_run10.txt`, `_history/reports/phase-4/code-reviewer-delta.md` | **BLOCKED**: benchmark `44bc6d6` собран и установлен 10:52:46, но приложение не запускалось и logcat не снят (secure-lock); `assembleDebug` последний на `8be22d8`; unit-тесты 144 PASS на `05ee399` |
| R23 | RU-тексты и отсутствие обрезаний | Все тексты по-русски, дословно; нет обрезаний/наложений на всех экранах фазы | visual-critic | `shots/r9_01_main_blue_status.png`, `shots/r9_02_main_copper_status.png`, `shots/r9_04_daily_detail.png`, `shots/r9_05_list_footer.png`, `shots/r9_07_veil.png`, `shots/r9_08_after_swipes.png` | **BLOCKED**: F-1/F-2/F-4 закрыты (r9_01/02/04/05), но F-3 не подтверждён видео, а тарифы такси после `44bc6d6` не пересняты; полного прохода «без обрезаний» на финальном APK нет |

## Открытые пункты фазы 4 (REJECT)

| ID / роль | Что именно закрыть | Чем подтвердить |
|---|---|---|
| R18 / `perf-qa` | Чистый замер скролла после ребута: loadavg рядом, ≥300 кадров, p95 ≤33 мс, jank ≤10 %. Сейчас `perf_run12_scroll_clean.txt` — 43 байта, «No process found»; последние 23–27 % jank сняты при loadavg 4.2–4.8 | `shots/perf_run12_scroll_clean.txt` + `gfxinfo` + `logcat`; при jank >15 % — правка скролл-пути и повторный замер |
| R10 / F-3 / `visual-critic` | Фикс `dc44ea0` в коде есть, но видео на финальном APK нет. Нужно 6–8 с, 4–5 штрихов: подсказка гаснет на первом касании и не появляется поверх капсулы; город остаётся СПб | `shots/dynamics/r12_clouds.mp4` + 8–10 кадров по PTS + `r12_03_veil_hint_first` |
| R6 / `visual-critic` | Новый кадр рулетки после `dc44ea0`: «Доступно» — белая пилюля с тёмным текстом, над чипами, не обрезано; телефон 118×160, жёлтые ◀ ▶ | `shots/r12_04_roulette.png` (или r13) |
| R8 / `visual-critic` | Кадр шторки такси после `44bc6d6`: все 3 тарифа видны, третий не выходит за правый padding. Арифметика: 3×112 + 2×8 = 352 dp при content-width ≈335.7 dp (375.7 dp экран − 2×20 dp), в комментарии кода ошибочно 353 dp | `shots/r12_05_taxi.png`; если тариф обрезан — вернуть LazyRow/веса и пересобрать |
| R1, R11, R17 / `visual-critic`, `feature-qa` | R1: скролл-кадр с почасовым рядом и 10 днями; R11: свежий кадр шторки травы; R17: runtime-проверка плейсхолдера при загрузке/ошибке фото | `r12_01_main_scrolled`, `r12_06_grass`, отчёт `feature-qa` по R17 |
| R12–R16, R19–R21 / `feature-qa` | PASS относится только к `b8c7290`. Нужен ретест на финальном APK: оффлайн/онлайн R19 (7timer уже подтверждён `r7_22b` на `8be22d8`), ветер м/с R2/R21, «Нет данных» R21, smoke R12–R16 | новый `_history/reports/phase-4/feature-qa-final.md` с APK `44bc6d6`/новее |
| R22 / `device-qa` | Запустить финальную сборку после разблокировки, снять `logcat` без FATAL/ANR, пересобрать `assembleDebug` на HEAD | `shots/logcat_run12.txt`, `shots/build_run12_debug.txt`, `dumpsys package` |
| M1/M2 / `code-reviewer` | PASS-дельта покрывает `05ee399`; `dc44ea0` и `44bc6d6` (UI-only) не ревьюились. Нужна короткая дельта-подпись | `_history/reports/phase-4/code-reviewer-delta.md` (дополнение) |

## Правила обновления

1. PASS ставит `judge` только при наличии пути к доказательству в колонке «Доказательство».
2. Если требование вне фазы — статус «вне фазы» с причиной, а не PASS.
3. Новое требование — новый ID; старые ID не переиспользовать.
4. Для R12–R15 критерий — поведение/состав, а не совпадение с несуществующим ref.
5. Перед PASS `judge` проверяет свежесть: доказательства сняты после последнего изменения кода.
