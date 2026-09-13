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
import com.hisab.app.data.remote.SupplierDue
import com.hisab.app.ui.LocalAppContainer
import com.hisab.app.ui.components.ChipPicker
import com.hisab.app.ui.components.EmptyState
import com.hisab.app.ui.components.HisabCard
import com.hisab.app.ui.components.ScreenHeader
import com.hisab.app.ui.theme.HisabMuted
import com.hisab.app.ui.theme.HisabRed
import com.hisab.app.util.Money
import kotlinx.coroutines.launch

@Composable
fun SuppliersScreen(navController: NavController) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var suppliers by remember { mutableStateOf<List<SupplierDue>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            runCatching { suppliers = container.businessApi.getSuppliers() }
                .onFailure { loadError = it.message ?: "লোড করা যায়নি" }
        }
    }
    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Dollar Suppliers", onBack = { navController.popBackStack() })
        when {
            loadError != null -> EmptyState("⚠️", "লোড করা যায়নি", loadError ?: "")
            suppliers == null -> Column(Modifier.fillMaxSize().padding(32.dp)) { CircularProgressIndicator() }
            suppliers!!.isEmpty() -> EmptyState("🏦", "কোনো supplier নেই", "Web dashboard-এর Dollar পেজ থেকে supplier যোগ করুন।")
            else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                items(suppliers!!, key = { it.id }) { supplier ->
                    SupplierRow(supplier) { reload() }
                }
            }
        }
    }
}

@Composable
private fun SupplierRow(supplier: SupplierDue, onSaved: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val accounts by container.accountRepository.observeAll().collectAsState(initial = emptyList())

    var expanded by remember(supplier.id) { mutableStateOf(false) }
    var isPurchase by remember(supplier.id) { mutableStateOf(true) }
    var dollarAmount by remember(supplier.id) { mutableStateOf("") }
    var buyRate by remember(supplier.id) { mutableStateOf("") }
    var payAmount by remember(supplier.id) { mutableStateOf("") }
    var accountId by remember(supplier.id) { mutableStateOf<Long?>(null) }
    var error by remember(supplier.id) { mutableStateOf<String?>(null) }
    var submitting by remember(supplier.id) { mutableStateOf(false) }

    HisabCard(modifier = Modifier.padding(bottom = 12.dp).clickable { expanded = !expanded }) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(supplier.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${Money.formatDollars(supplier.dollarPurchasedCents)} কেনা হয়েছে · avg ${Money.formatRate(supplier.avgRateMinor)}",
                    style = MaterialTheme.typography.bodySmall, color = HisabMuted
                )
            }
            Text(
                if (supplier.dueMinor > 0) "আপনি দেবেন ${Money.format(supplier.dueMinor)}" else "Settled",
                color = if (supplier.dueMinor > 0) HisabRed else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (expanded) {
            ChipPicker("কাজ", listOf(0L to "Dollar কেনা", 1L to "Payment দেওয়া"), if (isPurchase) 0L else 1L) { isPurchase = it == 0L }

            if (isPurchase) {
                OutlinedTextField(
                    value = dollarAmount, onValueChange = { dollarAmount = it },
                    label = { Text("Dollar Amount ($)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
                OutlinedTextField(
                    value = buyRate, onValueChange = { buyRate = it },
                    label = { Text("Buy Rate (৳ per $)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
            } else {
                OutlinedTextField(
                    value = payAmount, onValueChange = { payAmount = it },
                    label = { Text("Amount (৳)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
                ChipPicker("পরিশোধ করছেন যে Account থেকে", accounts.map { it.id to "${it.icon} ${it.name}" }, accountId) { accountId = it }
            }

            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
            Button(
                enabled = !submitting,
                onClick = {
                    scope.launch {
                        submitting = true
                        error = null
                        try {
                            if (isPurchase) {
                                val dollarCents = dollarAmount.toDoubleOrNull()?.times(100)?.toLong()
                                val rateMinor = buyRate.toDoubleOrNull()?.times(100)?.toLong()
                                if (dollarCents == null || dollarCents <= 0) { error = "সঠিক dollar amount দিন"; return@launch }
                                if (rateMinor == null || rateMinor <= 0) { error = "সঠিক rate দিন"; return@launch }
                                container.businessApi.recordSupplierPurchase(supplier.id, dollarCents, rateMinor, null)
                                dollarAmount = ""; buyRate = ""
                            } else {
                                val amountMinor = payAmount.toDoubleOrNull()?.times(100)?.toLong()
                                val acc = accountId
                                if (amountMinor == null || amountMinor <= 0) { error = "সঠিক amount দিন"; return@launch }
                                if (acc == null) { error = "Account বেছে নিন"; return@launch }
                                val remoteAccountId = container.accountRepository.getById(acc)?.remoteId
                                if (remoteAccountId == null) { error = "Account sync হয়নি — আগে Settings থেকে Sync করুন"; return@launch }
                                container.businessApi.recordSupplierPayment(supplier.id, remoteAccountId, amountMinor, null)
                                payAmount = ""
                            }
                            expanded = false
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
            ) { Text(if (isPurchase) "Record Purchase" else "Record Payment") }
        }
    }
}
