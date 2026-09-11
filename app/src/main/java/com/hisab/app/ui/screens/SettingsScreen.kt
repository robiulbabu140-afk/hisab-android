package com.hisab.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hisab.app.data.remote.SyncWorker
import com.hisab.app.ui.LocalAppContainer
import com.hisab.app.ui.components.HisabCard
import com.hisab.app.ui.components.ScreenHeader
import com.hisab.app.ui.navigation.Dest
import com.hisab.app.ui.theme.HisabGreen
import com.hisab.app.ui.theme.HisabMuted
import com.hisab.app.ui.theme.HisabRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    var baseUrl by remember { mutableStateOf(container.syncPrefs.baseUrl) }
    var apiKey by remember { mutableStateOf(container.syncPrefs.apiKey) }
    var autoSync by remember { mutableStateOf(container.syncPrefs.autoSyncEnabled) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var syncSucceeded by remember { mutableStateOf(true) }
    var lastSyncMillis by remember { mutableStateOf(container.syncPrefs.lastSyncMillis) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Settings", onBack = { navController.popBackStack() })
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {

            HisabCard {
                Text("Backend Sync", style = MaterialTheme.typography.titleMedium)
                Text(
                    "hisab-web backend-এর ঠিকানা ও API Key দিন (Settings পেজ, ওয়েব ড্যাশবোর্ডে) — এটা দিলে এই ফোনের হিসাব ওয়েব ড্যাশবোর্ডেও দেখা যাবে।",
                    color = HisabMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Backend URL (e.g. https://hisab.prothom.shop)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
                Button(
                    onClick = {
                        container.syncPrefs.baseUrl = baseUrl
                        container.syncPrefs.apiKey = apiKey
                        baseUrl = container.syncPrefs.baseUrl
                        syncMessage = "সেভ হয়েছে"
                        syncSucceeded = true
                    },
                    modifier = Modifier.padding(top = 14.dp)
                ) { Text("Save") }
            }

            HisabCard(modifier = Modifier.padding(top = 12.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Auto Background Sync", style = MaterialTheme.typography.bodyLarge)
                        Text("প্রতি ৩০ মিনিটে wifi/data থাকলে automatic sync", color = HisabMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = autoSync,
                        onCheckedChange = { checked ->
                            autoSync = checked
                            container.syncPrefs.autoSyncEnabled = checked
                            if (checked) SyncWorker.enable(context) else SyncWorker.disable(context)
                        }
                    )
                }

                if (lastSyncMillis > 0) {
                    val formatted = remember(lastSyncMillis) {
                        SimpleDateFormat("d MMM yyyy, h:mm a", Locale.US).format(Date(lastSyncMillis))
                    }
                    Text("সর্বশেষ sync: $formatted", color = HisabMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp))
                } else {
                    Text("এখনো sync হয়নি", color = HisabMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp))
                }

                if (isSyncing) {
                    Row(Modifier.padding(top = 12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 10.dp))
                        Text("Sync হচ্ছে...")
                    }
                } else {
                    Button(
                        onClick = {
                            isSyncing = true
                            syncMessage = null
                            scope.launch {
                                val result = container.syncManager.sync()
                                isSyncing = false
                                syncMessage = result.message
                                syncSucceeded = result.success
                                lastSyncMillis = container.syncPrefs.lastSyncMillis
                            }
                        },
                        modifier = Modifier.padding(top = 14.dp)
                    ) { Text("Sync Now") }
                }

                syncMessage?.let {
                    Text(it, color = if (syncSucceeded) HisabGreen else HisabRed, modifier = Modifier.padding(top = 10.dp))
                }
            }

            HisabCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable { navController.navigate(Dest.ACCOUNTS) }) {
                Text("💳  Accounts")
                Text("Add or edit accounts", style = MaterialTheme.typography.bodySmall)
            }
            HisabCard(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 24.dp).clickable { navController.navigate(Dest.CATEGORIES) }
            ) {
                Text("🏷️  Categories")
                Text("Income & Expense categories", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
