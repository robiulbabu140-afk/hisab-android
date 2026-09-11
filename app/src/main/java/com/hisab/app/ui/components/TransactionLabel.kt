package com.hisab.app.ui.components

import com.hisab.app.data.local.Account
import com.hisab.app.data.local.Category
import com.hisab.app.data.local.Transaction
import com.hisab.app.data.local.TxnType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TxnLabel(val icon: String, val title: String, val subtitle: String)

private val timeFormat = SimpleDateFormat("d MMM · h:mm a", Locale.US)

/** Builds the icon/title/subtitle a [TransactionRow] shows for one ledger entry, given the
 * account/category name lookups (avoids every screen re-deriving this display logic). */
fun transactionLabel(
    txn: Transaction,
    accountsById: Map<Long, Account>,
    categoriesById: Map<Long, Category>
): TxnLabel {
    val time = timeFormat.format(Date(txn.timestampMillis))
    return when (txn.type) {
        TxnType.INCOME, TxnType.EXPENSE -> {
            val category = txn.categoryId?.let { categoriesById[it] }
            val account = accountsById[txn.accountId]
            TxnLabel(
                icon = category?.icon ?: "💰",
                title = category?.name ?: "Uncategorized",
                subtitle = "${account?.name ?: "Account"} · $time"
            )
        }
        TxnType.TRANSFER -> {
            val from = accountsById[txn.accountId]?.name ?: "Account"
            val to = txn.toAccountId?.let { accountsById[it]?.name } ?: "Account"
            TxnLabel(icon = "🔄", title = "Transfer", subtitle = "$from → $to · $time")
        }
        TxnType.NEUTRAL -> {
            val account = accountsById[txn.accountId]?.name ?: "Account"
            TxnLabel(icon = "⚪", title = txn.note?.takeIf { it.isNotBlank() } ?: "Neutral", subtitle = "$account · $time")
        }
    }
}
