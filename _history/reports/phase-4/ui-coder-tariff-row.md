# ui-coder — тарифы такси без LazyRow (perf-qa P1)

**Дата:** 14.11.2026.

`ui/components/TaxiBottomSheet.kt`: `LazyRow` с тремя тарифами заменён на обычный `Row`.

**Было:**
```kotlin
LazyRow(
    horizontalArrangement = Arrangement.spacedBy(KochSpace.s),
    contentPadding = PaddingValues(vertical = KochSpace.xs)
) { items(TaxiTariffType.values().toList(), key = { it.name }) { … } }
```
**Стало:**
```kotlin
Row(
    modifier = Modifier.padding(vertical = KochSpace.xs),
    horizontalArrangement = Arrangement.spacedBy(KochSpace.s)
) { TaxiTariffType.values().forEach { tariff -> TariffCard(…) } }
```

Обоснование: 3 × 112 dp + 2 × 8 dp = **352 dp** при доступных 353 dp (393 − 2×20) — прокрутка не
нужна; LazyRow добавлял контейнер прокрутки и ленивый measure в первом кадре листа.
`KochCard` у маршрутов, `shadowElevation = 24 dp` и `contentReady`-отложенность не затрагивались.

**Сборка:** `compileDebugKotlin` → **BUILD SUCCESSFUL in 8s**, 0 warnings, wall 8.6 s
(`.local/build_tariff_row.txt`).
