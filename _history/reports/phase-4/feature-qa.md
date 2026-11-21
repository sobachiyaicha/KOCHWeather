# feature-qa — Фаза 4 (KOCHWeather)

**Дата прогона:** 14.11.2026, 02:18–02:52 MSK
**Устройство:** Sony G8342 (Xperia XZ1), serial `CB512FQDXN`, Android 11 (API 30), 1080×1920, root-shell доступен
**Приложение:** `com.koch.weather`, versionName 1.0 (versionCode 1), `lastUpdateTime=2026-11-14 02:09:16`
**APK не переустанавливался.** Тестовые действия — только adb (input/swipe/am/logct/screencap/uiautomator), чтение приватных данных — через root-shell.

> **Важно: установленная сборка ≠ текущее рабочее дерево.** Пока шёл прогон, другие роли меняли исходники (`git status`: новые `ui/components/WeatherMapScreen.kt`, `data/deeplink/`, `data/provider/`, `ImageRepository.kt`, `SavedCityWeather.kt` и др.). Установленный APK собран в 02:09:16 и этих изменений **не содержит**. Все результаты ниже относятся к установленной сборке; после новой сборки/установки R12–R16, R19, R21 нужно перепроверить.

## ИТОГОВЫЙ СТАТУС: **FAIL**

| Блок | Результат |
|---|---|
| Доступность источников из РФ без VPN | Open-Meteo API — **НЕТ** (TCP blackhole), geocoding — **нестабильно** (3/6), MET Norway / Nominatim / Photon / OSM-тайлы / Wikimedia — **ДА** |
| R20 Города | **PASS** (СПб по умолчанию, Москва, поиск работают) |
| R12–R16 | **FAIL** (Карта/Список/Поиск/Вид — диалоги, а не экраны; deep links не вызываются) |
| R19 Оффлайн | **FAIL** (баннер есть, но кэша прогноза нет; скрытие баннера не связано с успешным обновлением) |
| R21 Без фейков | **FAIL** (синтетические цифры при отсутствии данных, «нет данных» не показывается) |
| Стабильность (logcat) | **PASS** (крашей и ANR нет) |

---

# 1. Доступность внешних источников из России без VPN

## 1.1. Что проверялось и чем

На телефоне доступны `curl 7.67.0 (Android)` и `toybox`, shell работает от uid=0. Тесты — прямым HTTPS с телефона (без VPN; на устройстве нет tun-интерфейса, public IP `91.223.89.35`). Скрипты и полные логи: `shots/fq_net_device.sh|txt`, `shots/fq_sources_device.sh|txt`, `shots/fq_openmeteo_retries.sh|txt`, `shots/fq_block_type.sh|txt`.

Ключевые команды:

```powershell
$adb = "D:\akcon\backup\Загрузки\platform-tools\adb.exe"
& $adb -s CB512FQDXN push shots\fq_sources_device.sh /data/local/tmp/fq_sources_device.sh
& $adb -s CB512FQDXN shell "sh /data/local/tmp/fq_sources_device.sh"

# на телефоне, внутри скрипта:
# curl -sS -m 15 -A "$UA" -D hdr -o body -w 'code=%{http_code} dns=%{time_namelookup} tcp=%{time_connect} tls=%{time_appconnect} total=%{time_total} ip=%{remote_ip}' <URL>
# ping -c 1 -W 4 <host>   # резолв + ICMP
# curl --resolve <host>:443:<IP> https://<host>/...   # проверка IP-уровня vs SNI
```

## 1.2. Таблица «источник → доступен из РФ без VPN»

