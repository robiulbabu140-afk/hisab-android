package com.hisab.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hisab.app.ui.components.HisabCard
import com.hisab.app.ui.components.ScreenHeader
import com.hisab.app.ui.navigation.Dest

@Composable
fun MoreScreen(navController: NavController) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("More")
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            MoreRow("💳", "Accounts", "Manage all balances") { navController.navigate(Dest.ACCOUNTS) }
            MoreRow("🏷️", "Categories", "Customize your categories") { navController.navigate(Dest.CATEGORIES) }
            MoreRow("👤", "Clients (Boosting)", "Due ও payment রেকর্ড করুন") { navController.navigate(Dest.CLIENTS) }
            MoreRow("💵", "Dollar Sale Buyers", "Due ও payment রেকর্ড করুন") { navController.navigate(Dest.DOLLAR_SALES) }
            MoreRow("🤝", "Managed Money", "Received/Paid এন্ট্রি করুন") { navController.navigate(Dest.MANAGED) }
            MoreRow("⚙️", "Settings", "App preferences") { navController.navigate(Dest.SETTINGS) }
        }
    }
}

@Composable
private fun MoreRow(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    HisabCard(modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clickable(onClick = onClick)) {
        Text("$icon  $title")
        Text(subtitle, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
    }
}
