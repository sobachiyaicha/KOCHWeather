# feature-qa — phase 4, финальный прогон (feature-qa-final)

**Дата/время доказательств:** 2026-11-14, **08:30:19 – 09:00:02 MSK** (окно прогона; все скриншоты/логи ниже сняты в этом окне).
**Устройство:** Sony G8342 (CB512FQDXN), Android 9, root (Magisk).
**Роль:** feature-qa. Код не правил, коммитов не делал, APK **не переустанавливал**, DataStore вручную не редактировал (только read-only копии через root). adb — только тестовые действия.

## 0. Привязка к сборке (снято до/во время тестов)

```
versionName=1.0-benchmark
versionCode=1
lastUpdateTime=2026-11-14 08:22:35
firstInstallTime=2026-11-10 14:43:58
```

- На телефоне — benchmark-APK, собранный из **HEAD `b8c7290`** («fix(ui): open marker info window and raise contrast of labels over sky photo»), установлен device-qa в 08:22:35 без `pm clear` (DataStore-кэш живых данных сохранён).
- `git rev-parse --short HEAD` на момент старта прогона: **`957d845`** = `b8c7290` + docs-only коммит со `shots/`/отчётом run 6 (код не менялся; проверено `git diff --name-only b8c7290 957d845` — только `_history/` и `shots/`).
- **ВАЖНО:** к моменту сдачи отчёта рабочее дерево ушло вперёд: **HEAD `8be22d8`** («fix(data): metric wind units and reserved fallback budget; fix(ui): final audit fixes…»). Этот код **не входит в протестированный APK**. После финальной пересборки нужен **ретест R1–R23**, в первую очередь R19, 7timer (изменён reserved fallback budget), ветер/метрика и визуальные фиксы. Всё ниже относится **только к APK `b8c7290` / lastUpdateTime 08:22:35**.

## 1. Итог по матрице (запрошенные ID)

| ID | Статус | Кратко | Доказательства |
|----|--------|--------|----------------|
| **R19** | **PASS** | Кэш → оффлайн → баннер с временем → сеть on → баннер исчез **после реального обновления**: lastUpdated 08:48:53 → 08:56:57, hash DataStore изменился, футер «Данные: MET Norway (api.met.no)» | `shots/fqf_00_launch.png`, `fqf_39_offline.png`, `fqf_40_offline_footer.png`, `fqf_41.xml`, `fqf_42_online.png`, `fqf_43_online_footer.png`, `fqf_cache_before_offline.pb`, `fqf_cache_after_online.pb` |
| **R20** | **PASS** | СПб по умолчанию; поиск «Moscow» живой (Photon→Nominatim) за **4.2 с** (≤12 с); выбор Москвы переключил город | `shots/fqf_00_launch.png`, `fqf_02_search.png`, `fqf_02_search_results.png`, `fqf_03_moscow.png` |
| **R12** | **PASS** | Полноэкранная OSM-карта, тайлы грузятся из РФ без VPN/ключа, центр = координаты города, зум (pinch), маркер + info-window по тапу | `shots/fqf_04_map.png`, `fqf_05_map_marker.png`, `fqf_07_pinch.png`, `fqf_provider_curl.txt` (OSM tile 200 / 33 904 B) |
| **R13** | **PASS** | Полноэкранный «Список городов», реальные температуры/«Нет данных», переход на город | `shots/fqf_01_list.png`, `fqf_36.xml`, `fqf_31.xml`, `fqf_37_spb_live.png` |
| **R14** | **PASS** | Полноэкранный «Поиск города», живой geocoding, выбор города | `shots/fqf_02_search.png`, `fqf_02_search_results.png`, `fqf_03_moscow.png` |
| **R15** | **PASS** | Полноэкранный «Вид», тема применяется; **медная → голубая пережила force-stop** | `shots/fqf_08_view.png`, `fqf_09_theme_blue_view.png`, `fqf_10_main_blue.png`, `fqf_17_persist.png` |
| **R16** | **PASS** | Такси и Еда: подтверждение → VIEW-intent (`dumpsys activity recents`) → веб-фолбэк Chrome; «Отмена» не запускает ничего | `shots/fqf_19`–`fqf_23`, `fqf_25`–`fqf_29`, `fqf_intents.txt` |
| **R21** | **PASS** | Город без данных — «Нет данных», «Нет данных о пробках» без цифры при null, при UNAVAILABLE — «Нет данных о погоде», выдуманных температур нет | `shots/fqf_01_list.png`, `fqf_35_dubai_traffic.png`, `fqf_32_dubai.png`, `fqf_19_taxi_sheet.png` |
| **7timer** | **PASS (частично) / BLOCKED сценарий** | Резерв **не активировался**, т.к. MET Norway жив (curl 200 / 0.16 с); 7timer-эндпоинт с телефона доступен (200 / 15 183 B). Сценарий «MET и Open-Meteo оба недоступны → „Данные: 7timer“» не воспроизведён | `shots/fqf_provider_curl.txt`, `fqf_provider_curl_dubai.txt` |
| **R22** | **PASS (feature-сторона)** | logcat 3 357 строк, **FATAL EXCEPTION / ANR / am_anr / E/AndroidRuntime — 0**; сборка/установка/unit-тесты — зона device-qa/code-reviewer | `shots/fqf_logcat.txt` |
| **Пейволл** | **PASS** | Покупка только после подтверждения; «Отмена» не меняет DataStore; premium активен и **пережил force-stop**; затем возвращён в исходное (не куплен) | `shots/fqf_13_paywall.png`, `fqf_14_paywall_confirm.png`, `fqf_15_paywall_cancel.png`, `fqf_16_premium_active.png`, `fqf_18_premium_persist.png`, `fqf_cache_precancel.pb`, `fqf_cache_final.pb` |

