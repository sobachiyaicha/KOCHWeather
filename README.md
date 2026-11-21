# КОЧWeather 1.1 — мультиплатформенное (Android + iOS из одного кода)

Приложение-сатира на погодные сервисы: живые карточки (Дубай, лутбоксы, такси, еда),
премиум-пейволл, реклама, выпадение тем из лутбоксов, жест «прогони облака пальцем»,
модалка «про траву», нижний таб-бар на 5 разделов.

**Один код — обе платформы.** Вся логика и весь UI живут в `shared/src/commonMain`
(Kotlin Multiplatform + Compose Multiplatform), платформенные детали — в
`androidMain`/`iosMain`: карта на osmdroid (Android) и MapKit с OSM-тайлами (iOS),
видео на ExoPlayer-пути Android и AVPlayer со звуком на iOS, шрифты Inter везде.
Тексты, мат, мемы и абсурд — дословно одинаковы на обеих платформах.

| Релиз | Android | iPhone |
|---|---|---|
| **v1.1** | [KOCHWeather-1.1.apk](../../releases/download/v1.1/KOCHWeather-1.1.apk) (8.2 МБ) | [KOCHWeather-1.1-ios.ipa](../../releases/download/v1.1/KOCHWeather-1.1-ios.ipa) (20.8 МБ, iOS 16.3+) |

## Как устроен код

```
shared/                     ← весь продукт: один код на обе платформы
  src/commonMain/           ← 100% UI (Compose Multiplatform), данные, сеть, DataStore,
                               лутбокс, реклама, облака, темы, навигация — всё здесь
  src/androidMain/          ← Android-специфика: osmdroid-карта, BitmapFactory, res/
  src/iosMain/              ← iOS-специфика: MapKit + OSM-тайлы, AVPlayer (звук),
                               Inter-шрифты, dispatcher на dispatch_after
androidApp/                 ← Android-приложение (бывшая папка app/)
iosApp/                     ← iOS-приложение: SwiftUI-хост + Info.plist
scripts/ios-build.sh        ← сборка iOS БЕЗ xcodebuild (swiftc + ad-hoc подпись)
_history/                   ← отчёты ролей порта по фазам + judge-вердикты
```

Android-разработка никуда не пропала: приложение живёт в `androidApp/`, собирается
тем же Gradle, 250 юнит-тестов зелёные. Прежний монолит `app/` переехал туда при
реструктуризации (история — в коммитах этого репозитория и в ветке `ios`).

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
./scripts/ios-build.sh --device --ipa --release # IPA
```

**Общие тесты**: `./gradlew :shared:allTests`

## Статус порта

Judge-гейты пройдены по фазам (отчёты в `_history/reports/`): реструктуризация KMP,
данные+UI в commonMain, ресурсы/темы, карта 1:1, QA-свейп (feature/offline/performance/
security/localization — PASS). Проверено на симуляторе: вся лента, 6 тем, лутбокс
(5 попыток, история, рестарт сохраняет), реклама (первый показ — лев, крестик 3/5 с,
автозакрытие видео), вуаль/облачка по таймерам, карта с OSM-тайлами и пином.
Что честно не проверено на живом iPhone: звук на слух, haptics, живучесть сессии >5 мин,
deep link в Яндекс Go — см. `_history/reports/final/HANDOFF.md`.

## Лицензия

Код — MIT. Фоны — Wikimedia Commons (см. ATTRIBUTIONS.md). Рекламные картинки и ролики —
из папки `changes/` заказчика, права не проверялись.
