package com.hisab.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hisab.app.data.local.TxnType
import com.hisab.app.ui.LocalAppContainer
import com.hisab.app.ui.components.EmptyState
import com.hisab.app.ui.components.ScreenHeader
import com.hisab.app.ui.components.TransactionRow
import com.hisab.app.ui.components.transactionLabel
import com.hisab.app.ui.navigation.Dest

private enum class TxnFilter(val label: String, val type: TxnType?) {
    ALL("All", null),
    INCOME("Income", TxnType.INCOME),
    EXPENSE("Expense", TxnType.EXPENSE),
    TRANSFER("Transfer", TxnType.TRANSFER),
    NEUTRAL("Neutral", TxnType.NEUTRAL)
}

@Composable
fun TransactionsScreen(navController: NavController) {
    val container = LocalAppContainer.current
    val accounts by container.accountRepository.observeAll().collectAsState(initial = emptyList())
    val categories by container.categoryRepository.observeAll().collectAsState(initial = emptyList())
    val allTxns by container.transactionRepository.observeAll().collectAsState(initial = emptyList())
    var filter by remember { mutableStateOf(TxnFilter.ALL) }

    val accountsById = remember(accounts) { accounts.associateBy { it.id } }
    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    val visible = remember(allTxns, filter) {
        if (filter.type == null) allTxns else allTxns.filter { it.type == filter.type }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Transactions")
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        ) {
            TxnFilter.entries.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f.label) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        if (visible.isEmpty()) {
            EmptyState("🧾", "No transactions", "Nothing in this filter yet.")
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                items(visible, key = { it.id }) { txn ->
                    val label = transactionLabel(txn, accountsById, categoriesById)
                    TransactionRow(
                        icon = label.icon,
                        title = label.title,
                        subtitle = label.subtitle,
                        amountMinor = txn.amountMinor,
                        type = txn.type,
                        onClick = { navController.navigate(Dest.transactionDetail(txn.id)) }
                    )
                }
            }
        }
    }
}
