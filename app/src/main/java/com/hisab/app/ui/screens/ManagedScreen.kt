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
import com.hisab.app.data.remote.ManagedPersonDue
import com.hisab.app.ui.LocalAppContainer
import com.hisab.app.ui.components.ChipPicker
import com.hisab.app.ui.components.EmptyState
import com.hisab.app.ui.components.HisabCard
import com.hisab.app.ui.components.ScreenHeader
import com.hisab.app.ui.theme.HisabGreen
import com.hisab.app.ui.theme.HisabMuted
import com.hisab.app.ui.theme.HisabRed
import com.hisab.app.util.Money
import kotlinx.coroutines.launch

@Composable
fun ManagedScreen(navController: NavController) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var persons by remember { mutableStateOf<List<ManagedPersonDue>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            runCatching { persons = container.businessApi.getManagedPersons() }
                .onFailure { loadError = it.message ?: "লোড করা যায়নি" }
        }
    }
    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Managed Money", onBack = { navController.popBackStack() })
        when {
            loadError != null -> EmptyState("⚠️", "লোড করা যায়নি", loadError ?: "")
            persons == null -> Column(Modifier.fillMaxSize().padding(32.dp)) { CircularProgressIndicator() }
            persons!!.isEmpty() -> EmptyState("👤", "কেউ নেই", "Web dashboard-এর Managed Money পেজ থেকে যোগ করুন।")
            else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                items(persons!!, key = { it.id }) { person ->
                    PersonRow(person) { reload() }
                }
            }
        }
    }
}

@Composable
private fun PersonRow(person: ManagedPersonDue, onSaved: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val accounts by container.accountRepository.observeAll().collectAsState(initial = emptyList())

    var expanded by remember(person.id) { mutableStateOf(false) }
    var direction by remember(person.id) { mutableStateOf(true) }
    var amount by remember(person.id) { mutableStateOf("") }
    var accountId by remember(person.id) { mutableStateOf<Long?>(null) }
    var error by remember(person.id) { mutableStateOf<String?>(null) }
    var submitting by remember(person.id) { mutableStateOf(false) }

    val statusText = when {
        person.remainingMinor > 0 -> "Owe ${Money.format(person.remainingMinor)}"
        person.remainingMinor < 0 -> "Overpaid ${Money.format(-person.remainingMinor)}"
        else -> "Settled"
    }
    val statusColor = when {
        person.remainingMinor > 0 -> HisabRed
        person.remainingMinor < 0 -> HisabMuted
        else -> HisabGreen
    }

    HisabCard(modifier = Modifier.padding(bottom = 12.dp).clickable { expanded = !expanded }) {
        Row(Modifier.fillMaxWidth()) {
            Text(person.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(statusText, color = statusColor, style = MaterialTheme.typography.bodyMedium)
        }
        if (expanded) {
            ChipPicker("Direction", listOf(0L to "Received", 1L to "Paid"), if (direction) 0L else 1L) { direction = it == 0L }
            OutlinedTextField(
                value = amount, onValueChange = { amount = it },
                label = { Text("Amount (৳)") },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
            ChipPicker("Account", accounts.map { it.id to "${it.icon} ${it.name}" }, accountId) { accountId = it }
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
                            if (direction) {
                                container.businessApi.recordManagedReceived(person.id, remoteAccountId, amountMinor, null)
                            } else {
                                container.businessApi.recordManagedPaid(person.id, remoteAccountId, amountMinor, null)
                            }
                            expanded = false; amount = ""; error = null
                            container.syncInBackground()
                            onSaved()
                        } catch (e: Exception) {
                            error = "ব্যর্থ: ${e.message ?: e.javaClass.simpleName}"
                        } finally {
                            submitting = false
                        }
                    }
                },
                modifier = Modifier.padding(top = 10.dp)
            ) { Text("Save") }
        }
    }
}