## 2. R19 — оффлайн/онлайн (главный сценарий) — PASS

Исходно: кэш живого MET Norway для СПб, `lastUpdated 08:09:46`; при запуске в 08:30 баннер «Сохранённый прогноз, 08:09» (`fqf_00_launch.png`) — кэш моложе 30 мин, поэтому отдаётся честно как CACHED.

Пошагово:
1. **08:55:14** — Wi-Fi и данные выключены (`wifi_on=0`, `Active default network: none`), приложение force-stop.
2. **08:55:26** — холодный старт оффлайн. На экране Дубай (последний город) с кэшем: **баннер «Сохранённый прогноз, 08:48»**, данные 27° / макс 29° / мин 25° не пропали — `fqf_39_offline.png`.
3. Футер оффлайн: **«Данные: MET Norway (сохранённый прогноз)»** + «Нет подключения к сети. Проверьте интернет и повторите.» — `fqf_40_offline_footer.png`.
4. `last_updated_time` в DataStore до = **1794635333300 = 08:48:53.300**, hash `a45fe7dc…` (`fqf_cache_before_offline.pb`).
5. **08:56:45** — включены Wi-Fi и данные (`Active default network: 239`). Баннер **не исчезает мгновенно**:
   - t=10.5 с: баннер «Сохранённый прогноз, 08:48» ещё на экране;
   - **08:56:57.298** — `last_updated_time` = **1794635817298** (новый, +8 мин 04 с от старого), hash сменился на `935b8b44…` (`fqf_cache_after_online.pb`);
   - t=17.7 с — баннер исчез (`fqf_42_online.png`).
6. Футер после обновления: **«Данные: MET Norway (api.met.no)»** + честная приписка «Open-Meteo недоступен: Не удалось подключиться к серверу погоды…» — `fqf_43_online_footer.png`. Т.е. обновление живое (MET), а не повторное чтение кэша; пометки «сохранённый прогноз» нет.

