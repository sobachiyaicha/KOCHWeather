# ui-coder — фон темы без удержания битмапов (правка по FAIL perf-qa, Фаза 5)
Дата: 2026-11-14, 21:00–21:24 MSK. Только `KochBackground.kt` и `ThemeVisuals.kt`.
Не коммитил, APK не собирал, adb не вызывал; `refs/`, `refs_src/`, `ACCEPTANCE.md`, `data/**` не менял.

## Дефект (perf-qa)
`painterResource` держит BitmapDrawable в кеше Resources: каждая тема навсегда оставляет
8.3 МБ (3 смены: GPU Image 19.7→35.6 МБ, PSS 186→219 МБ) + синхронный декод на UI-потоке.

## Что изменено
- `KochBackground.kt`: `BitmapFactory.decodeResource` (RGB_565, `inScaled=false`, `inMutable=false`)
  вместо `painterResource`; декод в `produceState(resId)` + `withContext(Dispatchers.IO)`.
- LRU на 2 записи (`resId`): вытесненный `Bitmap.recycle()`; кадры, которые ещё в композиции,
  учитываются через `DisposableEffect` и освобождаются после ухода с экрана.
- `Image(bitmap = remember(bitmap) { bitmap.asImageBitmap() }, Crop)`; пока новый кадр
  декодируется, показывается предыдущий. Скримы 40/30/48 % и верхние 32 dp не тронуты.
- RGB_565 = 4.15 МБ/кадр вместо 8.3. Квантование проверено локально на тех же JPEG со
  скримом (Pillow): Δ ср. 1.9/255, p99/max 5/255. Если visual-critic увидит полосы —
  ARGB_8888, лимит 2 записи сохранится.
- `ThemeVisuals.kt`: превью LruCache ≤ 8 (7 тем + запас, ~4 МБ), `inSampleSize=4`,
  `inScaled=false`; при ошибке декода — градиент, полный ресурс не грузится. Удалён
  `ThemeBackgroundImage` (последний painterResource на 1080×1920).

## Память и проверка
≤ 2 × 4.15 МБ битмапов + 2 GPU-текстуры; после 3 смен PSS ≈ +4 МБ, пик с декодом ≈ +8 МБ
(раньше +8 МБ за каждую тему).
`gradlew --console=plain compileDebugKotlin testDebugUnitTest` — BUILD SUCCESSFUL,
37 сьютов / 223 теста, 0 падений/ошибок/пропусков. Задачи были UP-TO-DATE: параллельный
ui-coder собрал этот tree в 21:21:31 и прогнал тесты в 21:21:51; `KochBackgroundKt` содержит
`ThemeBackgroundCache`/`DisposableEffect`; мой прогон в 21:07 выполнил тесты (37/223) до
финальной правки кеша.

## Как проверить на устройстве
1. `adb shell dumpsys meminfo com.koch.weather | findstr "TOTAL PSS"`.
2. Сменить тему 3 раза (Вид → тема → Главная); после каждой — meminfo и
   `adb shell dumpsys gfxinfo com.koch.weather | findstr "Image Scratch"`.
3. Ожидание: +≈4 МБ после первой смены, дальше без роста (±2 МБ), Image ≤ ~12 МБ;
   нет новых `Slow bitmap uploads`/`Slow UI thread` от смены темы.
## Ограничения
На G8342 не мерил (запрет adb), PSS-числа расчётные; RGB_565 проверен локальной
эмуляцией квантования, не скриншотами с телефона.
