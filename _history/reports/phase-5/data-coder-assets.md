# data-coder B — локальные фоны тем (пункт 1 замечаний Фазы 5)

Дата: 2026-11-14. Роль: `data-coder` (ассеты). Ветка/состояние: рабочее дерево, без коммита.

Задача: заменить сетевые превью Wikimedia 500 px (растягивались на 1080 px — «фон почти
всех тем в ОЧЕНЬ низком разрешении») на локальные, заранее размытые и затемнённые фото
1080x1920 для всех 7 тем, встроенные в APK.

## 1. Сделано

| Артефакт | Что это |
| --- | --- |
| `app/src/main/res/drawable-nodpi/theme_*.jpg` | 7 фонов 1080x1920, JPEG |
| `app/src/main/kotlin/com/koch/weather/data/model/ThemeBackgrounds.kt` | `val AppThemeMode.backgroundRes: Int?` (`@DrawableRes`), все 7 тем — ненулевой ресурс |
| `ATTRIBUTIONS.md` (корень) | таблица «тема -> файл Commons -> автор -> лицензия -> страница файла» |
| `.local/tmp/prepare_theme_backgrounds.py` | скрипт-обработчик (в git не идёт, `.local/` в `.gitignore`) |

Сетевые `ImageCatalog.SKY_*` не тронуты: они остаются в каталоге как есть, UI просто
перестаёт использовать их как фон (подключение `backgroundRes` — задача `ui-coder`).

## 2. Файлы: вес и разрешение

Проверено Pillow: все 7 — `JPEG`, ровно `1080x1920`.

| Файл | Тема | Разрешение | Вес |
| --- | --- | --- | --- |
| `theme_blue_sky.jpg` | `BLUE_SKY` | 1080x1920 | 162 113 B (158.3 КБ) |
| `theme_copper_sunset.jpg` | `COPPER_SUNSET` | 1080x1920 | 229 557 B (224.2 КБ) |
| `theme_forest.jpg` | `FOREST` | 1080x1920 | 239 332 B (233.7 КБ) |
| `theme_mars.jpg` | `MARS` | 1080x1920 | 218 118 B (213.0 КБ) |
| `theme_cyberpunk.jpg` | `CYBERPUNK` | 1080x1920 | 262 980 B (256.8 КБ) |
| `theme_bloody_sunset.jpg` | `BLOODY_SUNSET` | 1080x1920 | 228 017 B (222.7 КБ) |
| `theme_aurora_borealis.jpg` | `AURORA_BOREALIS` | 1080x1920 | 169 016 B (165.1 КБ) |
| **Итого** | | | **1 509 133 B = 1473.8 КБ = 1.44 МБ** |

Лимиты: 120-280 КБ на файл — выполнено (158.3-256.8 КБ); суммарно <= 1.6 МБ — выполнено
(1.44 МБ); размеры ровно 1080x1920 — выполнено.

Контрольные суммы (первые 12 символов MD5, для сверки при переносе):

```
theme_aurora_borealis.jpg  a54ad4cfd3a8
theme_bloody_sunset.jpg    dcfa6ff77c21
theme_blue_sky.jpg         28551eb445f8
theme_copper_sunset.jpg    b6a2d79032f5
theme_cyberpunk.jpg        68b70c5321f8
theme_forest.jpg           9249892d3128
theme_mars.jpg             c5d1bc9326c9
```

## 3. Источники и лицензии

Все — Wikimedia Commons, только свободные лицензии, реальные фотографии (без коллажей,
скриншотов и текстовых изображений). Данные — из API `extmetadata` (`Artist`,
`LicenseShortName`, `LicenseUrl`); полная таблица со ссылками — в `ATTRIBUTIONS.md`.

| Тема | Файл Commons | Автор | Лицензия | Оригинал | Кроп 9:16 |
| --- | --- | --- | --- | --- | --- |
| `BLUE_SKY` | White Cumulus Clouds against Blue Sky (2).jpg | Lance Vanlewen | CC BY-SA 4.0 | 4032x2268 | 1276 px |
| `COPPER_SUNSET` | Red cloudy sunset - Ontario, Canada.jpg | David Zhang | CC BY-SA 2.0 | 12144x6875 | 3867 px |
| `FOREST` | Forest, Taiga, Krasnoyarsk Sea, Russia.jpg | Vyacheslav Argenberg | CC BY 4.0 | 6000x4000 | 2250 px |
| `MARS` | PIA26554A-Mars-CuriosityRover-UbajaraSite-20230430.jpg | NASA/JPL-Caltech/MSSS | Public domain (NASA/JPL) | 1896x2110 | 1187 px |
| `CYBERPUNK` | Dotonbori, Osaka, at night, November 2016.jpg | Martin Falbisoner | CC BY-SA 4.0 | 3872x4738 | 2665 px |
| `BLOODY_SUNSET` | Crimson clouds at sunset.jpg | Yuri Levchenko | CC BY 2.0 | 6000x4000 | 2250 px |
| `AURORA_BOREALIS` | Aurora in Abisko near Torneträsk.jpg | Pavel.shyshkouski | CC BY-SA 4.0 | 5442x3628 | 2041 px |

У `MARS` на Commons нет машинно-читаемого `LicenseShortName`: лицензия задана шаблоном
`{{JPL Image Copyright}}` и категорией `PD NASA`. Так и записано в `ATTRIBUTIONS.md`,
без выдуманной «CC»-строки.