**Вывод:** баннер скрывается только после реального сетевого обновления; D1 прошлого прогона не воспроизводится. PASS.

## 3. R20 — города — PASS

- СПб по умолчанию при первом запуске: шапка «Санкт-Петербург / Облачно / 3° / Макс.: 4°, мин.: 2°» — `fqf_00_launch.png`.
- Поиск: вкладка «Поиск города», запрос `Moscow` → живая выдача «Москва, Россия», «Moscow, Idaho, United States», «Москва, Москва, Россия» — `fqf_02_search_results.png`. Время от ввода до появления «Москва» — **4.2 с** (замер с polling uiautomator; критерий ≤12 с выполнен).
- Выбор Москвы → шапка «Москва / Облачно / 2° / Макс.: 4°, мин.: 2°», без баннера (Live) — `fqf_03_moscow.png`. В DataStore появились `weather_москва` / `weather_updated_москва` / `weather_source_москва=met_no` (`fqf_cache_after_moscow.pb`).
- После force-stop приложение открылось на Москве — сохранение последнего города работает (`fqf_17_persist.png`).

## 4. R12 — карта — PASS

- Экран «Карта» полноэкранный, шапка «Москва / Карта · OpenStreetMap · 2°», карта на весь экран, тайлы OSM подгружены (виден центр Москвы, Кремль/Арбат/Тверской), маркер в центре, подпись «Маркер — выбранный город» и «© OpenStreetMap contributors» — `fqf_04_map.png`.
- Центр = координаты выбранного города. Кнопка «Центр карты — выбранный город» присутствует.
- Тап по маркеру: info-window **«Москва / 2° · Облачно»** с указателем — `fqf_05_map_marker.png`.
- Зум: pinch двумя пальцами (инъекция `sendevent` через root) изменил масштаб, тайлы перерисовались — `fqf_07_pinch.png` (сравнить с `fqf_04`).
- OSM доступен с телефона без VPN/ключа: `curl https://tile.openstreetmap.org/10/618/327.png` → **200, 33 904 B, 0.07 с** (`fqf_provider_curl.txt`).

## 5. R13 / R14 / R15 — полноэкранные экраны, тема и персист — PASS

**R13 «Список городов»** — полноэкранный, без таб-бара: СПб 3° (сохранённый), Москва 2° (сохранённый), Минск «Нет данных», Дубай «Нет данных» — `fqf_01_list.png`; позже Дубай 27° live, футер «Данные: MET Norway (api.met.no)» — `fqf_36.xml`. Переход на город из списка работает (SPb/Дубай).
**R14 «Поиск города»** — полноэкранный, поле, подсказка «Поиск идёт по всему миру и не требует ключей», живая выдача, выбор — `fqf_02_search.png`, `fqf_02_search_results.png`.
**R15 «Оформление»** — полноэкранный: «Рулетка тем», карточки «Голубое небо», «Медный закат», «Изумрудный лес», «Планета Марс» — «Доступно»; «Неоновый киберпанк», «Кровавый закат» — «В лутбоксах» (без фейковой разблокировки) — `fqf_08_view.png`.
- Выбор «Голубого неба» применился сразу (фон/чипы/чекмарк) — `fqf_09_theme_blue_view.png`, главная — `fqf_10_main_blue.png`.
- **Force-stop → повторный запуск:** тема осталась голубой, город Москва, баннер кэша 08:33 — `fqf_17_persist.png`. Персист темы подтверждён.
- В конце прогона тема возвращена в исходную медную (`fqf_46_restored.png`).

## 6. R16 — deep links (такси/еда) — PASS

Установлены: `ru.yandex.yandexmaps`, `com.android.chrome`, `Vanced`, «Браузер»; **нет** `ru.yandex.taxi` и `ru.foodfox.client`.

