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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.hisab.app.data.local.Account
import com.hisab.app.data.local.Category
import com.hisab.app.data.local.CategoryKind
import com.hisab.app.data.local.RawSms
import com.hisab.app.data.local.RawSmsStatus
import com.hisab.app.data.local.TxnSource
import com.hisab.app.data.remote.BuyerDue
import com.hisab.app.data.remote.ClientDue
import com.hisab.app.data.remote.ManagedPersonDue
import com.hisab.app.ui.LocalAppContainer
import com.hisab.app.ui.components.ChipPicker
import com.hisab.app.ui.components.EmptyState
import com.hisab.app.ui.components.HisabCard
import com.hisab.app.ui.components.ScreenHeader
import com.hisab.app.ui.navigation.Dest
import com.hisab.app.util.Money
import kotlinx.coroutines.launch
import org.json.JSONObject

private enum class ReviewChoice { EXPENSE, INCOME, TRANSFER, NEUTRAL, CLIENT_PAYMENT, DOLLAR_SALE_PAYMENT, MANAGED_RECEIVED, MANAGED_PAID, IGNORE }

/** "Cash In"/"Deposit"/"Credit" -> money came in; "Cash Out"/"Withdrawal"/"Debit" -> money went
 * out — same rule as the web dashboard's SMS Review, so only the choices that make sense for
 * the SMS's actual direction are shown (a debit SMS can't be a Client Payment, say). */
private fun smsDirectionIsCredit(detectedType: String?): Boolean? {
    val t = detectedType?.lowercase() ?: return null
    if (t.isBlank()) return null
    if ("out" in t || "debit" in t || "withdraw" in t) return false
    if ("in" in t || "credit" in t || "deposit" in t) return true
    return null
}

private val CREDIT_CHOICES = setOf(ReviewChoice.INCOME, ReviewChoice.TRANSFER, ReviewChoice.NEUTRAL, ReviewChoice.CLIENT_PAYMENT, ReviewChoice.DOLLAR_SALE_PAYMENT, ReviewChoice.MANAGED_RECEIVED, ReviewChoice.IGNORE)
private val DEBIT_CHOICES = setOf(ReviewChoice.EXPENSE, ReviewChoice.TRANSFER, ReviewChoice.NEUTRAL, ReviewChoice.MANAGED_PAID, ReviewChoice.IGNORE)

