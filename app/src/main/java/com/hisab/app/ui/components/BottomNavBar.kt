package com.hisab.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.hisab.app.ui.navigation.Dest

data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

val bottomTabs = listOf(
    BottomTab(Dest.DASHBOARD, "Home", Icons.Filled.Home),
    BottomTab(Dest.TRANSACTIONS, "Transactions", Icons.Filled.SwapVert),
    BottomTab(Dest.ADD, "Add", Icons.Filled.Add),
    BottomTab(Dest.REPORTS, "Reports", Icons.Filled.BarChart),
    BottomTab(Dest.MORE, "More", Icons.Filled.MoreHoriz)
)

@Composable
fun HisabBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar {
        bottomTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onNavigate(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}
