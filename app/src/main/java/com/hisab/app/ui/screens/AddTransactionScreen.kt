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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hisab.app.data.local.CategoryKind
import com.hisab.app.data.local.TxnSource
import com.hisab.app.data.local.TxnType
import com.hisab.app.ui.LocalAppContainer
import com.hisab.app.ui.components.ScreenHeader
import com.hisab.app.ui.navigation.Dest
import com.hisab.app.util.Money
import kotlinx.coroutines.launch

@Composable
fun AddTransactionScreen(navController: NavController) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    var type by remember { mutableStateOf(TxnType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val accounts by container.accountRepository.observeAll().collectAsState(initial = emptyList())
    val categoryKind = if (type == TxnType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
    val categories by container.categoryRepository.observeByKind(categoryKind).collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Add Transaction", onBack = { navController.popBackStack() })
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                listOf(TxnType.EXPENSE, TxnType.INCOME).forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t; selectedCategoryId = null },
                        label = { Text(t.name) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = { navController.navigate(Dest.TRANSFER) { popUpTo(Dest.DASHBOARD) } },
                    label = { Text("TRANSFER") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                FilterChip(
                    selected = false,
                    onClick = { navController.navigate(Dest.NEUTRAL) { popUpTo(Dest.DASHBOARD) } },
                    label = { Text("NEUTRAL") }
                )
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount (৳)") },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            )

            Text("Account", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                accounts.forEach { acc ->
                    FilterChip(
                        selected = selectedAccountId == acc.id,
                        onClick = { selectedAccountId = acc.id },
                        label = { Text("${acc.icon} ${acc.name}") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Text("Category", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = cat.id },
                        label = { Text("${cat.icon} ${cat.name}") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }

            Button(
                onClick = {
                    val amountMinor = Money.parseTakaInputToMinor(amountText)
                    val accountId = selectedAccountId
                    val categoryId = selectedCategoryId
                    when {
                        amountMinor == null || amountMinor <= 0 -> error = "সঠিক amount দিন"
                        accountId == null -> error = "Account বেছে নিন"
                        categoryId == null -> error = "Category বেছে নিন"
                        else -> {
                            error = null
                            scope.launch {
                                val now = System.currentTimeMillis()
                                if (type == TxnType.INCOME) {
                                    container.transactionRepository.recordIncome(
                                        accountId, categoryId, amountMinor, note.ifBlank { null }, now, TxnSource.MANUAL
                                    )
                                } else {
                                    container.transactionRepository.recordExpense(
                                        accountId, categoryId, amountMinor, note.ifBlank { null }, now, TxnSource.MANUAL
                                    )
                                }
                                navController.popBackStack()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp)
            ) { Text("Save Transaction") }
        }
    }
}
