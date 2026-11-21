# ui-coder — этап 1 редизайна (токены + листы)

**Дата:** 14.11.2026. **План:** `_history/reports/phase-4/ui-design-plan.md`.
**Область этапа 1:** дизайн-система + 9 листов/диалогов + фиксы блокеров 1, 2, 5, 7, 8.
**Код правился только в `app/src/main/kotlin/com/koch/weather/ui/**`; `data/**` и `refs/` не тронуты.**

## 0. Доказательство сборки

```
JAVA_HOME=D:\jdk-17\jdk-17.0.12+7  TEMP=.local\tmp
.\gradlew.bat compileDebugKotlin
BUILD SUCCESSFUL in 8s
7 actionable tasks: 1 executed, 6 up-to-date
```
Лог: `.local/build_stage1.txt`. Предупреждений компилятора — **0** (было 2 «condition always true»,
исправлены). `assembleDebug`/install — за `device-qa`; холодный старт после установки проверит он же.

## 1. Токены (`ui/theme/Tokens.kt` — новый файл)

| Что | Было | Стало |
|---|---|---|
| Кегли | 23 значения, шаг 0.5 sp | **8 размеров**: 10 / 12 / 14 / 16 / 18 / 22 / 28 / 60 |
| Веса | Bold 96, Black 2 | 400 / 500 / 600 / 700 + 800 только у температуры-героя |
| Радиусы | 17 значений | 6 токенов: 8 / 16 / 20 / 24 / 28 / 32 + pill |
| Отступы | 1,2,3,4,5,6,8,10,12,14,16,18,20 | 4 / 8 / 12 / 16 / 20 / 24 / 32 |
| Высоты | 29 значений | cta 52, secondary 44, input 48, row 64, rowFood 96, chip 32, nav 64, thumb 72, hero 112 |
| Рамки | 0.6–2 dp, 9 цветов | `hairline 0.8`, `focus 1.5`, `dashed 1.5`; цвет — только из темы |
| Тени | цветные/неоновые | карточки 0; лист 24; навбар 12 |

`KochType` (16 именованных стилей), `KochSpace`, `KochRadius`, `KochShape`, `KochSize`,
`KochBorderWidth`, `KochScrim`.

## 2. Палитра (тоже токены — `ui/theme/Theme.kt`)

`KochColors` — 30 семантических ролей (`bgTop/bgUpper/bgLower/bgBottom`, `surface`, `surfaceAlt`,
`surfaceGlass/Strong`, `border/Strong`, `textPrimary/Secondary/Tertiary/Quaternary`, `accent`,
`accentPressed`, `accentPromo`, `accentStar`, `accentSuccess`, `accentDanger`, `accentAd*`,
`navFill/navContent/navActive`, `sheetSurface`, `capsuleFill`).

- **Синий** — измеренные стопы `#8EB3C7 → #579BBB → #6AAED0 → #9ACCEC`, стекло `#1AFFFFFF`,
  кант `#33FFFFFF`, действие `#0A8AF0`, капсула `#E9BF60`, навбар `#A6FFFFFF` + `#21445C`.
- **Медный** — `#D5967E → #B07258 → #A1624A → #8F5240`, поверхность `#4B2A20`, кант `#6E4433`,
  навбар `#F2E6DC` + `#4A2A1E` / акцент `#C2552D`.
- **Производные** 5 тем считаются одной функцией `derived()` (фон + 55 % затемнение + белые
  канты), отдельных хардкодов больше нет. `Color.kt` очищен (30 мёртвых констант удалены).
- `LocalKochColors` + `ProvideKochColors(mode)`; `MainScreen` оборачивает всё дерево, листы
  читают палитру из CompositionLocal.

## 3. Примитивы (`ui/design/`, 8 групп + утилиты)