@Composable
fun SmsReviewScreen(navController: NavController) {
    val container = LocalAppContainer.current
    val pending by container.smsRepository.observePending().collectAsState(initial = emptyList())
    val accounts by container.accountRepository.observeAll().collectAsState(initial = emptyList())
    val categories by container.categoryRepository.observeAll().collectAsState(initial = emptyList())

    var clients by remember { mutableStateOf<List<ClientDue>>(emptyList()) }
    var buyers by remember { mutableStateOf<List<BuyerDue>>(emptyList()) }
    var persons by remember { mutableStateOf<List<ManagedPersonDue>>(emptyList()) }

    LaunchedEffect(Unit) {
        runCatching { clients = container.businessApi.getClientsWithDue() }
        runCatching { buyers = container.businessApi.getBuyersWithDue() }
        runCatching { persons = container.businessApi.getManagedPersons() }
    }

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
                        clients = clients,
                        buyers = buyers,
                        persons = persons,
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
    clients: List<ClientDue>,
    buyers: List<BuyerDue>,
    persons: List<ManagedPersonDue>,
    onOpenDetail: () -> Unit
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    val isCredit = remember(raw.id) { smsDirectionIsCredit(raw.detectedType) }
    val visibleChoices = remember(isCredit) {
        when (isCredit) {
            true -> ReviewChoice.values().filter { it in CREDIT_CHOICES }
            false -> ReviewChoice.values().filter { it in DEBIT_CHOICES }
            null -> ReviewChoice.values().toList()
        }
    }

    // sender ("EBL") -> every account whose name contains it ("Ebl bank parsonal", "ebl
    // business") — narrows the picker to just those candidates instead of the full account
    // list, same as the web dashboard's SMS Review. Falls back to the full list when nothing
    // matches, so the picker is never left empty.
    val matchedAccounts = remember(raw.id, accounts) {
        val s = raw.sender.trim().lowercase()
        if (s.isBlank()) emptyList() else accounts.filter { acc ->
            val n = acc.name.trim().lowercase()
            n.contains(s) || s.contains(n)
        }
    }
    val accountOptions = if (matchedAccounts.isNotEmpty()) matchedAccounts else accounts

    var choice by remember(raw.id) { mutableStateOf(if (isCredit == false) ReviewChoice.EXPENSE else ReviewChoice.INCOME) }
    var accountId by remember(raw.id) {
        mutableStateOf(if (matchedAccounts.size == 1) matchedAccounts[0].id else null)
    }
    var toAccountId by remember(raw.id) { mutableStateOf<Long?>(null) }
    var categoryId by remember(raw.id) { mutableStateOf<Long?>(null) }
    var isInflow by remember(raw.id) { mutableStateOf(isCredit != false) }
    var clientId by remember(raw.id) { mutableStateOf<Long?>(null) }
    var buyerName by remember(raw.id) { mutableStateOf<String?>(null) }
    var personId by remember(raw.id) { mutableStateOf<Long?>(null) }
    var error by remember(raw.id) { mutableStateOf<String?>(null) }
    var submitting by remember(raw.id) { mutableStateOf(false) }

    HisabCard(modifier = Modifier.padding(bottom = 14.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text("📱 ${raw.provider} · ${raw.detectedType ?: ""}", modifier = Modifier.weight(1f))
        }
        Text(Money.format(raw.amountMinor), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            visibleChoices.forEach { c ->
                FilterChip(
                    selected = choice == c,
                    onClick = { choice = c; categoryId = null },
                    label = { Text(c.name.replace('_', ' ')) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        when (choice) {
            ReviewChoice.EXPENSE, ReviewChoice.INCOME -> {
                val kind = if (choice == ReviewChoice.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
                ChipPicker("Account", accountOptions.map { it.id to "${it.icon} ${it.name}" }, accountId) { accountId = it }
                ChipPicker(
                    "Category",
                    categories.filter { it.kind == kind }.map { it.id to "${it.icon} ${it.name}" },
                    categoryId
                ) { categoryId = it }
            }
            ReviewChoice.TRANSFER -> {
                ChipPicker("From Account", accountOptions.map { it.id to "${it.icon} ${it.name}" }, accountId) { accountId = it }
                ChipPicker("To Account", accounts.map { it.id to "${it.icon} ${it.name}" }, toAccountId) { toAccountId = it }
            }
            ReviewChoice.NEUTRAL -> {
                ChipPicker("Account", accountOptions.map { it.id to "${it.icon} ${it.name}" }, accountId) { accountId = it }
                ChipPicker(
                    "Direction",
                    listOf(0L to "Money In", 1L to "Money Out"),
                    if (isInflow) 0L else 1L
                ) { isInflow = it == 0L }
            }
            ReviewChoice.CLIENT_PAYMENT -> {
                ChipPicker("Account", accountOptions.map { it.id to "${it.icon} ${it.name}" }, accountId) { accountId = it }
                ChipPicker("Client", clients.map { it.id to "${it.name} (Due ${Money.format(it.dueMinor)})" }, clientId) { clientId = it }
            }
            ReviewChoice.DOLLAR_SALE_PAYMENT -> {
                ChipPicker("Account", accountOptions.map { it.id to "${it.icon} ${it.name}" }, accountId) { accountId = it }
                val buyersWithDue = buyers.filter { it.dueMinor > 0 }
                ChipPicker(
                    "Buyer",
                    buyersWithDue.mapIndexed { i, b -> i.toLong() to "${b.buyerName} (Due ${Money.format(b.dueMinor)})" },
                    buyersWithDue.indexOfFirst { it.buyerName == buyerName }.takeIf { it >= 0 }?.toLong()
                ) { idx -> buyerName = buyersWithDue.getOrNull(idx.toInt())?.buyerName }
            }
            ReviewChoice.MANAGED_RECEIVED, ReviewChoice.MANAGED_PAID -> {
                ChipPicker("Account", accountOptions.map { it.id to "${it.icon} ${it.name}" }, accountId) { accountId = it }
                ChipPicker("Person", persons.map { it.id to it.name }, personId) { personId = it }
            }
            ReviewChoice.IGNORE -> {}
        }

        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
        }

        Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Button(enabled = !submitting, onClick = {
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
                        ReviewChoice.CLIENT_PAYMENT, ReviewChoice.DOLLAR_SALE_PAYMENT, ReviewChoice.MANAGED_RECEIVED, ReviewChoice.MANAGED_PAID -> {
                            val acc = accountId
                            if (acc == null) { error = "Account বেছে নিন"; return@launch }
                            if (choice == ReviewChoice.CLIENT_PAYMENT && clientId == null) { error = "Client বেছে নিন"; return@launch }
                            if (choice == ReviewChoice.DOLLAR_SALE_PAYMENT && buyerName == null) { error = "Buyer বেছে নিন"; return@launch }
                            if ((choice == ReviewChoice.MANAGED_RECEIVED || choice == ReviewChoice.MANAGED_PAID) && personId == null) { error = "Person বেছে নিন"; return@launch }

                            submitting = true
                            try {
                                if (!container.syncPrefs.isConfigured()) { error = "Backend URL/API Key সেট করা হয়নি — Settings-এ দিন"; return@launch }
                                container.syncManager.sync()
                                val freshRaw = container.smsRepository.getById(raw.id)
                                val rawRemoteId = freshRaw?.remoteId
                                val accountRemoteId = container.accountRepository.getById(acc)?.remoteId
                                if (rawRemoteId == null || accountRemoteId == null) { error = "Sync ব্যর্থ হয়েছে, আবার চেষ্টা করুন"; return@launch }

                                val body = JSONObject()
                                    .put("action", "confirm")
                                    .put("raw_sms_id", rawRemoteId)
                                    .put("account_id", accountRemoteId)
                                when (choice) {
                                    ReviewChoice.CLIENT_PAYMENT -> body.put("choice", "client_payment").put("client_id", clientId)
                                    ReviewChoice.DOLLAR_SALE_PAYMENT -> body.put("choice", "dollar_sale_payment").put("buyer_name", buyerName)
                                    ReviewChoice.MANAGED_RECEIVED -> body.put("choice", "managed_received").put("person_id", personId)
                                    ReviewChoice.MANAGED_PAID -> body.put("choice", "managed_paid").put("person_id", personId)
                                    else -> {}
                                }
                                container.apiClient.postForObject("sms.php", body)
                                container.smsRepository.updateStatus(freshRaw, RawSmsStatus.CONFIRMED)
                            } catch (e: Exception) {
                                error = "ব্যর্থ: ${e.message ?: e.javaClass.simpleName}"
                                return@launch
                            } finally {
                                submitting = false
                            }
                        }
                    }
                    error = null
                    // Confirm করার সাথে সাথেই push — 15 মিনিটের পরবর্তী background sync-এর
                    // জন্য অপেক্ষা করতে হবে না, ওয়েব ড্যাশবোর্ডে প্রায় সাথে সাথেই দেখা যাবে।
                    container.syncInBackground()
                }
            }) {
                if (submitting) CircularProgressIndicator(modifier = Modifier.padding(end = 6.dp)) else Text("Confirm & Next")
            }
            Button(onClick = onOpenDetail, modifier = Modifier.padding(start = 8.dp)) { Text("Details") }
        }
    }
}
