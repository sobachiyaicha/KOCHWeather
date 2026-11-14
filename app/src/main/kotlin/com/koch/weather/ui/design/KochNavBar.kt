package com.koch.weather.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.koch.weather.data.model.BottomTab
import com.koch.weather.ui.theme.KochBorderWidth
import com.koch.weather.ui.theme.KochShape
import com.koch.weather.ui.theme.KochSize
import com.koch.weather.ui.theme.KochType
import com.koch.weather.ui.theme.LocalKochColors

/**
 * Светлый стеклянный таб-бар (refs/bottom_nav.png, 0:45): заливка из темы, тёмные подписи,
 * активный таб — пилюля + акцентная иконка. Высота 64 dp, форма — пилюля.
 */
@Composable
fun KochNavBar(
    selectedTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKochColors.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(KochSize.nav),
        shape = KochShape.pill,
        color = colors.navFill,
        border = BorderStroke(KochBorderWidth.hairline, colors.borderStrong),
        shadowElevation = 12.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KochNavItem(
                label = "Главная",
                selected = selectedTab == BottomTab.HOME,
                activeIcon = Icons.Filled.Home,
                inactiveIcon = Icons.Outlined.Home,
                onClick = { onTabSelected(BottomTab.HOME) }
            )
            KochNavItem(
                label = "Карта",
                selected = selectedTab == BottomTab.MAP,
                activeIcon = Icons.Filled.Map,
                inactiveIcon = Icons.Outlined.Map,
                onClick = { onTabSelected(BottomTab.MAP) }
            )
            KochNavItem(
                label = "Вид",
                selected = selectedTab == BottomTab.THEMES,
                activeIcon = Icons.Filled.Palette,
                inactiveIcon = Icons.Outlined.Palette,
                onClick = { onTabSelected(BottomTab.THEMES) }
            )
            KochNavItem(
                label = "Список",
                selected = selectedTab == BottomTab.LIST,
                activeIcon = Icons.AutoMirrored.Filled.FormatListBulleted,
                inactiveIcon = Icons.AutoMirrored.Filled.FormatListBulleted,
                onClick = { onTabSelected(BottomTab.LIST) }
            )
            KochNavItem(
                label = "Поиск",
                selected = selectedTab == BottomTab.SEARCH,
                activeIcon = Icons.Filled.Search,
                inactiveIcon = Icons.Outlined.Search,
                onClick = { onTabSelected(BottomTab.SEARCH) }
            )
        }
    }
}

@Composable
private fun KochNavItem(
    label: String,
    selected: Boolean,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    onClick: () -> Unit
) {
    val colors = LocalKochColors.current
    val contentColor = if (selected) colors.navActive else colors.navContent

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = if (selected) 44.dp else 28.dp, height = 24.dp)
                .clip(KochShape.pill)
                .background(
                    if (selected) colors.navActive.copy(alpha = 0.14f)
                    else androidx.compose.ui.graphics.Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) activeIcon else inactiveIcon,
                contentDescription = label,
                tint = contentColor.copy(alpha = if (selected) 1f else 0.78f),
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            style = KochType.tab,
            color = contentColor.copy(alpha = if (selected) 1f else 0.72f),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}