Кандидаты отбирались вручную по превью: у неба, закатов, тайги, Марса, ночного города и
сияния просмотрено 3-6 вариантов на тему, из них выбраны самые «тематичные» и с
равномерной яркостью (фон не должен «съедать» белый текст). Отклонены, например:
панорама Марса Pathfinder (чёрные углы сшивки), `A fiery sky over Paranal` (звёздное небо
вместо заката), `Neon Night 1` (крупная вывеска ODEON — читаемый текст в кадре).

## 4. Обработка

Скрипт: `.local/tmp/prepare_theme_backgrounds.py`, Python из рантайма DSH
(`C:\Users\admin\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe`,
Pillow 12.3.0). Для каждого файла:

1. скачивание оригинала `upload.wikimedia.org` с User-Agent
   `KOCHWeather/1.0 (android; +https://github.com/koch/weather)`;
2. EXIF-поворот (`ImageOps.exif_transpose`) -> RGB;
3. центральный кроп ровно 9:16;
4. ресайз до 1080x1920 (Lanczos);
5. `ImageFilter.GaussianBlur(radius=6)`;
6. `ImageEnhance.Brightness(img).enhance(0.88)`;
7. сохранение JPEG `optimize=True, progressive=True`, субдискретизация 4:4:4.

### Отклонения от ТЗ и почему

1. **Скачивался оригинал, а не превью `iiurlwidth=1920`.** У пейзажа 16:9 центральный
   кроп 9:16 от превью шириной 1920 даёт всего ~607 px, и его пришлось бы растягивать до
   1080 — то самое «низкое разрешение», с которого началась задача. Скрипт берёт
   оригинал и проверяет, что ширина кропа >= 1080 (фактически 1187-3867 px), то есть
   апскейла нет ни в одном файле. Если оригинал не JPEG/PNG, скрипт падает на превью
   1920 и всё равно проверяет размеры.
2. **Качество JPEG 92-94 вместо 82.** После `GaussianBlur(6)` в кадре почти нет
   высокочастотных деталей, и на q=82 файлы весят 54-127 КБ — ниже заданного минимума
   120 КБ, который тот же пункт требует. Скрипт подбирает лучшее качество из лестницы
   (94, 92, 90, ..., 82), при котором файл не вылезает за 280 КБ: 6 файлов ушли в q=94,
   `theme_forest.jpg` — в q=92. Качество только выросло, лимиты по весу соблюдены
   (158-257 КБ, сумма 1.44 МБ).

## 5. Проверки

Размеры и вес (Pillow + файловая система):

```powershell
& "C:\Users\admin\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe" -c @"
import os
from PIL import Image
res = r'D:\akcon\weatherAPK-dsh\app\src\main\res\drawable-nodpi'
tot = 0
for f in sorted(os.listdir(res)):
    p = os.path.join(res, f); b = os.path.getsize(p); tot += b
    im = Image.open(p)
    print(f, im.format, im.size, b, 'bytes')
print('TOTAL', tot, 'bytes', round(tot/1024/1024, 2), 'MB')
"@
```

Результат: 7 файлов `JPEG (1080, 1920)`, 1 509 133 B суммарно. Все проверки внутри
`prepare_theme_backgrounds.py` завершаются строкой `all checks passed` (при нарушении
лимита скрипт возвращает код 2 и печатает `PROBLEM: ...`).

Сборка ресурсов + Kotlin (без сборки APK):

```powershell
.\gradlew.bat --console=plain compileDebugKotlin
```

Результат: `BUILD SUCCESSFUL`, `EXITCODE=0` (проверено 2026-11-14 17:27). Задача
`:app:compileDebugKotlin` при этом реально исполнялась (5m 1s, инкрементальный кэш Kotlin
пришлось пересобрать после падения демона), следующий прогон — `UP-TO-DATE`.
Дополнительно: в `app/build/intermediates/compile_r_class_jar/debug/generateDebugRFile/R.jar`
есть все 7 id (`R.drawable.theme_blue_sky` ... `theme_aurora_borealis`), а в
`merged_res/debug/mergeDebugResources/` — 7 `.flat` наших JPEG, то есть aapt2 их принял.

Замечание для оркестратора: в 17:20 `compileDebugKotlin` падал не на моих файлах —
`TaxiModels.kt:61: Unresolved reference 'roundToInt'` (параллельная правка `data-coder` A,
импорт ещё не был добавлен). К 17:27 файл был исправлен, сборка прошла. Повторных
падений по моим изменениям не было.

## 6. Что осталось вне моей роли

- `ui-coder`: `KochSkyBackground`/`MainScreen` должны рисовать фон темы через
  `Image(painterResource(mode.backgroundRes))` вместо `sky*ImageUrl`; сетевую
  предзагрузку фона убрать; в галерее тем — превью локального фона.
- `visual-critic`: оценить читаемость белого текста поверх новых фонов на 7 темах
  (фоны затемнены на 12 %, но у `FOREST` сверху есть светлый участок неба — там нужен
  штатный верхний скрим).

## 7. Запреты, которые соблюдены

`refs/`, `refs_src/`, `ACCEPTANCE.md`, `_history/reports/phase-4/` не тронуты; коммита нет;
`adb` и `assembleDebug` не запускались; эмодзи в текстах нет; `ImageCatalog.SKY_*`
не изменён; все временные файлы — в `.local/tmp/`.
