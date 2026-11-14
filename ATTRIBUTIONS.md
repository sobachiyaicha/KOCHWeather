# Атрибуция изображений

## Фоны тем (текущая версия)

7 фонов — фотографии с Wikimedia Commons, 1080×1920 JPEG, с размытием 4 px и
зерном. Встроены в APK, доступ через `AppThemeMode.backgroundRes`
(`data/model/ThemeBackgrounds.kt`).

| Тема | Файл Commons | Автор | Лицензия |
| --- | --- | --- | --- |
| `BLUE_SKY` | White Cumulus Clouds against Blue Sky (2).jpg | Lance Vanlewen | CC BY-SA 4.0 |
| `COPPER_SUNSET` | Red cloudy sunset - Ontario, Canada.jpg | David Zhang | CC BY-SA 2.0 |
| `FOREST` | Forest, Taiga, Krasnoyarsk Sea, Russia.jpg | Vyacheslav Argenberg | CC BY 4.0 |
| `MARS` | PIA26554-Mars-CuriosityRover-UbajaraSite-20230430.jpg (панорама 29163×7446, из неё кроп) | NASA/JPL-Caltech/MSSS | Public domain (NASA/JPL) |
| `CYBERPUNK` | Dotonbori, Osaka, at night, November 2016.jpg | Martin Falbisoner | CC BY-SA 4.0 |
| `BLOODY_SUNSET` | Crimson clouds at sunset.jpg | Yuri Levchenko | CC BY 2.0 |
| `AURORA_BOREALIS` | Aurora in Abisko near Torneträsk.jpg | Pavel.shyshkouski | CC BY-SA 4.0 |

Страницы файлов — на Wikimedia Commons (поиск по имени файла). Тексты лицензий:
[CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/),
[CC BY-SA 2.0](https://creativecommons.org/licenses/by-sa/2.0/),
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/),
[CC BY 2.0](https://creativecommons.org/licenses/by/2.0/).

### Что изменено

1. скачан исходник с `upload.wikimedia.org` (ширина от 3872 до 29163 px; превью
   сервиса — не крупнее 3840 px, поэтому для панорамы MARS взят оригинал 41 МБ);
2. EXIF-поворот, приведение к RGB;
3. центральный кроп под 9:16 (для MARS — вырезка 2304×4080 без чёрных полей);
4. ресайз до 1080x1920 — ровно экран телефона (Lanczos), чтобы не было второго
   размытия от ресемплинга на GPU;
5. размытие `GaussianBlur(radius=4)` в пикселях экрана;
6. монохромное зерно σ=1.2: без текстуры глаз читает размытие как артефакт
   сжатия, с зерном — как мягкий фокус;
7. сохранение JPEG, качество 93, 4:2:0, оптимизация, прогрессивный режим.

Исходные кропы без размытия лежат в `theme-bg-v2/` (рабочая папка, не в APK).

### Оговорки

- **MARS.** Машинно-читаемого `LicenseShortName` у файла нет; лицензия задана
  шаблоном `{{JPL Image Copyright}}` и категорией `PD NASA`. Материал NASA не
  подразумевает одобрения NASA или JPL продукта KOCHWeather.
- **CC BY / CC BY-SA.** Указаны автор и лицензия, файлы изменены (см. выше).
  Производные распространяются на тех же условиях, что и оригиналы.

## Альтернатива: сгенерированные обои

В репозитории лежит генератор `tools/theme-backgrounds.py` (сетчатые градиенты
из палитр тем, без фотографий) и его вывод в `theme-bg-graphic/`. В APK **не
входят**: сейчас используются фотографии. Если переключимся на графику,
атрибуция сторонних авторов станет не нужна.

## Прочие изображения

- **`ImageCatalog`** — сетевые превью Wikimedia (500 px) для рекламных
  креативов и витрин; к фонам тем отношения не имеют.
- **Иконки** — Material Icons (Apache 2.0), встроены через Compose.
- **Шрифты** — системные (Roboto/`sans-serif`), сторонних файлов нет.
