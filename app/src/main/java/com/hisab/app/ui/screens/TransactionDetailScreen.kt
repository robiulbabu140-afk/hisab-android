package com.hisab.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hisab.app.data.local.Account
import com.hisab.app.data.local.Category
import com.hisab.app.data.local.Transaction
import com.hisab.app.data.local.TxnSource
import com.hisab.app.data.local.TxnType
import com.hisab.app.ui.LocalAppContainer
import com.hisab.app.ui.components.HisabCard
import com.hisab.app.ui.components.ScreenHeader
import com.hisab.app.ui.components.transactionLabel
import com.hisab.app.ui.theme.HisabGreen
import com.hisab.app.ui.theme.HisabMuted
import com.hisab.app.ui.theme.HisabRed
import com.hisab.app.util.Money

@Composable
fun TransactionDetailScreen(navController: NavController, id: Long) {
    val container = LocalAppContainer.current
    var txn by remember { mutableStateOf<Transaction?>(null) }
    var account by remember { mutableStateOf<Account?>(null) }
    var toAccount by remember { mutableStateOf<Account?>(null) }
    var category by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(id) {
        val loaded = container.transactionRepository.getById(id)
        txn = loaded
        if (loaded != null) {
            account = container.accountRepository.getById(loaded.accountId)
            toAccount = loaded.toAccountId?.let { container.accountRepository.getById(it) }
            category = loaded.categoryId?.let { container.categoryRepository.getById(it) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Transaction", onBack = { navController.popBackStack() })
        val current = txn
        if (current == null) return@Column

        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            val label = transactionLabel(
                current,
                listOfNotNull(account, toAccount).associateBy { it.id },
                category?.let { mapOf(it.id to it) } ?: emptyMap()
            )
            val color = when (current.type) {
                TxnType.INCOME -> HisabGreen
                TxnType.EXPENSE -> HisabRed
                else -> HisabMuted
            }
            HisabCard {
                Text(label.icon, style = MaterialTheme.typography.titleLarge)
                Text(label.title, color = HisabMuted)
                Text(
                    Money.format(current.amountMinor),
                    style = MaterialTheme.typography.titleLarge,
                    color = color,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            HisabCard(modifier = Modifier.padding(top = 12.dp)) {
                DetailRow("Type", current.type.name)
                DetailRow("Account", account?.name ?: "—")
                if (current.type == TxnType.TRANSFER) DetailRow("To Account", toAccount?.name ?: "—")
                if (category != null) DetailRow("Category", category!!.name)
                DetailRow("Source", if (current.source == TxnSource.SMS) "SMS" else "Manual")
                if (!current.note.isNullOrBlank()) DetailRow("Note", current.note!!)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(label, color = HisabMuted)
        Text(value)
    }
}
