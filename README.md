# КОЧWeather

Перегруженное говном погодное приложение для Android на Kotlin + Jetpack Compose. Сделано как «погода,
в которую никогда не хочется заходить»: встроеное казино, реклама каждые 15 секунд, премиум подписка,
ии факты, оценка поогоды и полу-интеграция сервисов яндекса с карточками на пол экрана!

APK версии 1.0 — в разделе [Releases](../../releases).

- факт дня, который не меняется при перезапуске.

## Данные

| Что | Источник |
| --- | --- |
| Прогноз | [MET Norway](https://api.met.no/) — основной |
| Фолбэк | Open-Meteo, затем 7timer |
| Кэш | DataStore, последний успешный прогноз |
| Карта | osmdroid + тайлы OpenStreetMap |
| Такси | тарифы Яндекс Go, deep link `yandex-taxi://` |

Если данных нет — приложение честно пишет «Нет данных», а не подставляет
выдуманные значения. Все состояния (LIVE / CACHED / UNAVAILABLE) видны в UI.

## Сборка

Нужны JDK 17 и Android SDK (compileSdk 36).

```bash
./gradlew :app:assembleDebug        # debug
./gradlew :app:assembleBenchmark    # release-подобная сборка (R8, не debuggable)
```

Ключ для источника пробок необязателен: создайте `local.properties` со строкой

```properties
TRAFFIC_API_KEY=...
```

Без ключа приложение показывает «Нет данных о пробках», а не нули.

Тесты — `app/src/test`: 37 сьютов, 229 тестов (парсеры, политики, репозитории,
русские тексты, отсутствие эмодзи в UI).

## Лицензии

- Код — [MIT](LICENSE).
- Фоны тем — фотографии с Wikimedia Commons (CC BY / CC BY-SA / public domain,
  NASA/JPL для Марса). Авторы и лицензии: [ATTRIBUTIONS.md](ATTRIBUTIONS.md).
- Иконки Material — Apache 2.0.
- Данные — по лицензиям MET Norway, Open-Meteo, 7timer, OpenStreetMap.

Материал NASA не подразумевает одобрения NASA или JPL этого приложения.