| Источник | С телефона | Код / валидный ответ | Доступен из РФ без VPN | Доказательство |
|---|---|---|---|---|
| `api.open-meteo.com` (прогноз) | **timeout TCP** | `code=000`, `tcp=0`, 6/6 попыток | **НЕТ** | `shots/fq_openmeteo_retries.txt`, `shots/fq_net_device.txt`, `shots/fq_block_type.txt` |
| `geocoding-api.open-meteo.com` | **нестабильно**: 3 успеха / 3 таймаута из 6 | успех `code=200`, JSON `"results"`; провал `code=000` | **НЕТ (нестабильно)** | `shots/fq_openmeteo_retries.txt` (раунды 2/4/6 — 200), `shots/fq_sources_device.txt` (02:29 — timeout) |
| `api.met.no` (`locationforecast/2.0/compact`) | **ДА** | `200`, `application/json`, 37 693 байта, маркер `"timeseries"` найден | **ДА** | `shots/fq_sources_device.txt` |
| `wttr.in` (`?format=j1`) | **ДА, но медленно** | `200`, 21 582 байта, маркер `"current_condition"` найден, соединение оборвано по 15-секундному таймауту после получения тела | **ДА (с оговоркой по скорости)** | `shots/fq_sources_device.txt` |
| `nominatim.openstreetmap.org` (геокодинг) | **ДА** | `200`, `application/json`, маркер `"place_id"` | **ДА** | `shots/fq_sources_device.txt` |
| `photon.komoot.io` (геокодинг) | **ДА** | `200`, `application/json`, маркер `"features"` | **ДА** | `shots/fq_sources_device.txt` |
| `tile.openstreetmap.org` (тайл z12/2393/1190) | **ДА** | `200`, `image/png`, 32 864 байта, PNG-сигнатура | **ДА** | `shots/fq_sources_device.txt`, `shots/fq_net_device.txt` |
| `upload.wikimedia.org` (Wikimedia Commons) | **ДА** | `200`, `image/jpeg`, 9 022 байта | **ДА** | `shots/fq_sources_device.txt` |

### С хоста (для сравнения; хост в той же сети, public IP `91.223.89.35`)

Важно: на хосте в пользовательском окружении задан `HTTPS_PROXY=http://127.0.0.1:10809`, поэтому «обычный» `curl` с хоста идёт **через локальный прокси/VPN** и не является проверкой «из РФ без VPN». Корректные данные — с `--noproxy "*"`:

| Источник | Хост напрямую (`--noproxy`) | Хост через прокси |
|---|---|---|
| `api.open-meteo.com` | `code=000`, fail connect 21 с | `200` (это и есть VPN-путь) |
| `geocoding-api.open-meteo.com` | `code=000`, fail connect 21 с | `200` |
| `tile.openstreetmap.org` | `200` | `200` |
| `upload.wikimedia.org` | `200` | `200` |
| `api.met.no` | `200` (TLS 4.1 с) | — |
| `nominatim` / `photon` | `200` / `200` | — |
| `wttr.in` | `200`, но медленно (обрыв на 20 с) | — |

## 1.3. Вывод по инциденту (DNS / IPv6 / блокировка / приложение)

Проверено и исключено:

1. **DNS — не причина.** С телефона `api.open-meteo.com → 157.90.83.168` (8/8 резолвов стабильны), `geocoding-api.open-meteo.com → 202.61.206.6`. Публичные DoH Google и Cloudflare возвращают тот же единственный A-рекорд `157.90.83.168` (`shots/fq_block_type.txt`, блок 6). TTL 1800. Резолв мгновенный (`dns≈0.002–0.06 с`). Домен не подменён.
2. **IPv6 — не причина.** У `api.open-meteo.com` нет AAAA (`SIG/SOA` в ответе DNS; `curl -6` → `Could not resolve host`). На телефоне нет глобального IPv6: только ULA `fd58:…` и `fe80::`; приложение ходит по IPv4 на `157.90.83.168:443` — это же видно в logcat.
3. **Приложение — не первопричина.** Клиент корректно обращается по документированному URL и таймаутит 15000 мс ровно как задано. Logcat:
   ```
   D/WeatherApp: Fetching forecast from: https://api.open-meteo.com/v1/forecast?latitude=59.9386&longitude=30.3141&...
   E/WeatherApp: OpenMeteo exception: failed to connect to api.open-meteo.com/157.90.83.168 (port 443) from /192.168.1.91 (port 38630) after 15000ms
   E/WeatherApp: java.net.SocketTimeoutException: failed to connect to api.open-meteo.com/157.90.83.168 (port 443) ... after 15000ms
   ```
   (`shots/fq_logcat.txt`)
