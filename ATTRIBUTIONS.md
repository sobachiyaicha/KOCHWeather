# Атрибуция изображений

## Фоны тем

В APK шесть тем. Пять фонов — фотографии с Wikimedia Commons, приведённые к 1080×1920
JPEG (размытие 4 px + зерно), доступ через `AppThemeMode.backgroundRes`
(`data/model/ThemeBackgrounds.kt`).

| Тема | Файл Commons | Автор | Лицензия |
| --- | --- | --- | --- |
| `COPPER_SUNSET` | Red cloudy sunset - Ontario, Canada.jpg | David Zhang | CC BY-SA 2.0 |
| `MARS` | PIA26554-Mars-CuriosityRover-UbajaraSite-20230430.jpg (панорама 29163×7446, из неё кроп) | NASA/JPL-Caltech/MSSS | Public domain (NASA/JPL) |
| `CYBERPUNK` | Dotonbori, Osaka, at night, November 2016.jpg | Martin Falbisoner | CC BY-SA 4.0 |
| `AURORA_BOREALIS` | Aurora in Abisko near Torneträsk.jpg | Pavel.shyshkouski | CC BY-SA 4.0 |
| `BLOODY_SUNSET` | без обоев — только градиент темы | — | — |

Страницы файлов — на Wikimedia Commons (поиск по имени файла). Тексты лицензий:
[CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/),
[CC BY-SA 2.0](https://creativecommons.org/licenses/by-sa/2.0/),
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/),
[CC BY 2.0](https://creativecommons.org/licenses/by/2.0/).

### Что изменено

1. скачан исходник с `upload.wikimedia.org` (ширина от 3872 до 29163 px; превью сервиса —
   не крупнее 3840 px, поэтому для панорамы MARS взят оригинал 41 МБ);
2. EXIF-поворот, приведение к RGB;
3. центральный кроп под 9:16 (для MARS — вырезка 2304×4080 без чёрных полей);
4. ресайз до 1080x1920 (Lanczos);
5. размытие `GaussianBlur(radius=4)` и монохромное зерно σ=1.2;
6. сохранение JPEG q90, 4:2:0, прогрессивный режим.

## Фон по умолчанию и тема «Голубое небо»

`theme_default.jpg` и `theme_default_preview.jpg` — фотография пса из папки `changes/`
заказчика. Источник и права не проверялись; распространяется вместе с приложением «как есть».

## Реклама и советы

`ad_photo1.jpg`, `ad_photo2.jpg`, `ad_son.jpg`, `ad_slippers.jpg`, `advice_1..3.jpg`,
`res/raw/ad_lion.mp4`, `res/raw/ad_slippers.mp4`, `res/raw/umbrella_face.mp4` — материалы
из папки `changes/` заказчика (скриншоты и ролики, в том числе со сторонними водяными знаками).
Права не проверялись, лицензии не подтверждены.

## Прочие изображения

- **`ImageCatalog`** — сетевые превью Wikimedia (500 px) для рекламных креативов и витрин;
  к фонам тем отношения не имеют.
- **Иконки** — Material Icons (Apache 2.0), встроены через Compose.
- **Шрифт Inter** — SIL Open Font License, файл лицензии в `app/src/main/assets/fonts/`.

Материал NASA не подразумевает одобрения NASA или JPL этого приложения.