| Файл | Что внутри |
|---|---|
| `KochSheet.kt` | `KochSheet(variant: Bottom/FullScreenInset, blocking, hero, content, footer)` — граббер, скрим 45 %, радиус 32, `navigationBarsPadding`, опциональное блокирование; `KochConfirmSheet(...)` — единое подтверждение |
| `KochCard.kt` | `KochCard(material: Glass/Solid/Overlay, shape, padding, onClick)` + `KochPanel` — кант + верхний блик, без цветных обводок |
| `KochButtons.kt` | `KochPrimaryButton` (52, pill, pressed/disabled/loading), `KochSecondaryButton`, `KochTextButton`, `KochIconButton`, `KochChevron` |
| `KochChip.kt` | `KochChip` — ширина по контенту, `selected`/`locked`, **без обрезки**; `KochColorDot` |
| `KochListRow.kt` | `KochListRow` 64/72/96 dp, разделитель 0.5 dp, `selected` = заливка + полоска 2 dp; `KochEmptyState` |
| `KochHeader.kt` | `KochDragHandle`, `KochCloseButton`, `KochSection`, `KochPageHeader` 28/700 |
| `KochNavBar.kt` | светлый стеклянный навбар: 5 табов, активный = пилюля 44×24 + accent |
| `KochBackground.kt` | `KochSkyBackground` — градиент темы + процедурные облака на `drawWithCache` (для этапа 2) |
| `Formatters.kt` | `formatRuAmount` («34 500»), `formatByn` (без `1120.0`) |

## 4. Переведённые листы (9 + deep-link confirm)

| Лист | Что стало |
|---|---|
| **Такси** | `KochSheet`; хедер с честной строкой пробок («Нет данных о пробках» без цифры при `null`); 4 пункта — одна группа строк с разделителями, выбранный = заливка+полоска; тарифы — горизонтальные карточки 104×92; CTA-капсула; активная поездка — карточка; подтверждение deep link — `KochConfirmSheet`. Цветных рамок нет |
| **Еда** | чипы ресторанов по контенту; строки меню 96 dp с фото 72×72 **только своего блюда** (`foodPhotoUrl` → плейсхолдер); полные описания (2 строки); степпер 32 dp; CTA «Заказать N шт. на X ₽» / disabled-состояние; лист скроллится целиком, третья позиция не режется |
| **Лутбоксы** | янтарный ореол вместо фиолетовой панели, герой 152 dp, история дропов, пустое состояние, синяя CTA; высота по контенту (пустоты ~500 px больше нет) |
| **Билеты** | фото-герой 112 dp (Дубай) + скрим, офферы строками (цена в одну строку, `1 120 BYN` без `.0`), погода третьей строкой без наложения, чипы мест, билет-карточка с штрихкодом |
| **Премиум** | витрина + **шаг подтверждения**: «Купить» → `requestPremiumPurchase()`, «Подтвердить» → `confirmPremiumPurchase()`, «Отмена»/dismiss → `cancelPremiumPurchase()`; `deprecated purchasePremium()` больше не вызывается; статус «★ ПРЕМИУМ АКТИВЕН» строкой |
| **Трава** | блокирующий лист 62 %, без иконки и крестика, пунктирная зона (dash 6/4) с «Загрузите фото травы», синяя капсула «Открыть камеру», статус загрузки внутри зоны |
| **Рулетка тем** | инсет-лист по `congrats.png`: «Поздравляем!», карточки «Оформление …» сверху/снизу, **фронтальный мокап телефона** (не задняя крышка), жёлтые треугольники `#FFD600` (Canvas, без кругов), чипы по контенту, автопрокрут; **«Продолжить» disabled для закрытой темы**, закрытые не применяются |
| **Реклама** | фото-герой + метка «РЕКЛАМА», тексты рефа, CTA в премиум; зелёная рамка и квадратная иконка удалены |
| **Зонты** | карточка товара, 3 кружка-расцветки, цена, капсула «Купить»; локальное состояние покупки сохранено |
| **Deep link** | taxi/food — `KochConfirmSheet` вместо `AlertDialog`, кнопки рабочие, веб-фолбэк и тексты ошибок сохранены |

## 5. Блокеры

| № | Статус |
|---|---|
| 1 | ✅ `Scaffold(bottomBar = KochNavBar)`, `contentPadding` = `innerPadding + 8`; `navigationBarsPadding` из списка убран. Контент не может уехать под бар |
| 2 | ✅ FPS выключен по умолчанию; включается долгим нажатием по футеру источника; 44×16, 9 sp, левый низ над баром; `BuildConfig.DEBUG` |
| 3 | ✅ чипы по контенту (`KochChip` без `weight`), «Изумрудный лес» целиком; ряд скроллится |
| 4 | ⏳ этап 2 (облака) — в плане §6.15: вуаль 45 %, подсказка без плашки, z-порядок с баннером |
| 5 | ✅ явная таблица `id → слот фото`; фри/наггетсы/пирожок/додстер/бамбл получают плейсхолдер, а не чужое фото |
| 6 | ✅ закрытая тема: CTA disabled «Тема закрыта», тап по чипу только превью; автопрокрут — только по `availableThemes` |
| 7 | ✅ двухшаговый пейволл, `purchasePremium()` не вызывается |
| 8 | ✅ все листы этапа 1 собраны только из примитивов и токенов |

