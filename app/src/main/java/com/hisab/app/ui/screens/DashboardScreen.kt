package com.hisab.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hisab.app.data.local.TxnType
import com.hisab.app.ui.LocalAppContainer
import com.hisab.app.ui.components.EmptyState
import com.hisab.app.ui.components.GradientBalanceCard
import com.hisab.app.ui.components.SectionHeader
import com.hisab.app.ui.components.StatTile
import com.hisab.app.ui.components.TransactionRow
import com.hisab.app.ui.components.transactionLabel
import com.hisab.app.ui.navigation.Dest
import com.hisab.app.ui.theme.HisabGreen
import com.hisab.app.ui.theme.HisabMuted
import com.hisab.app.ui.theme.HisabRed
import com.hisab.app.util.Money
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun DashboardScreen(navController: NavController) {
    val container = LocalAppContainer.current
    val coroutineScope = rememberCoroutineScope()

    // Dashboard খোলা মানেই ওয়েবে কিছু নতুন হয়ে থাকলে সেটা এখনই টেনে আনার একটা সুযোগ — নিঃশব্দে,
    // ব্যর্থ হলেও কোনো error দেখানো হয় না যেহেতু background periodic sync তো আছেই।
    LaunchedEffect(Unit) { container.syncInBackground() }

    val monthStartMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val accounts by container.accountRepository.observeAll().collectAsState(initial =emptyList())
    val totalBalance by container.accountRepository.observeTotalBalance().collectAsState(initial =0L)
    val categories by container.categoryRepository.observeAll().collectAsState(initial =emptyList())
    val monthIncome by container.transactionRepository
        .observeSumForRange(TxnType.INCOME, monthStartMillis, Long.MAX_VALUE)
        .collectAsState(initial =0L)
    val monthExpense by container.transactionRepository
        .observeSumForRange(TxnType.EXPENSE, monthStartMillis, Long.MAX_VALUE)
        .collectAsState(initial =0L)
    val pendingSmsCount by container.smsRepository.observePendingCount().collectAsState(initial =0)
    val recentTxns by container.transactionRepository.observeAll().collectAsState(initial =emptyList())
    val unresolvedGaps by container.balanceGapDao.observeUnresolved().collectAsState(initial = emptyList())

    val accountsById = remember(accounts) { accounts.associateBy { it.id } }
    val categoriesById = remember(categories) { categories.associateBy { it.id } }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        item {
            Text("Good day! 👋", color = HisabMuted, modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
            GradientBalanceCard(
                label = "Total Available Balance",
                amountMinor = totalBalance,
                subItems = listOf("This Month Income" to monthIncome, "This Month Expense" to monthExpense)
            )
        }
        items(unresolvedGaps) { gap ->
            val accountName = accountsById[gap.accountId]?.name ?: "Account"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(Color(0xFFFFF4E5), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                val message = if (gap.gapAmountMinor > 0) {
                    "⚠️ $accountName-এ ${Money.format(gap.gapAmountMinor)} টাকার গ্যাপ পাওয়া গেছে — এই টাকা কোথা থেকে এসেছে, হিসাবে নেই।"
                } else {
                    "⚠️ $accountName-এ ${Money.format(-gap.gapAmountMinor)} টাকার গ্যাপ পাওয়া গেছে — এই টাকা কোথায় খরচ হয়েছে, হিসাবে নেই।"
                }
                Text(message, style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = {
                        coroutineScope.launch { container.balanceGapDao.update(gap.copy(resolved = true)) }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) { Text("ঠিক আছে, বুঝেছি") }
            }
        }
        item {
            SectionHeader("This Month")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Income", monthIncome, HisabGreen, icon = "↑", modifier = Modifier.weight(1f))
                StatTile("Expense", monthExpense, HisabRed, icon = "↓", modifier = Modifier.weight(1f))
            }
        }
        item {
            SectionHeader(
                "SMS Review",
                action = if (pendingSmsCount > 0) "$pendingSmsCount pending →" else null,
                onAction = { navController.navigate(Dest.SMS_REVIEW) }
            )
            if (pendingSmsCount == 0) {
                Text("কোনো নতুন SMS নেই।", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionHeader("Recent Transactions", action = "See all", onAction = { navController.navigate(Dest.TRANSACTIONS) })
        }
        if (recentTxns.isEmpty()) {
            item { EmptyState("🧾", "No transactions yet", "Add your first transaction or wait for SMS detection.") }
        } else {
            items(recentTxns.take(6)) { txn ->
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
        item { Column(Modifier.padding(bottom = 24.dp)) {} }
    }
}
