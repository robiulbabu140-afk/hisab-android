package com.hisab.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hisab.app.data.local.Account
import com.hisab.app.data.local.AccountType
import com.hisab.app.ui.LocalAppContainer
import com.hisab.app.ui.components.ChipPicker
import com.hisab.app.ui.components.HisabCard
import com.hisab.app.ui.components.ScreenHeader
import com.hisab.app.ui.navigation.Dest
import com.hisab.app.util.Money
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun AccountsScreen(navController: NavController) {
    val container = LocalAppContainer.current
    val accounts by container.accountRepository.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    // Picks up account edits/creates made from the web dashboard right away instead of
    // waiting for the next periodic background sync.
    LaunchedEffect(Unit) { container.syncInBackground() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Accounts", onBack = { navController.popBackStack() })
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            items(accounts, key = { it.id }) { acc ->
                AccountRow(acc, onOpen = { navController.navigate(Dest.accountDetail(acc.id)) })
            }
            item {
                if (showAdd) {
                    HisabCard(modifier = Modifier.padding(top = 4.dp)) {
                        OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Account name") })
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    scope.launch {
                                        container.accountRepository.create(Account(name = newName, type = AccountType.OTHER, icon = "💳"))
                                        container.syncInBackground()
                                        newName = ""
                                        showAdd = false
                                    }
                                }
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) { Text("Save Account") }
                    }
                } else {
                    Button(onClick = { showAdd = true }, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)) {
                        Text("＋ Add New Account")
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(acc: Account, onOpen: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    var editing by remember(acc.id) { mutableStateOf(false) }
    var name by remember(acc.id) { mutableStateOf(acc.name) }
    var icon by remember(acc.id) { mutableStateOf(acc.icon) }
    var type by remember(acc.id) { mutableStateOf(acc.type) }
    var saving by remember(acc.id) { mutableStateOf(false) }

    HisabCard(modifier = Modifier.padding(bottom = 10.dp)) {
        Row(Modifier.fillMaxWidth().clickable(enabled = !editing) { onOpen() }) {
            Column(Modifier.weight(1f)) {
                Text("${acc.icon} ${acc.name}")
                Text(Money.format(acc.balanceMinor))
            }
            TextButton(onClick = {
                name = acc.name; icon = acc.icon; type = acc.type
                editing = !editing
            }) { Text(if (editing) "Close" else "Edit") }
        }
        if (editing) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
            OutlinedTextField(
                value = icon, onValueChange = { icon = it },
                label = { Text("Icon (emoji)") },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
            ChipPicker(
                "Type",
                listOf(
                    AccountType.MOBILE_WALLET.ordinal.toLong() to "Mobile Wallet",
                    AccountType.BANK.ordinal.toLong() to "Bank",
                    AccountType.CASH.ordinal.toLong() to "Cash",
                    AccountType.OTHER.ordinal.toLong() to "Other"
                ),
                type.ordinal.toLong()
            ) { type = AccountType.entries[it.toInt()] }
            Button(
                enabled = !saving && name.isNotBlank(),
                onClick = {
                    scope.launch {
                        saving = true
                        try {
                            val updated = acc.copy(name = name.trim(), icon = icon.ifBlank { "💳" }, type = type)
                            container.accountRepository.update(updated)
                            val remoteId = updated.remoteId
                            if (remoteId != null) {
                                container.apiClient.postForObject(
                                    "accounts.php",
                                    JSONObject()
                                        .put("action", "update")
                                        .put("id", remoteId)
                                        .put("name", updated.name)
                                        .put("type", updated.type.name)
                                        .put("icon", updated.icon)
                                )
                            }
                            container.syncInBackground()
                            editing = false
                        } catch (_: Exception) {
                            // Local edit already saved; next sync retries the remote push.
                        } finally {
                            saving = false
                        }
                    }
                },
                modifier = Modifier.padding(top = 10.dp)
            ) { Text("Save") }
        }
    }
}
