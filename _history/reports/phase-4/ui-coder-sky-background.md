# ui-coder — фото неба как фон главной

**Дата:** 14.11.2026. **Сборка:** `compileDebugKotlin` → **BUILD SUCCESSFUL in 7s**, 0 warnings
(`.local/build_sky.txt`). `data/**` не менялся.

## Контракт
Из state (data-coder): `uiState.skyBlueImageUrl`, `skySunsetImageUrl`, `skyNorthernImageUrl`
(`imageUrls[ImageCategory.SKY_*].firstOrNull()`).

## Что сделано

### `ui/design/KochBackground.kt` — `KochSkyBackground(modifier, imageUrl, showClouds)`
1. **База всегда** — градиент темы (`colors.gradient`). Он виден и до загрузки фото, и офлайн.
2. **Фото неба** — `AsyncImage` c `ContentScale.Crop`, `imageLoader` — общий Wikimedia-загрузчик
   (обязательный UA-заголовок, без crossfade). У `AsyncImage` намеренно **нет** `placeholder`/
   `error`: пока фото нет, сквозь него виден градиент — серый бокс невозможен.
3. **Скрим** 30 % сверху / 20 % в середине / 35 % снизу — рисуется **только после
   `AsyncImagePainter.State.Success`** (`onState`), поэтому офлайн фон не темнеет зря.
4. **Облака** — процедурный фолбэк: рисуются, пока фото не загрузилось (и всегда, если у темы
   фото нет). После успешной загрузки убираются — фотография сама даёт фактуру.
5. `ui/components/WeatherImage.kt` — загрузчик открыт как `wikimediaImageLoader(context)`
   (один OkHttp/Coil-инстанс на процесс, UA для upload.wikimedia.org).

### `ui/main/MainScreen.kt` — выбор фото по теме
```kotlin
val skyPhotoUrl = when (uiState.currentTheme) {
    AppThemeMode.BLUE_SKY -> uiState.skyBlueImageUrl
    AppThemeMode.COPPER_SUNSET -> uiState.skySunsetImageUrl
    AppThemeMode.AURORA_BOREALIS -> uiState.skyNorthernImageUrl
    else -> null
}
KochSkyBackground(modifier = Modifier.matchParentSize(), imageUrl = skyPhotoUrl)
```
Для FOREST / MARS / CYBERPUNK / BLOODY_SUNSET фото нет — остаётся градиент с бликом, виньеткой
и облаками (тот же код, что был).

## Офлайн-сценарий
`AsyncImage` без плейсхолдера/ошибки + облака до `Success` → при отсутствии сети на экране
полноценный процедурный градиент, а не серый прямоугольник и не пустота. Капсула, карточки и
таб-бар читаются: тёмные/светлые иконки статус-бара уже считаются от `bgTop`, стекло и белый
текст лежат на затемнённом фото (скрим 20–35 %).

## Производительность
Один полноэкранный `AsyncImage` (не в списке), без `SubcomposeAsyncImage`, без crossfade;
кеш-загрузчик общий с карточками. Облака исчезают после загрузки — лишних draw-вызовов на кадр
не добавляется.

## Замечание на следующий этап (вне этой правки)
Полноэкранные «Список»/«Поиск»/«Вид» принимают `background: Brush` и пока рисуют только
градиент. Если critic захочет фото и там — заменить параметр на `(@Composable () -> Unit)` или
переиспользовать `KochSkyBackground` внутри этих экранов (одна правка на экран).
