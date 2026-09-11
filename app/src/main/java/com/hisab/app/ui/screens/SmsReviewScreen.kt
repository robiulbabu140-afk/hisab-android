package com.hisab.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.hisab.app.data.local.Account
import com.hisab.app.data.local.Category
import com.hisab.app.data.local.CategoryKind
import com.hisab.app.data.local.RawSms
import com.hisab.app.data.local.TxnSource
import com.hisab.app.ui.LocalAppContainer
import com.hisab.app.ui.components.EmptyState
import com.hisab.app.ui.components.HisabCard
import com.hisab.app.ui.components.ScreenHeader
import com.hisab.app.ui.navigation.Dest
import com.hisab.app.util.Money
import kotlinx.coroutines.launch

private enum class ReviewChoice { EXPENSE, INCOME, TRANSFER, NEUTRAL, IGNORE }

@Composable
fun SmsReviewScreen(navController: NavController) {
    val container = LocalAppContainer.current
    val pending by container.smsRepository.observePending().collectAsState(initial = emptyList())
    val accounts by container.accountRepository.observeAll().collectAsState(initial = emptyList())
    val categories by container.categoryRepository.observeAll().collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("SMS Review (${pending.size})", onBack = { navController.popBackStack() })
        if (pending.isEmpty()) {
            EmptyState("✅", "All caught up", "কোনো নতুন SMS review করার নেই।")
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                items(pending, key = { it.id }) { raw ->
                    SmsReviewRow(
                        raw = raw,
                        accounts = accounts,
                        categories = categories,
                        onOpenDetail = { navController.navigate(Dest.smsDetail(raw.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SmsReviewRow(
    raw: RawSms,
    accounts: List<Account>,
    categories: List<Category>,
    onOpenDetail: () -> Unit
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    var choice by remember(raw.id) { mutableStateOf(ReviewChoice.EXPENSE) }
    var accountId by remember(raw.id) { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }
    var toAccountId by remember(raw.id) { mutableStateOf<Long?>(null) }
    var categoryId by remember(raw.id) { mutableStateOf<Long?>(null) }
    var isInflow by remember(raw.id) { mutableStateOf(false) }
    var error by remember(raw.id) { mutableStateOf<String?>(null) }

    HisabCard(modifier = Modifier.padding(bottom = 14.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text("📱 ${raw.provider} · ${raw.detectedType ?: ""}", modifier = Modifier.weight(1f))
        }
        Text(Money.format(raw.amountMinor), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            listOf(
                ReviewChoice.EXPENSE, ReviewChoice.INCOME, ReviewChoice.TRANSFER,
                ReviewChoice.NEUTRAL, ReviewChoice.IGNORE
            ).forEach { c ->
                FilterChip(
                    selected = choice == c,
                    onClick = { choice = c; categoryId = null },
                    label = { Text(c.name) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        when (choice) {
            ReviewChoice.EXPENSE, ReviewChoice.INCOME -> {
                val kind = if (choice == ReviewChoice.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
                ChipPicker("Account", accounts.map { it.id to "${it.icon} ${it.name}" }, accountId) { accountId = it }
                ChipPicker(
                    "Category",
                    categories.filter { it.kind == kind }.map { it.id to "${it.icon} ${it.name}" },
                    categoryId
                ) { categoryId = it }
            }
            ReviewChoice.TRANSFER -> {
                ChipPicker("From Account", accounts.map { it.id to "${it.icon} ${it.name}" }, accountId) { accountId = it }
                ChipPicker("To Account", accounts.map { it.id to "${it.icon} ${it.name}" }, toAccountId) { toAccountId = it }
            }
            ReviewChoice.NEUTRAL -> {
                ChipPicker("Account", accounts.map { it.id to "${it.icon} ${it.name}" }, accountId) { accountId = it }
                ChipPicker(
                    "Direction",
                    listOf(0L to "Money In", 1L to "Money Out"),
                    if (isInflow) 0L else 1L
                ) { isInflow = it == 0L }
            }
            ReviewChoice.IGNORE -> {}
        }

        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
        }

        Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Button(onClick = {
                scope.launch {
                    val now = raw.timestampMillis
                    when (choice) {
                        ReviewChoice.IGNORE -> container.smsRepository.ignore(raw.id)
                        ReviewChoice.EXPENSE -> {
                            val acc = accountId; val cat = categoryId
                            if (acc == null || cat == null) { error = "Account ও Category বেছে নিন"; return@launch }
                            container.transactionRepository.recordExpense(acc, cat, raw.amountMinor, raw.detectedType, now, TxnSource.SMS, raw.id)
                        }
                        ReviewChoice.INCOME -> {
                            val acc = accountId; val cat = categoryId
                            if (acc == null || cat == null) { error = "Account ও Category বেছে নিন"; return@launch }
                            container.transactionRepository.recordIncome(acc, cat, raw.amountMinor, raw.detectedType, now, TxnSource.SMS, raw.id)
                        }
                        ReviewChoice.TRANSFER -> {
                            val from = accountId; val to = toAccountId
                            if (from == null || to == null || from == to) { error = "From/To Account ঠিকভাবে বেছে নিন"; return@launch }
                            container.transactionRepository.recordTransfer(from, to, raw.amountMinor, raw.detectedType, now, TxnSource.SMS, raw.id)
                        }
                        ReviewChoice.NEUTRAL -> {
                            val acc = accountId
                            if (acc == null) { error = "Account বেছে নিন"; return@launch }
                            container.transactionRepository.recordNeutral(acc, raw.amountMinor, isInflow, raw.detectedType, now, TxnSource.SMS, raw.id)
                        }
                    }
                    error = null
                }
            }) { Text("Confirm & Next") }
            Button(onClick = onOpenDetail, modifier = Modifier.padding(start = 8.dp)) { Text("Details") }
        }
    }
}

@Composable
private fun ChipPicker(label: String, options: List<Pair<Long, String>>, selectedId: Long?, onSelect: (Long) -> Unit) {
    Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        options.forEach { (id, text) ->
            FilterChip(
                selected = selectedId == id,
                onClick = { onSelect(id) },
                label = { Text(text) },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}
