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
fun TransferScreen(navController: NavController) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val accounts by container.accountRepository.observeAll().collectAsState(initial = emptyList())

    var amountText by remember { mutableStateOf("") }
    var fromId by remember { mutableStateOf<Long?>(null) }
    var toId by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Transfer Money", onBack = { navController.popBackStack() })
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            Text(
                "Transfer আপনার Income বা Expense নয়। শুধু এক Account থেকে অন্য Account-এ টাকা সরাবে।",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount (৳)") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("From", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                accounts.forEach { acc ->
                    FilterChip(
                        selected = fromId == acc.id,
                        onClick = { fromId = acc.id },
                        label = { Text("${acc.icon} ${acc.name}") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Text("To", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                accounts.forEach { acc ->
                    FilterChip(
                        selected = toId == acc.id,
                        onClick = { toId = acc.id },
                        label = { Text("${acc.icon} ${acc.name}") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            Button(
                onClick = {
                    val amount = Money.parseTakaInputToMinor(amountText)
                    val from = fromId; val to = toId
                    when {
                        amount == null || amount <= 0 -> error = "সঠিক amount দিন"
                        from == null || to == null -> error = "From ও To account বেছে নিন"
                        from == to -> error = "From ও To একই account হতে পারবে না"
                        else -> {
                            error = null
                            scope.launch {
                                container.transactionRepository.recordTransfer(
                                    from, to, amount, null, System.currentTimeMillis(), TxnSource.MANUAL
                                )
                                navController.popBackStack()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            ) { Text("Save Transfer") }
        }
    }
}