4. **Имеет место блокировка/чёрная дыра на уровне IP:порт конкретного хоста.** TCP-соединение к `157.90.83.168:443` **не устанавливается вообще** (`tcp=0.000000`, `nc` — Timeout), в том числе:
   - на порт **80** (`http://api.open-meteo.com/` — timeout 15 с);
   - с **чужим SNI** (`curl --resolve example.com:443:157.90.83.168 https://example.com/` → timeout) — значит это не SNI/HTTP-фильтрация, а именно недоступность IP:порта;
   - **с мобильных данных** (logcat: `from /100.105.119.222 ... after 15000ms`) и **с Wi-Fi** (`/192.168.1.91`) — обе сети оператора;
   - **с хоста напрямую** (`--noproxy`, IP `91.223.89.35`).
   ICMP на оба IP Open-Meteo не отвечает, при этом OSM/Wikimedia/MET Norway/Photon отвечают (ttl 51–53).
5. **Не «весь Hetzner» и не «весь зарубеж»**: `photon.komoot.io` (`116.202.51.114`, Hetzner) и `wttr.in` (`5.9.243.187`, Hetzner) доступны; MET Norway, Fastly (OSM/Nominatim), Wikimedia — доступны. Проблема точечная: `157.90.83.168` (api) и, с перебоями, `202.61.206.6` (geocoding).
6. Различить «блокировка на стороне сети РФ» и «geo-firewall на стороне Open-Meteo» по сетевым данным нельзя — оба варианта дают SYN-timeout. Но **это не DNS, не IPv6 и не баг приложения**.

**Практический вывод:** live-погода в текущей сети недоступна; для работоспособного приложения нужен либо рабочий сетевой путь к `api.open-meteo.com`, либо смена источника на доступный из РФ (`api.met.no` отдаёт валидный JSON `properties.timeseries`; геокодинг — Nominatim/Photon), что является решением по коду, а не по тестированию.

---

# 2. Матрица ACCEPTANCE: ID → результат → доказательство

| ID | Требование | Результат | Доказательство | Комментарий |
|---|---|---|---|---|
| **R12** | Экран «Карта»: настоящая OSM-карта, центр = координаты города, зум, маркер | **FAIL** | `shots/fq_05_tab_map.png` | Вкладка «Карта» открывает модальный диалог **«Карта осадков»** с рисованным радаром (концентрические круги на сетке). OSM-тайлов, зума и маркера нет. Текст «Радар: слабый дождь через 40 мин.» — без источника данных |
| **R13** | Экран «Список»: полноэкранный список городов с температурой, переход на город | **FAIL** | `shots/fq_06_tab_list.png` | Открывается модальный диалог **«Выбор города»**: СПб, Москва, Минск, Дубай (и добавленный Лондон), только название + страна, **без температур**; не полноэкранный |
| **R14** | Экран «Поиск»: полноэкранный поиск, живой geocoding Open-Meteo | **FAIL** (geocoding работает) | `shots/fq_07_tab_search.png` | Тот же модальный диалог «Выбор города». Живой geocoding **работает**: запрос `name=London` ушёл в `geocoding-api.open-meteo.com`, вернулись «Лондон/Британия», «Лондон/Канада». Но экран не полноэкранный — критерий не выполнен |
| **R15** | Экран «Вид»: полноэкранный выбор оформления (темы лутбоксов + рулетка) | **FAIL** | `shots/fq_09_tab_view.png` | Открывается диалог **«🎨 Рулетка тем 2025 / Поздравляем! Выберите стиль»** с чипами «Голубое небо / Медный закат / Изумрудный лес», стрелками и «Продолжить». Это не полноэкранный экран выбора оформления |
| **R16** | Deep links: Яндекс Go (маршрут по координатам), Яндекс Еда, веб-фолбэк | **FAIL** | `shots/fq_12_taxi_call.png`, `shots/fq_19_food_order.png`; `shots/fq_13_yandex_go_scheme.png`, `shots/fq_17_eda_browser.png` | В приложении заказ **не** запускает внешний intent: такси показывает фейковую «Поездка в процессе» (Hyundai Solaris, «о142вх 178», водитель Алексей В., 403 ₽), фокус остаётся `com.koch.weather`; заказ еды просто сбрасывает корзину. При этом сами deep links на устройстве рабочие (см. 3.4) — приложение их не вызывает |
| **R19** | Оффлайн: кэш последнего прогноза в DataStore, баннер «нет сети», скрытие при восстановлении | **FAIL** | `shots/fq_21_offline_header.png`, `shots/fq_22_online_banner.png`; DataStore | Баннер «Нет подключения к сети. Показан сохраненный прогноз.» отображается. Но **кэша прогноза нет вообще**: в `weather_cache.preferences_pb` только `saved_cities_json` (341→435 байт, т.е. только список городов и добавленный Лондон). После восстановления сети баннер исчез (`fq_22`), хотя обновление снова упало по таймауту, а данные остались прежними → скрытие по факту сетевого колбэка, а не по успешному обновлению |
| **R20** | Города: СПб по умолчанию, затем Москва; поиск и переключение | **PASS** | `shots/fq_04_fresh_header.png`, `shots/fq_07_tab_search.png`, `shots/fq_08_london_selected.png`, `shots/fq_23_moscow_selected.png` | После `pm clear` холодный старт — **Санкт-Петербург**. Переключение на Москву из списка работает (шапка «Москва, 7°, макс. 9°, мин. 2°»), поиск «London» возвращает результаты и переключает город |
| **R21** | Без заглушек и фейков: при отсутствии данных — «нет данных», а не выдуманные цифры | **FAIL** | `shots/fq_04_fresh_header.png`, `shots/fq_08_london_selected.png`; `shots/fq_logcat.txt`; дамп DataStore | После `pm clear`, при пустом кэше и заблокированном API показаны «Санкт-Петербург, 5°, Переменная облачность, макс. 7°, мин. 1°», а после выбора Лондона — «Лондон, 15°, макс. 18°, мин. 8°». Ни одного «нет данных» в UI не найдено |
| **R22** | Стабильность (в части feature-qa: logcat без крэшей/ANR) | **PASS** (только logcat) | `shots/fq_logcat.txt` | Буфер `crash` пуст, `/data/anr/` пуст, `am_anr` для `com.koch.weather` нет, dropbox пуст. Ошибки сети — закономерные `SocketTimeoutException`/`Unable to resolve host`, не крэши |
| **R17** | Реальные фото по URL (Wikimedia/Coil), плейсхолдер | **вне задачи прогона**; источник — **ДА** | `shots/fq_sources_device.txt` | `upload.wikimedia.org` доступен (200, image/jpeg). Полная проверка R17 в задание не входила; на проверенных экранах фото не обнаружены (эмодзи-иллюстрации). Учесть, что исходники меняются параллельно |

