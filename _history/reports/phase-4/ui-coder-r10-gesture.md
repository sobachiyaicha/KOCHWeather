# ui-coder — R10: конкуренция жеста «прогони облака» с pager'ом городов

**Дата:** 14.11.2026. **Сборка:** `compileDebugKotlin` → **BUILD SUCCESSFUL in 12s**, 0 warnings
(`.local/build_r10.txt`).

## Что было в run 8
Вуаль появляется, накрывает капсулу, после рестарта не возвращается — всё ок. Но **после
полного стирания** (1–2 свайп) оставшиеся свайпы того же жеста попадали в pager: `guard`
`return@pointerInput` работал только пока `veilVisible == true`; как только пелена исчезала,
следующий `pointerInput`-хендлер pager'а подписывался заново и получал хвост жеста → СПб → Дубай.

## Фикс (`ui/main/MainScreen.kt`)
1. Новое локальное состояние (без изменения контракта ViewModel):
   ```kotlin
   var veilCooldownUntil by remember { mutableLongStateOf(0L) }
   ```
2. В `CloudVeilOverlay.onCleared` (момент полного стирания):
   ```kotlin
   veilCooldownUntil = System.currentTimeMillis() + 1200L
   onCloudVeilDismissed(); onCloudsCleared()
   ```
3. В pager'е (`detectHorizontalDragGestures`) добавлена проверка на время:
   ```kotlin
   val pagerBlocked: () -> Boolean = { System.currentTimeMillis() < veilCooldownUntil }
   onHorizontalDrag = { _, amount -> if (!pagerBlocked()) dragAccumulator += amount }
   onDragEnd = { if (!pagerBlocked()) { …onNextCity/onPrevCity… }; dragAccumulator = 0f }
   ```
   `veilCooldownUntil` добавлен в ключи `pointerInput`, чтобы хендлер пересоздавался вместе
   с уходом вуали.

## Проверка логики (4 быстрых свайпа)
| Свайп | Что происходит |
|---|---|
| 1 | `veilVisible=true` → pager не подписан; облака стираются, прогресс растёт |
| 2 | прогресс ≥ порога (650 dp) → `onCleared`: ставится cooldown +1200 мс, вуаль исчезает |
| 3 | pager получил хвост жеста, но `pagerBlocked()==true` → dragAccumulator не копится, `onDragEnd` не листает |
| 4 | то же (в пределах 1200 мс) |
| через 1200 мс | обычное листание городов восстанавливается |

Итог: город остаётся Санкт-Петербургом, пелена уходит; кулдаун не мешает обычному свайпу по
шапке (он истекает за 1.2 с).

## Что проверить на финальном прогоне
1. 4 быстрых свайпа по шапке под вуалью — город тот же, облака исчезли.
2. Один свайп по шапке **без** вуали (спустя >1.2 с) — карусель городов листается как раньше.
3. Видео жеста ≥15 fps для R10.
