package com.hisab.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A confirmed ledger entry. [amountMinor] is always a positive magnitude for display;
 * direction is implied by [type] (INCOME +, EXPENSE -, TRANSFER from-account - / to-account +).
 * For [TxnType.TRANSFER], [accountId] is the "from" account and [toAccountId] is the "to"
 * account; [categoryId] is null. For INCOME/EXPENSE, [accountId] is the affected account and
 * [categoryId] is required. For NEUTRAL, [accountId] is the one account touched, [toAccountId]
 * and [categoryId] are null, and [isInflow] decides whether that account's balance goes up or
 * down — either way it is excluded from Income/Expense totals.
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxnType,
    val amountMinor: Long,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val isInflow: Boolean = true,
    val note: String? = null,
    val timestampMillis: Long,
    val source: TxnSource = TxnSource.MANUAL,
    val rawSmsId: Long? = null,
    val remoteId: Long? = null
)
