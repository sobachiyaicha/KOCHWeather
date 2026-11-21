# КОЧWeather 1.1 — Android + iOS из одного кода

Android-приложение-сатира на погодные сервисы: живые карточки (Дубай, лутбоксы, такси, еда),
премиум-пейволл, реклама, выпадение тем из лутбоксов, жест «прогони облака пальцем»,
модалка «про траву», нижний таб-бар на 5 разделов.

**Теперь и на iPhone.** Порт 1:1 на Kotlin Multiplatform: общий код один,
нативные части платформенные — карта на MapKit с OSM-тайлами, видео на AVPlayer
со звуком, шрифты Inter. Все тексты, мат, мемы и абсурд — дословно.

| Релиз | Android | iPhone |
|---|---|---|
| **v1.1** | [KOCHWeather-1.1.apk](../../releases/download/v1.1/KOCHWeather-1.1.apk) (8.2 МБ) | [KOCHWeather-1.1-ios.ipa](../../releases/download/v1.1/KOCHWeather-1.1-ios.ipa) (20.8 МБ, iOS 16.3+) |

Установка на iPhone — [TrollStore](https://github.com/opa334/TrollStore): перекинуть ipa на телефон,
«Install from file», готово (без 7-дневного профиля и без платного Apple ID). Обычная Xcode-подпись
(Personal Team) тоже работает, но протухает через 7 дней.

## Как устроен код

```
shared/                     ← весь продукт: один код на обе платформы
  src/commonMain/           ← 100% UI (Compose Multiplatform), данные, сеть, DataStore,
                               лутбокс, реклама, облака, темы, навигация — всё здесь
  src/androidMain/          ← Android-специфика: osmdroid-карта, BitmapFactory, res/
  src/iosMain/              ← iOS-специфика: MapKit + OSM-тайлы, AVPlayer (звук),
                               Inter-шрифты, dispatcher на dispatch_after
androidApp/                 ← Android-обёртка (бывшая папка app/, см. ветку main)
iosApp/                     ← iOS-обёртка: SwiftUI-хост + Info.plist
scripts/ios-build.sh        ← сборка iOS БЕЗ xcodebuild (swiftc + ad-hoc подпись)
_history/                   ← отчёты ролей порта по фазам + judge-вердикты
```

Прежний монолит `app/` переехал: UI и данные — в `shared/src/commonMain`
(идентично на обеих платформах), платформенные замены — в `androidMain`/`iosMain`.
Ветка [`main`](https://github.com/sobachiyaicha/KOCHWeather/tree/main) — оригинальный
Android до порта, как архив.

## Стек

- **Общий**: Kotlin 2.4.21, Compose Multiplatform 1.12.2, kotlinx.serialization,
  Ktor (Darwin/OkHttp), DataStore 1.1.1, kotlinx-datetime, AGP 9.0.1, Gradle 9.1.0, JDK 17
- **Android**: Jetpack Compose, Material3, osmdroid (карта OSM), Coil, `minSdk 24`
- **iOS**: iOS 16.3+, SwiftUI-хост + ComposeUIViewController, MKMapView
  (OSM-тайлы, честный User-Agent), AVPlayer + AVAudioSession `.playback`,
  DataStore в Application Support

## Данные (работает из РФ без VPN и ключей)

| Задача | Основной | Резерв |
|---|---|---|
| Прогноз | **MET Norway** `api.met.no` (свой User-Agent, кэш-политика 30 мин / 3 ч) | **7timer** (8 с), затем **Open-Meteo** |
| Геокодинг | **Photon** | Nominatim → Open-Meteo geocoding |
| Карта | OSM-тайлы (osmdroid на Android, MKTileOverlay на iOS) | — |
| Фото | Wikimedia Commons (свой User-Agent) | плейсхолдер Coil |

Порядок: гонка MET/Open-Meteo (20 с) → 7timer (8 с) → снимок из DataStore → честное «Нет данных».
Синтетических температур нет: если данных нет — «Нет данных», кэш помечается как кэш с временем.
Авто-retry с backoff (5→20→40→60 с), триггеры: сеть, foreground, таймер.

## Сборка

**Android** (JDK 17):
```bash
./gradlew :androidApp:assembleDebug      # APK
./gradlew :androidApp:testDebugUnitTest  # 250 тестов
```

**iOS** (macOS + JDK 17, без xcodebuild — подробности в docs/14-TOOLCHAIN.md):
```bash
./scripts/ios-build.sh --simulator              # для симулятора
./scripts/ios-build.sh --device --ipa --release # IPA для TrollStore
```

**Общие тесты**: `./gradlew :shared:allTests`

## Статус порта

Judge-гейты пройдены по фазам (отчёты в `_history/reports/`): реструктуризация KMP,
данные+UI в commonMain, ресурсы/темы, карта 1:1, QA-свейп (feature/offline/performance/
security/localization — PASS). Проверено на симуляторе: вся лента, 6 тем, лутбокс
(5 попыток, история, рестарт сохраняет), реклама (первый показ — лев, крестик 3/5 с,
автозакрытие видео), вуаль/облачка по таймерам, карта с OSM-тайлами и пином.
Что честно не проверено на живом iPhone: звук на слух, haptics, живучесть сессии >5 мин,
deep link в Яндекс Go — см. список device-обязательств в `_history/reports/final/HANDOFF.md`.

## Лицензия

Код — MIT. Фоны — Wikimedia Commons (см. ATTRIBUTIONS.md). Рекламные картинки и ролики —
из папки `changes/` заказчика, права не проверялись.