---

# 3. Детализация по приложению

## 3.1. R21 — синтетические данные (главная находка)

1. Исходное состояние (до очистки): `shots/fq_00_current.png` — «Москва, 7°, Переменная облачность, макс. 9°, мин. 2°», красный баннер «Нет подключения к сети. Показан сохранённый прогноз.».
2. `adb shell pm clear com.koch.weather` → холодный старт. В DataStore **только** `saved_cities_json` (341 байт, hex-дамп: СПб/Москва/Минск/Дубай). Погодного ключа (`last_weather_json`, `weather_<city>`) нет.
3. Через ~20 с (после таймаута API) на экране — «Санкт-Петербург, 5°, Переменная облачность, макс. 7°, мин. 1°» и баннер «Показан сохраненный прогноз» (`shots/fq_04_fresh_header.png`).
4. Поиск «London» → выбор → сразу «Лондон, 15°, макс. 18°, мин. 8°» (`shots/fq_08_london_selected.png`), хотя запрос к API для Лондона тоже упал по таймауту.
5. DataStore после всех действий: **435 байт**, ключи — только `saved_cities_json` (добавился Лондон). Прогноза нет.
6. Ни в одном состоянии не появилось «нет данных» / «нет связи» вместо цифр.

Вывод: приложение показывает правдоподобные, но **выдуманные** значения по каждому городу (СПб 5°, Москва 7°, Лондон 15°), а баннер утверждает, что это «сохранённый прогноз». Это прямое нарушение R21 и вводящая в заблуждение формулировка R19.

## 3.2. R19 — оффлайн и кэш

- Авиарежим (`cmd connectivity airplane-mode enable`): Wi-Fi off, маршрутов нет. Перезапуск приложения → баннер «Нет подключения к сети. Показан сохраненный прогноз.» + те же цифры (`shots/fq_21_offline_header.png`).
- Logcat в оффлайне: `OpenMeteo exception: Unable to resolve host "api.open-meteo.com": No address associated with hostname` — ожидаемо.
- Реальный кэш прогноза **ни разу не создался** (в `files/datastore/` один файл, в нём только города). То есть сценарий «кэш последнего прогноза» проверить не на чем: его нет.
- Восстановление сети (`airplane-mode disable`, Wi-Fi «Keenetic-9538-5Gg», `ping 8.8.8.8` OK): баннер **исчез** (`shots/fq_22_online_banner.png`), но последний fetch снова упал (`SocketTimeoutException ... after 15000ms`, уже с мобильного IP `100.105.119.222`), данные остались прежними. Скрытие баннера происходит по событию «сеть появилась», а не по успешному обновлению.