**Такси:**
- Карточка «Такси / Куда едем?» → шторка «Такси «Сухой салон»», «Нет данных о пробках», маршруты, «Вызвать Эконом за 320 ₽» — `fqf_19_taxi_sheet.png`.
- Кнопка → подтверждение «Открыть Яндекс Go?» / «Открыть» / «Отмена» — `fqf_20_taxi_confirm.png`.
- **«Отмена»:** приложение осталось на переднем плане (`mResumedActivity=com.koch.weather/.MainActivity`), `ACTION_VIEW` в logcat после нажатия нет — `fqf_21_taxi_cancel.png`, отметка времени 08:41:53.
- **«Открыть»:** chooser (`mResumedActivity=android/…ResolverActivity`) — `fqf_22_taxi_open.png`; выбран Chrome → `mResumedActivity=com.android.chrome/…ChromeTabbedActivity`, в адресной строке `taxi.yandex.ru/route…`, страница Яндекс Go — `fqf_23_taxi_chrome.png`.
- Полный intent из `dumpsys activity recents` (`fqf_intents.txt`): `act=android.intent.action.VIEW dat=https://taxi.yandex.ru/route?start-lat=55.7558&start-lon=37.6173&end-lat=55.78356646284697&end-lon=37.630521468530105&tariffClass=econom&ref=kochweather`.

**Еда:**
- Шторка «Доставка горячей еды», меню, «+» → «Заказать 2 шт. на 310 ₽» — `fqf_25_food_sheet.png`, `fqf_26_food_order.png`.
- Подтверждение «Открыть Яндекс Еду?» / «Открыть» / «Отмена» — `fqf_27_food_confirm.png`.
- «Открыть» → chooser — `fqf_28_food_open.png`; Chrome: `eda.yandex.ru` загружен — `fqf_29_food_chrome.png`; recents: `dat=https://eda.yandex.ru/`.
- Нативные `yandextaxi://`/`eda.yandex://` на устройстве не проверить (приложений нет) — критерий «при отсутствии приложения веб-фолбэк» выполнен.

## 7. R21 — честность данных — PASS

- «Список городов»: у Минска и Дубая (без кэша на тот момент) — **«Нет данных»** и в строке состояния, и в правой колонке; никаких выдуманных градусов — `fqf_01_list.png`.
- Пробки для Дубая (регион не поддерживается, `score=null`): «Дорожная ситуация / Дорожная обстановка / **Нет данных о пробках**», цифры `N/10` нет — `fqf_35_dubai_traffic.png`.
- Шторка такси при `trafficScore=null`: «Нет данных о пробках» без цифры — `fqf_19_taxi_sheet.png` (сравнить с Москвой, где живой Yandex Traffic дал «1/10 · На дорогах свободно» — `fqf_11_traffic.png`).
- Полный отказ провайдеров (08:46–08:51, Open-Meteo недоступен): экран показал **«Нет данных о погоде»** (красный баннер) и spinner, без подставленных температур — `fqf_32_dubai.png`; после ответа MET в 08:48:53 появились реальные 27°/29°/25° (`weather_source_дубай=met_no`).
- Закрытые темы не выдаются за доступные — `fqf_08_view.png`.

## 8. 7timer — резерв и доступность

- `curl` **с телефона** (`fqf_provider_curl.txt`, 08:49–08:50):
  - MET Norway (Москва): **http=200, 38 827 B, 0.16 с**;
  - Open-Meteo (Москва): **timeout 25 с, exit 28** (недоступен с этого Wi-Fi; в приложении — `SocketTimeoutException` в logcat);
  - **7timer** (Москва): **http=200, 15 183 B, 0.50 с** — эндпоинт доступен;
  - OSM tile: 200, 33 904 B.