## 6. Замечания visual-critic

| № | Статус |
|---|---|
| 1 облака | этап 2 |
| 2 главная/порядок | этап 2 |
| 3 бар | ✅ светлое стекло темы, контент не уходит под него |
| 4 рулетка | ✅ переделана; **CTA синяя `#0A8AF0`, не белая — по замеру `congrats.png` (`#0B8DF1`)**. Если критик настаивает на белой pill — это одна строка в `PaywallDialog`/`ThemeRouletteDialog` |
| 5 фото/обрезки | ✅ в листах; travel-карточка — этап 2 (главная) |
| 6 FPS | ✅ |
| 7 материал/температура | частично: листы на новых материалах; фото-фон, «тёмное стекло», тонкая температура — этап 2 (шапка и карточки главной) |
| 8 сборка | ✅ compile OK; install/холодный старт — `device-qa` |

## 7. Файлы

**Новые (10):** `ui/theme/Tokens.kt`, `ui/design/{KochSheet,KochCard,KochButtons,KochChip,KochListRow,KochHeader,KochNavBar,KochBackground,Formatters}.kt`.
**Переписаны:** `TaxiBottomSheet`, `FoodBottomSheet`, `LootboxDialog`, `FlightBookingDialog`, `PaywallDialog`, `GrassLockDialog`, `ThemeRouletteDialog`, `AdPromoDialog`, `UmbrellaShopDialog`, `FpsOverlay`, `Theme.kt`, `Type.kt`, `ThemeVisuals.kt`, `Color.kt`.
**Точечно:** `MainScreen.kt` — `ProvideKochColors`, `Scaffold` + `KochNavBar` (удалены `FullSizeBottomBar`/`BottomTabItem`), FPS по жесту, новые параметры листов, отступ 20 dp. `CityPickerDialog`, `DailyDetailDialog`, `CityListScreen`, `CitySearchScreen`, `ThemeGalleryScreen`, `CloudVeilOverlay`, `WeatherMapScreen` **не тронуты** (этап 2).

## 8. Этап 2 (не начат)

1. Главная: порядок блоков и развёрнутый hourly по `clean_main.png`; зонтики после «Пн»; AI-факт без плашки; реклама с фото справа; сервисы 2×2 без фото Дубая; тёмное стекло/тонкая температура — по замечанию критика.
2. Облака: вуаль только над шапкой, статус-бар читаем, без прогресса/процентов, z-порядок.
3. Страницы «Список», «Поиск», «Вид», «Карта» на `KochPageHeader`/`KochCard`/`KochChip`.
4. `CityPickerDialog` → переход на «Список»; `DailyDetailDialog` — лист.
5. Шрифт Inter (после решения оркестратора), иконки `Outlined` в неактивных состояниях.

## 9. Отклонения от плана (осознанные)

1. **`KochSheet` построен на `Dialog` + нижний `Surface`**, а не на M3 `ModalBottomSheet`:
   нужен гарантированный контроль над блокировкой («Трава»), инсетами и отсутствием
   частичного раскрытия; M3-шит мог сломать R11. Визуальный контракт (низ, радиус 32, скрим
   45 %, граббер) соблюдён. Drag-to-dismiss не реализован — закрытие тапом по скриму/назад.
2. **Рулетка**: центральный объект — мокап из Compose, потому что единственное фото iPhone
   в каталоге — задняя крышка (критик её запретил). Параметр `iphoneImageUrl` удалён.
3. **CTA рулетки синяя** `#0A8AF0` — по замеру кадра (у критика «белая pill»); см. вопрос 1.

## 10. Вопросы

1. Принять ли синюю CTA в рулетке (по замеру) вместо «белой pill» из замечания критика.
2. Рулетка: центральный объект — мокап из Compose (фронт). Реальное фото iPhone в каталоге —
   **задняя крышка**, критик её запретил; нужен URL фронта от `data-coder` либо согласие на мокап.
3. Фото фри/наггетсов/пирожка/додстера/бамбла: нужны URL, до этого — плейсхолдеры.