## 3.3. R12–R15 — что реально открывается

| Вкладка | Что появляется | UI-тексты (uiautomator) |
|---|---|---|
| Карта | диалог «Карта осадков» | «Карта осадков», «Санкт-Петербург • 5°», «Радар: слабый дождь через 40 мин.», «Закрыть радар» |
| Список | диалог «Выбор города» | «Выбор города», «Поиск города…», «Санкт-Петербург/Россия», «Москва/Россия», «Минск/Беларусь», «Дубай/ОАЭ», «Лондон» |
| Поиск | тот же диалог, живой geocoding | «Лондон/Британия», «Лондон/Канада», «Ландон/США» |
| Вид | диалог «Рулетка тем 2025» | «🎨 Рулетка тем 2025», «Поздравляем! Выберите стиль», «Голубое небо / Доступно ✓», «Медный закат», «Изумрудный лес», «🎠 Рулетка!», «Продолжить ›» |

Ни один из четырёх разделов не является полноэкранным; карта — рисованная, список — без температур.

## 3.4. R16 — deep links: приложение vs платформа

В приложении:
- Такси: «Вызвать Эконом за 320 ₽ ›» → остаёмся в `com.koch.weather`; появляется «Поездка в процессе», «Hyundai Solaris (Белый) • о142вх 178», «Водитель: Алексей В. (4.9 ★)», «Стоимость поездки: 403 ₽ / 13.0 BYN». В logcat — ни одного `ActivityTaskManager START` с внешним пакетом (`shots/fq_12_taxi_call.png`).
- Еда: выбор блюда → «Заказать 1 шт. на 195 ₽ ›» → лист просто сбрасывается в меню, внешнего перехода нет (`shots/fq_19_food_order.png`).

Проверка самих ссылок через `am start` (то, что просили):
- `yandexmaps://maps.yandex.ru/?rtext=59.9386,30.3141~55.7558,37.6173&rtt=auto` → открылся `ru.yandex.yandexmaps/…MapActivity` (`shots/fq_13_yandex_go_scheme.png`, `shots/fq_14_yandex_go_loaded.png`).
- `https://yandex.ru/maps/?rtext=…` → системный chooser «Яндекс Карты / Браузер / Chrome / Vanced» (`shots/fq_15_https_resolver.png`) — веб-фолбэк возможен.
- `https://eda.yandex.ru/` → chooser, затем Chrome: страница реально загрузилась («Выберите адрес для доставки», адресная строка `eda.yandex.ru`) (`shots/fq_17_eda_browser.png`). Яндекс Еды как приложения на устройстве нет (`pm list packages | grep -i yandex` → только `ru.yandex.yandexmaps`, `ru.yandex.music`), то есть корректный сценарий для Еды — именно веб-фолбэк.

Вывод: платформа и устройство deep links поддерживают; приложение их не формирует и не отправляет.

## 3.5. R20 — города

- После `pm clear` холодный старт: **Санкт-Петербург** (`shots/fq_04_fresh_header.png`), список городов по умолчанию: СПб → Москва → Минск → Дубай.
- Переключение на Москву через «Список»: шапка «Москва, 7°» (`shots/fq_23_moscow_selected.png`), в logcat — запросы `latitude=55.7558&longitude=37.6173`.
- Поиск: «London» → `geocoding-api.open-meteo.com` → результаты → выбор → «Лондон» (`shots/fq_07_tab_search.png`, `shots/fq_08_london_selected.png`).
- Оговорка: температуры для всех городов синтетические (см. R21); geocoding отвечает нестабильно (3/6).

## 3.6. Logcat — стабильность

- `logcat -b crash -d` — пусто; `/data/anr/` — пусто; `logcat -b events | grep am_anr` — пусто; `dumpsys dropbox` по `com.koch.weather` — пусто.
- Единственные ошибки приложения — сетевые (`SocketTimeoutException`, `Unable to resolve host`); они обработаны, крэша нет.
- Наблюдение (не perf-вердикт): оверлей FPS в приложении показывал 58–60 FPS на всех проверенных экранах.