- Для Дубая (`fqf_provider_curl_dubai.txt`): MET 200/0.15 с; 7timer **302** (OkHttp по умолчанию следует редиректам, но фактический fallback-разбор не проверен); Open-Meteo timeout.
- **Резерв не активировался**: MET Norway был доступен и отвечал, поэтому источник остался «Данные: MET Norway (api.met.no)» (`fqf_43_online_footer.png`). Сценарий «MET и Open-Meteo оба недоступны → „Данные: 7timer“» **BLOCKED**: оба первичных провайдера одновременно с устройства не погасить без модификации системы/сети, а код я не трогаю. Эндпоинт 7timer доступен; сам fallback-путь (parser/label) на этом APK не подтверждён. В новом коммите `8be22d8` изменён reserved fallback budget — перепроверить после пересборки.

## 9. Пейволл и premium — PASS

- Исходно premium **не куплен** (реклама на главной).
- «Купить подписку Премиум» → **подтверждение** «Подтвердить покупку? · Погода ПРЕМИУМ · Годовой VIP · 5.99 BYN за год · списание один раз» / «Подтвердить» / «Отмена» — `fqf_14_paywall_confirm.png`.
- **«Отмена»:** SHA-256 DataStore до/после — `adabaea1e017f4b86b3ff98e1427fdfce84853e2983d560cd5588f5b419f9614` (не изменился); снова витрина с «Купить» — `fqf_15_paywall_cancel.png`.
- **«Подтвердить»:** «★ ПРЕМИУМ АКТИВЕН», кнопка «Сбросить подписку (для теста рекламы)», hash `bac0841d69fe8a876bf87d24748e0f09d7132cf13b9314dd0eded470fccb18b7` — `fqf_16_premium_active.png`.
- **force-stop → запуск:** в футере/баннере «★ ПРЕМИУМ», рекламный баннер отсутствует, hash тот же (ключ `is_premium_purchased=true`) — `fqf_18_premium_persist.png`.
- В конце premium сброшен тестовой кнопкой: `is_premium_purchased=false`, витрина снова «Купить» (`fqf_45.xml`, `fqf_cache_final.pb`).

## 10. R22 — logcat

- Полный дамп: `shots/fqf_logcat.txt` — **3 357 строк, 485 069 B**, окно 08:30–09:00.
- `FATAL EXCEPTION` — **0**, `ANR in com.koch.weather` — **0**, `am_anr` — **0**, `E/AndroidRuntime` — **0**.
- Есть только сетевые `E/WeatherApp: OpenMeteo exception: SocketTimeoutException…` (Open-Meteo недоступен с телефона) — это не крэш: приложение показывало «Нет данных», ретраило и позже получило MET. Для perf/стабильности — не блокер.

## 11. Наблюдения (не влияют на PASS по ID)

1. **Open-Meteo недоступен с этого Wi-Fi** (connect timeout 6 с в приложении, 25 с в curl) при живых MET/OSM/7timer. Приложение не падает и честно пишет «Нет данных», но окно «Нет данных» для Дубая длилось ~5 минут (08:46–08:51) до ответа MET.
2. **Ретраи долгие:** при недоступном провайдере приложение повторяет запросы с интервалом ~60 с (видно в logcat 08:46:52, 08:47:43, 08:48:53); для UX стоит проверить.
3. **Порядок блоков различается между голубой и медной темами** (подтверждено в run 6 и здесь: `fqf_10_main_blue.png` vs `fqf_46_restored.png`) — вопрос к visual-critic/judge, не к feature-qa.
4. При выборе SPb в 08:53 первый сетевой запрос не удался (баннер 08:09 остался), повторный в 08:59:39 обновил кэш — внешняя нестабильность MET, не логика.

## 12. Уборка состояния (проверено в 09:00:02)

```
$ adb shell am force-stop com.koch.weather
$ adb shell pidof com.koch.weather        → <empty>
wifi_on=1, mobile_data=1, Active default network: 239
versionName=1.0-benchmark, lastUpdateTime=2026-11-14 08:22:35 (APK не менялся)
```

