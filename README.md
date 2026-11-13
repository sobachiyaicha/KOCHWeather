# KOCHWeather — «Погода 2025»

Android-приложение-сатира на погодные сервисы, собранное по референс-видео:
живые карточки (Дубай, лутбоксы, такси, еда), премиум-пейволл, рекламный баннер,
рулетка тем, жест «прогони облака пальцем», модалка «про траву», нижний таб-бар.

## Стек

- Kotlin + Jetpack Compose + Material3, `minSdk 24`, Gradle Kotlin DSL
- kotlinx.serialization, Ktor/OkHttp, DataStore, Coil
- Данные: [Open-Meteo](https://open-meteo.com) — прогноз и геокодинг городов, без API-ключей
- Офлайн: кэш последнего прогноза в DataStore + баннер «нет сети»

## Структура

| Путь | Что внутри |
|------|------------|
| `app/src/main/kotlin/com/koch/weather/` | исходники: `data/` (api, cache, model, repository), `ui/` (main, theme, components) |
| `app/src/test/kotlin/` | unit-тесты (ViewModels, сериализация Open-Meteo, WMO-коды) |
| `refs/` | эталонные кадры из референс-видео + `SCREENS.md` с описанием экранов |
| `refs_src/` | исходное видео (только чтение, не модифицировать) |
| `shots/` | скриншоты с реального устройства, замеры `gfxinfo`, логи `logcat` |
| `artifacts/` | собранный `app-debug.apk` |
| `.agents/agents/` | роли субагентов: `ui-coder`, `data-coder`, `device-qa`, `feature-qa`, `ui-critic`, `ref-critic`, `judge` |
| `_history/` | история разработки: полные транскрипты всех бесед, описание процесса и текущее состояние |
| `AGENTS.md` | правила проекта: границы, источники правды, роли, цикл сдачи |

## Сборка

```powershell
.\gradlew.bat assembleDebug
```

- Путь к Android SDK и Gradle home задаются в `local.properties` и `gradle.properties`
  (`gradle.user.home=D:/.gradle`, чтобы не занимать системный диск).
- APK появится в `app\build\outputs\apk\debug\app-debug.apk`;
  последняя собранная версия уже лежит в `artifacts\app-debug.apk`.
- Проверки перед сдачей: `.\gradlew.bat compileDebugKotlin` и `.\gradlew.bat testDebugUnitTest`.

## Установка на устройство

```powershell
adb install -r artifacts\app-debug.apk
adb shell am start -n com.koch.weather/.MainActivity
```

Целевое устройство — Sony Xperia XZ1 (`G8342`), adb-серийник `CB512FQDXN`.
Скриншоты и замеры складываются в `shots/`.

## Как ведётся работа

1. `AGENTS.md` — правила и границы, `_history/workflow.md` — роли, фазы и цикл приёмки.
2. Код пишут только `ui-coder` и `data-coder`; сборку и устройство трогает только `device-qa`;
   визуал сверяют `ui-critic` и `ref-critic`; итог приёмки выносит `judge`.
3. Между фазами — остановка до подтверждения; один коммит на фазу.
4. «Готово» подтверждается скриншотом с телефона, путём APK и логами, а не словами.

## История и состояние

- `_history/index.md` — все беседы разработки со ссылками.
- `_history/STATE.md` — на чём остановились, что не закоммичено, что осталось.
- `_history/workflow.md` — устройство процесса (роли, quality gate, фазы).
