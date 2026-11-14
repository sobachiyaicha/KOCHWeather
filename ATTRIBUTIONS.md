# Атрибуция изображений

Фоны тем приложения KOCHWeather — локальные файлы в
`app/src/main/res/drawable-nodpi/` (1080x1920, JPEG). Они скачаны с Wikimedia
Commons и изменены; исходные данные ниже взяты из Wikimedia Commons API
(`prop=imageinfo&iiprop=extmetadata`: `Artist`, `LicenseShortName`,
`LicenseUrl`) и с страниц файлов. Ничего не добавлено «от себя».

Доступ к ресурсам — расширение `AppThemeMode.backgroundRes`
(`app/src/main/kotlin/com/koch/weather/data/model/ThemeBackgrounds.kt`).

## Таблица источников

| Тема | Файл Commons | Автор | Лицензия | Страница файла |
| --- | --- | --- | --- | --- |
| `BLUE_SKY` — `theme_blue_sky.jpg` | White Cumulus Clouds against Blue Sky (2).jpg | Lance Vanlewen | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:White_Cumulus_Clouds_against_Blue_Sky_%282%29.jpg) |
| `COPPER_SUNSET` — `theme_copper_sunset.jpg` | Red cloudy sunset - Ontario, Canada.jpg | David Zhang | CC BY-SA 2.0 | [Commons](https://commons.wikimedia.org/wiki/File:Red_cloudy_sunset_-_Ontario,_Canada.jpg) |
| `FOREST` — `theme_forest.jpg` | Forest, Taiga, Krasnoyarsk Sea, Russia.jpg | Vyacheslav Argenberg | CC BY 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Forest,_Taiga,_Krasnoyarsk_Sea,_Russia.jpg) |
| `MARS` — `theme_mars.jpg` | PIA26554A-Mars-CuriosityRover-UbajaraSite-20230430.jpg | NASA/JPL-Caltech/MSSS | Public domain (NASA/JPL) | [Commons](https://commons.wikimedia.org/wiki/File:PIA26554A-Mars-CuriosityRover-UbajaraSite-20230430.jpg) |
| `CYBERPUNK` — `theme_cyberpunk.jpg` | Dotonbori, Osaka, at night, November 2016.jpg | Martin Falbisoner | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Dotonbori,_Osaka,_at_night,_November_2016.jpg) |
| `BLOODY_SUNSET` — `theme_bloody_sunset.jpg` | Crimson clouds at sunset.jpg | Yuri Levchenko | CC BY 2.0 | [Commons](https://commons.wikimedia.org/wiki/File:Crimson_clouds_at_sunset.jpg) |
| `AURORA_BOREALIS` — `theme_aurora_borealis.jpg` | Aurora in Abisko near Torneträsk.jpg | Pavel.shyshkouski | CC BY-SA 4.0 | [Commons](https://commons.wikimedia.org/wiki/File:Aurora_in_Abisko_near_Tornetr%C3%A4sk.jpg) |

Тексты лицензий: [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/),
[CC BY-SA 2.0](https://creativecommons.org/licenses/by-sa/2.0/),
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/),
[CC BY 2.0](https://creativecommons.org/licenses/by/2.0/).

## Что изменено в каждом файле

Файлы — производные произведения (derivative works). Для каждой из 7 фотографий
выполнено одно и то же:

1. скачан оригинал с `upload.wikimedia.org` (JPEG, ширина от 1896 до 12144 px);
2. EXIF-поворот, приведение к RGB;
3. центральный кроп под 9:16;
4. ресайз до 1080x1920 (Lanczos);
5. размытие `ImageFilter.GaussianBlur(radius=6)`;
6. затемнение `ImageEnhance.Brightness(img).enhance(0.88)`;
7. сохранение JPEG с оптимизацией и прогрессивным режимом.

Исходные пропорции и кадрирование описаны в
`_history/reports/phase-5/data-coder-assets.md`.

## Оговорки по лицензиям

- **MARS.** У файла на Commons нет машинно-читаемого `LicenseShortName`; лицензия
  задана шаблоном `{{JPL Image Copyright}}` и категорией `PD NASA` (NASA/JPL —
  public domain). Материал NASA не подразумевает одобрения NASA или JPL
  продукта KOCHWeather.
- **CC BY / CC BY-SA.** Для работ под CC BY и CC BY-SA указаны автор и лицензия,
  а также отмечено, что файлы изменены (см. блок «Что изменено»). Производные
  файлы распространяются на тех же условиях CC BY-SA, что и оригиналы.
- **`ImageCatalog`.** Сетевые превью Wikimedia 500 px (`SKY_*` и другие категории)
  остались в коде для рекламы и витрин и к фонам тем больше не относятся.
