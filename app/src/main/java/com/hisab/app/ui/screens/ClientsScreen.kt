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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.hisab.app.data.remote.ClientDue
import com.hisab.app.ui.LocalAppContainer
import com.hisab.app.ui.components.ChipPicker
import com.hisab.app.ui.components.EmptyState
import com.hisab.app.ui.components.HisabCard
import com.hisab.app.ui.components.ScreenHeader
import com.hisab.app.ui.theme.HisabRed
import com.hisab.app.util.Money
import kotlinx.coroutines.launch

@Composable
fun ClientsScreen(navController: NavController) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var clients by remember { mutableStateOf<List<ClientDue>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            runCatching { clients = container.businessApi.getClientsWithDue() }
                .onFailure { loadError = it.message ?: "লোড করা যায়নি" }
        }
    }
    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Clients (Boosting Due)", onBack = { navController.popBackStack() })
        when {
            loadError != null -> EmptyState("⚠️", "লোড করা যায়নি", loadError ?: "")
            clients == null -> Column(Modifier.fillMaxSize().padding(32.dp)) { CircularProgressIndicator() }
            clients!!.isEmpty() -> EmptyState("👤", "কোনো client নেই", "Web dashboard থেকে client যোগ করুন।")
            else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                items(clients!!, key = { it.id }) { client ->
                    ClientRow(client) { reload() }
                }
            }
        }
    }
}

@Composable
private fun ClientRow(client: ClientDue, onPaid: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val accounts by container.accountRepository.observeAll().collectAsState(initial = emptyList())

    var expanded by remember(client.id) { mutableStateOf(false) }
    var amount by remember(client.id) { mutableStateOf("") }
    var accountId by remember(client.id) { mutableStateOf<Long?>(null) }
    var error by remember(client.id) { mutableStateOf<String?>(null) }
    var submitting by remember(client.id) { mutableStateOf(false) }

    HisabCard(modifier = Modifier.padding(bottom = 12.dp).clickable { expanded = !expanded }) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(client.name, style = MaterialTheme.typography.titleMedium)
                Text("${Money.format(client.contractRateMinor)}/$", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                if (client.dueMinor > 0) "Due ${Money.format(client.dueMinor)}" else "No due",
                color = if (client.dueMinor > 0) HisabRed else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (expanded) {
            OutlinedTextField(
                value = amount, onValueChange = { amount = it },
                label = { Text("Amount (৳)") },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
            ChipPicker("Received Into Account", accounts.map { it.id to "${it.icon} ${it.name}" }, accountId) { accountId = it }
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
            Button(
                enabled = !submitting,
                onClick = {
                    val amountMinor = amount.toDoubleOrNull()?.times(100)?.toLong()
                    val acc = accountId
                    if (amountMinor == null || amountMinor <= 0) { error = "সঠিক amount দিন"; return@Button }
                    if (acc == null) { error = "Account বেছে নিন"; return@Button }
                    scope.launch {
                        submitting = true
                        try {
                            val remoteAccountId = container.accountRepository.getById(acc)?.remoteId
                            if (remoteAccountId == null) { error = "Account sync হয়নি — আগে Settings থেকে Sync করুন"; return@launch }
                            container.businessApi.recordClientPayment(client.id, remoteAccountId, amountMinor, null)
                            expanded = false; amount = ""; error = null
                            container.syncInBackground()
                            onPaid()
                        } catch (e: Exception) {
                            error = "ব্যর্থ: ${e.message ?: e.javaClass.simpleName}"
                        } finally {
                            submitting = false
                        }
                    }
                },
                modifier = Modifier.padding(top = 10.dp)
            ) { Text("Record Payment") }
        }
    }
}
