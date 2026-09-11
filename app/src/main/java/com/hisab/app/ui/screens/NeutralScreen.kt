package com.hisab.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hisab.app.data.local.TxnSource
import com.hisab.app.ui.LocalAppContainer
import com.hisab.app.ui.components.ScreenHeader
import com.hisab.app.util.Money
import kotlinx.coroutines.launch

@Composable
fun NeutralScreen(navController: NavController) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val accounts by container.accountRepository.observeAll().collectAsState(initial = emptyList())

    var amountText by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var isInflow by remember { mutableStateOf(true) }
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Neutral Transaction", onBack = { navController.popBackStack() })
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            Text(
                "এই transaction আপনার Income/Expense রিপোর্টে যাবে না, কিন্তু সম্পূর্ণ history-তে থাকবে।",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount (৳)") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Account", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                accounts.forEach { acc ->
                    FilterChip(
                        selected = accountId == acc.id,
                        onClick = { accountId = acc.id },
                        label = { Text("${acc.icon} ${acc.name}") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Text("Direction", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
            Row(Modifier.fillMaxWidth()) {
                FilterChip(selected = isInflow, onClick = { isInflow = true }, label = { Text("Money In") }, modifier = Modifier.padding(end = 8.dp))
                FilterChip(selected = !isInflow, onClick = { isInflow = false }, label = { Text("Money Out") })
            }
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason (e.g. মালের টাকা / অন্যের টাকা)") },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            Button(
                onClick = {
                    val amount = Money.parseTakaInputToMinor(amountText)
                    val acc = accountId
                    when {
                        amount == null || amount <= 0 -> error = "সঠিক amount দিন"
                        acc == null -> error = "Account বেছে নিন"
                        else -> {
                            error = null
                            scope.launch {
                                container.transactionRepository.recordNeutral(
                                    acc, amount, isInflow, reason.ifBlank { null }, System.currentTimeMillis(), TxnSource.MANUAL
                                )
                                navController.popBackStack()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            ) { Text("Save as Neutral") }
        }
    }
}
