package com.hisab.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hisab.app.ui.LocalAppContainer
import com.hisab.app.ui.components.EmptyState
import com.hisab.app.ui.components.HisabCard
import com.hisab.app.ui.components.ScreenHeader
import com.hisab.app.ui.components.TransactionRow
import com.hisab.app.ui.components.transactionLabel
import com.hisab.app.ui.navigation.Dest
import com.hisab.app.util.Money

@Composable
fun AccountDetailScreen(navController: NavController, id: Long) {
    val container = LocalAppContainer.current
    val accounts by container.accountRepository.observeAll().collectAsState(initial = emptyList())
    val categories by container.categoryRepository.observeAll().collectAsState(initial = emptyList())
    val txns by container.transactionRepository.observeForAccount(id).collectAsState(initial = emptyList())
    val accountsById = remember(accounts) { accounts.associateBy { it.id } }
    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    val account = accountsById[id]

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(account?.name ?: "Account", onBack = { navController.popBackStack() })
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            HisabCard {
                Text("Current Balance", style = MaterialTheme.typography.bodySmall)
                Text(Money.format(account?.balanceMinor ?: 0L), style = MaterialTheme.typography.titleLarge)
            }
            if (txns.isEmpty()) {
                EmptyState("🧾", "No transactions", "This account has no history yet.")
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                    items(txns, key = { it.id }) { txn ->
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
}