- Тема возвращена в исходную **медную** (`current_theme=COPPER_SUNSET`), premium — **не куплен** (`is_premium_purchased=false`), город — **Санкт-Петербург** (`last_city_json`), последнее обновление кэша 08:59:39 (MET).
- Сеть возвращена в исходное состояние (Wi-Fi + данные включены). Телефон готов к финальному переснимку device-qa.

## 13. Индекс доказательств

| Файл | Что доказывает |
|------|----------------|
| `shots/fqf_00_launch.png` | старт: СПб по умолчанию, баннер «Сохранённый прогноз, 08:09» |
| `fqf_01_list.png`, `fqf_36.xml` | R13: полноэкранный список, температуры/«Нет данных» |
| `fqf_02_search.png`, `fqf_02_search_results.png` | R14: полноэкранный поиск, живая выдача за 4.2 с |
| `fqf_03_moscow.png` | R20: выбор Москвы |
| `fqf_04_map.png`, `fqf_05_map_marker.png`, `fqf_07_pinch.png` | R12: OSM, центр, маркер/info-window, pinch-зум |
| `fqf_08_view.png`, `fqf_09_theme_blue_view.png`, `fqf_10_main_blue.png` | R15: «Вид», применение темы |
| `fqf_17_persist.png` | R15: тема пережила force-stop |
| `fqf_11_traffic.png` | R21: живой трафик Москвы «1/10» |
| `fqf_13`–`fqf_18` | пейволл: витрина, подтверждение, отмена, покупка, persist |
| `fqf_19`–`fqf_23` | R16 такси: шторка, подтверждение, отмена, intent, Chrome |
| `fqf_25`–`fqf_29` | R16 еда: шторка, заказ, подтверждение, chooser, Chrome |
| `fqf_32_dubai.png` | R21: UNAVAILABLE «Нет данных о погоде» без фейковых цифр |
| `fqf_35_dubai_traffic.png` | R21: «Нет данных о пробках» без цифры |
| `fqf_39_offline.png`, `fqf_40_offline_footer.png` | R19: офлайн-баннер с временем + футер «сохранённый прогноз» |
| `fqf_42_online.png`, `fqf_43_online_footer.png` | R19: баннер исчез, футер «Данные: MET Norway (api.met.no)» |
| `fqf_cache_before_offline.pb`, `fqf_cache_after_online.pb` | R19: lastUpdated 08:48:53 → 08:56:57, hash a45fe7… → 935b8b… |
| `fqf_cache_precancel.pb`, `fqf_cache_final.pb` | пейволл/финальное состояние (premium=false, тема медная, СПб) |
| `fqf_cache_before.pb`, `fqf_cache_after_moscow.pb`, `fqf_cache_after_dubai.pb` | проверка источников/времени кэша |
| `fqf_provider_curl.txt`, `fqf_provider_curl_dubai.txt` | curl MET/Open-Meteo/7timer/OSM с телефона |
| `fqf_intents.txt` | полные deep-link intent’ы из `dumpsys activity recents` |
| `fqf_logcat.txt` | R22: полный logcat, без FATAL/ANR |

## 14. Рекомендация

- **Для этого APK (`b8c7290`, lastUpdateTime 08:22:35): PASS** по проверенным ID (R12–R16, R19–R22, пейволл, 7timer-доступность).
- **После пересборки `8be22d8` — обязательный ретест R1–R23** (код ушёл вперёд: metric wind units, reserved fallback budget, UI-фиксы). В первую очередь: R19, 7timer (полный сценарий с недоступными MET+Open-Meteo), ветер в м/с, визуальные фиксы.
- Полный fallback на 7timer на текущем APK **не подтверждён** (BLOCKED: оба первичных провайдера одновременно не выключались). Нужен отдельный прогон с управляемой блокировкой MET+Open-Meteo (например, на тестовом стенде/эмуляторе), чтобы увидеть «Данные: 7timer».
