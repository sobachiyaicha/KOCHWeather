package com.koch.weather.ui.components

import androidx.compose.runtime.Composable

/**
 * Шов системного «назад» (владелец — ui-porter, фаза 2).
 *
 * Замена `androidx.activity.compose.BackHandler`, которого нет в commonMain:
 *  - Android — activity-compose BackHandler (1:1 с исходником);
 *  - iOS — системного back нет: no-op. По port plan §5/§13 на iOS нельзя
 *    полагаться на BackHandler, все диалоги обязаны иметь кнопку закрытия
 *    (крестик/CTA) — она есть во всех диалогах этого пакета.
 */
@Composable
expect fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit)
