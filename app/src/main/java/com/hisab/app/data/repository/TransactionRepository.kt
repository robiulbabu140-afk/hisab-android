package com.hisab.app.data.repository

import androidx.room.withTransaction
import com.hisab.app.data.local.BalanceGap
import com.hisab.app.data.local.HisabDatabase
import com.hisab.app.data.local.RawSmsStatus
import com.hisab.app.data.local.Transaction
import com.hisab.app.data.local.TxnSource
import com.hisab.app.data.local.TxnType
import kotlinx.coroutines.flow.Flow

/**
 * Owns every write to the `transactions` and `accounts` tables together, so a balance
 * update can never happen without its ledger row (or vice versa). This is the only place
 * account balances change outside direct account creation.
 */
class TransactionRepository(private val db: HisabDatabase) {

    fun observeAll(): Flow<List<Transaction>> = db.transactionDao().observeAll()

    fun observeForAccount(accountId: Long): Flow<List<Transaction>> =
        db.transactionDao().observeForAccount(accountId)

    fun observeSumForRange(type: TxnType, startMillis: Long, endMillis: Long): Flow<Long> =
        db.transactionDao().observeSumForRange(type, startMillis, endMillis)

    suspend fun getById(id: Long): Transaction? = db.transactionDao().getById(id)

    suspend fun recordIncome(
        accountId: Long,
        categoryId: Long,
        amountMinor: Long,
        note: String?,
        timestampMillis: Long,
        source: TxnSource = TxnSource.MANUAL,
        rawSmsId: Long? = null,
        remoteId: Long? = null
    ): Long = db.withTransaction {
        val txnId = db.transactionDao().insert(
            Transaction(
                type = TxnType.INCOME,
                amountMinor = amountMinor,
                accountId = accountId,
                categoryId = categoryId,
                note = note,
                timestampMillis = timestampMillis,
                source = source,
                rawSmsId = rawSmsId,
                remoteId = remoteId
            )
        )
        detectGapIfAny(accountId, rawSmsId, amountMinor)
        db.accountDao().adjustBalance(accountId, amountMinor)
        linkRawSmsIfAny(rawSmsId, txnId)
        txnId
    }

    suspend fun recordExpense(
        accountId: Long,
        categoryId: Long,
        amountMinor: Long,
        note: String?,
        timestampMillis: Long,
        source: TxnSource = TxnSource.MANUAL,
        rawSmsId: Long? = null,
        remoteId: Long? = null
    ): Long = db.withTransaction {
        val txnId = db.transactionDao().insert(
            Transaction(
                type = TxnType.EXPENSE,
                amountMinor = amountMinor,
                accountId = accountId,
                categoryId = categoryId,
                note = note,
                timestampMillis = timestampMillis,
                source = source,
                rawSmsId = rawSmsId,
                remoteId = remoteId
            )
        )
        detectGapIfAny(accountId, rawSmsId, -amountMinor)
        db.accountDao().adjustBalance(accountId, -amountMinor)
        linkRawSmsIfAny(rawSmsId, txnId)
        txnId
    }

    suspend fun recordTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        amountMinor: Long,
        note: String?,
        timestampMillis: Long,
        source: TxnSource = TxnSource.MANUAL,
        rawSmsId: Long? = null,
        remoteId: Long? = null
    ): Long = db.withTransaction {
        val txnId = db.transactionDao().insert(
            Transaction(
                type = TxnType.TRANSFER,
                amountMinor = amountMinor,
                accountId = fromAccountId,
                toAccountId = toAccountId,
                note = note,
                timestampMillis = timestampMillis,
                source = source,
                rawSmsId = rawSmsId,
                remoteId = remoteId
            )
        )
        db.accountDao().adjustBalance(fromAccountId, -amountMinor)
        db.accountDao().adjustBalance(toAccountId, amountMinor)
        linkRawSmsIfAny(rawSmsId, txnId)
        txnId
    }

    suspend fun recordNeutral(
        accountId: Long,
        amountMinor: Long,
        isInflow: Boolean,
        note: String?,
        timestampMillis: Long,
        source: TxnSource = TxnSource.MANUAL,
        rawSmsId: Long? = null,
        remoteId: Long? = null
    ): Long = db.withTransaction {
        val txnId = db.transactionDao().insert(
            Transaction(
                type = TxnType.NEUTRAL,
                amountMinor = amountMinor,
                accountId = accountId,
                isInflow = isInflow,
                note = note,
                timestampMillis = timestampMillis,
                source = source,
                rawSmsId = rawSmsId,
                remoteId = remoteId
            )
        )
        val signedDelta = if (isInflow) amountMinor else -amountMinor
        detectGapIfAny(accountId, rawSmsId, signedDelta)
        db.accountDao().adjustBalance(accountId, signedDelta)
        linkRawSmsIfAny(rawSmsId, txnId)
        txnId
    }

    suspend fun markSynced(transaction: Transaction, remoteId: Long) {
        db.transactionDao().update(transaction.copy(remoteId = remoteId))
    }

    suspend fun getUnsynced(): List<Transaction> = db.transactionDao().getUnsynced()
    suspend fun getSyncedRemoteIds(): List<Long> = db.transactionDao().getSyncedRemoteIds()

    /** Marks the raw SMS this transaction was confirmed from as CONFIRMED and links it back. */
    private suspend fun linkRawSmsIfAny(rawSmsId: Long?, txnId: Long) {
        if (rawSmsId == null) return
        val raw = db.rawSmsDao().getById(rawSmsId) ?: return
        db.rawSmsDao().update(raw.copy(status = RawSmsStatus.CONFIRMED, linkedTransactionId = txnId))
    }

    /**
     * Called right before a confirmed-from-SMS transaction is applied. Most bank/MFS SMS state
     * the account's balance *after* the transaction — reversing this transaction's own delta out
     * of that gives what the balance must have been *before* it, per the bank. If that doesn't
     * match what the app already has on record for the account right now, something in between
     * never got recorded (a missed SMS, usually) — log it as a [BalanceGap] instead of silently
     * accepting a number that's about to become wrong anyway.
     */
    private suspend fun detectGapIfAny(accountId: Long, rawSmsId: Long?, signedDeltaMinor: Long) {
        if (rawSmsId == null) return
        val raw = db.rawSmsDao().getById(rawSmsId) ?: return
        val statedBalanceAfter = raw.balanceAfterMinor ?: return
        val currentBalance = db.accountDao().getById(accountId)?.balanceMinor ?: return

        val expectedBalanceBefore = statedBalanceAfter - signedDeltaMinor
        val gap = expectedBalanceBefore - currentBalance
        if (gap == 0L) return

        db.balanceGapDao().insert(
            BalanceGap(
                accountId = accountId,
                gapAmountMinor = gap,
                detectedAtMillis = System.currentTimeMillis(),
                relatedRawSmsId = rawSmsId
            )
        )
    }
}