---

# 4. Блокеры

| ID | Блокер | Что заблокировано | Что нужно |
|---|---|---|---|
| **B1** | `api.open-meteo.com` (`157.90.83.168`) — TCP blackhole на 80/443 из этой сети (Wi-Fi, мобильные данные, хост напрямую); DNS/IPv6/приложение исключены | Невозможно проверить live-прогноз, создание кэша в DataStore и «скрытие баннера после успешного обновления». Любые положительные проверки R19/R21 на реальных данных недостижимы | Сетевой путь к API (VPN/прокси как временная мера для теста) **или** перевод приложения на доступный источник (`api.met.no` проверен: 200 + `properties.timeseries`) — решение по коду, вне полномочий feature-qa |
| **B2** | `geocoding-api.open-meteo.com` доступен нестабильно (3/6) | Поиск города может «молчать» при повторных прогонах | Аналогично: ретраи/альтернативный геокодер (Nominatim/Photon проверены — 200) |
| **B3** | В **установленной** сборке (02:09) нет OSM-карты и вызовов внешних intent (вкладки открывают диалоги; такси/еда — локальная имитация) | R12–R16 не могут быть PASS в принципе | Реализация экранов и deep links (ui-coder/data-coder), затем сборка, установка и повторный прогон. В рабочем дереве уже появились `WeatherMapScreen.kt`, `data/deeplink/` — в установленном APK их нет |

Ограничения прогона: APK не переустанавливался; проверялось только установленное приложение. На хосте обнаружен пользовательский `HTTPS_PROXY=http://127.0.0.1:10809` — «хостовые» проверки разделены на «через прокси» и «напрямую». Время прогона 02:18–02:52 MSK.

---

# 5. Завершение прогона

По требованию пользователя приложение остановлено, чтобы не расходовать батарею:

```powershell
& $adb -s CB512FQDXN shell am force-stop com.koch.weather
& $adb -s CB512FQDXN shell pidof com.koch.weather      # пусто
& $adb -s CB512FQDXN shell "ps -A | grep com.koch.weather"  # пусто
```

Фактический вывод: `pidof` — пустая строка, `ps` — пустая строка, процесс отсутствует.

Состояние устройства после прогона: авиарежим выключен (`airplane_mode_on=0`), Wi-Fi подключён («Keenetic-9538-5Gg»), `ping 8.8.8.8` — 23 мс, приложение не запущено.

---

# 6. Файлы доказательств

**Скриншоты (`shots/`):**
`fq_00_current.png`, `fq_01_launch_online.png`, `fq_02_clouds_swiped.png`, `fq_03_fresh_online.png`, `fq_04_fresh_header.png` (СПб 5° + баннер, R20/R21), `fq_05_tab_map.png` (R12), `fq_06_tab_list.png` (R13), `fq_07_tab_search.png` (R14), `fq_08_london_selected.png` (R21), `fq_09_tab_view.png` (R15), `fq_10_scrolled.png`, `fq_11_taxi_sheet.png`, `fq_12_taxi_call.png` (R16), `fq_13_yandex_go_scheme.png`, `fq_14_yandex_go_loaded.png`, `fq_15_https_resolver.png`, `fq_16_eda_web.png`, `fq_17_eda_browser.png`, `fq_18_food_sheet.png`, `fq_19_food_order.png` (R16), `fq_20_offline_clouds.png`, `fq_21_offline_header.png` (R19), `fq_22_online_banner.png` (R19), `fq_23_moscow_selected.png` (R20).

**Логи и скрипты (`shots/`):**
`fq_net_device.txt` (первые curl-пробы), `fq_sources_device.txt` (полная матрица источников + DNS/IPv6 + public IP), `fq_openmeteo_retries.txt` (6 раундов api/geo), `fq_block_type.txt` (DNS 8/8, foreign-SNI, DoH, nc), `fq_logcat.txt` (полный лог прогона), `fq_net_device.sh`, `fq_sources_device.sh`, `fq_openmeteo_retries.sh`, `fq_block_type.sh`.

**Дампы UI (временные, `.local/tmp/`):** `fq_ui_01…28.xml` — иерархии экранов, использованные для текстов выше.
