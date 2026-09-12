package com.hisab.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Detected whenever a confirmed SMS's stated post-transaction balance doesn't match what the
 * app had on record for that account right before applying it — meaning at least one real
 * transaction happened in between that never sent (or the app never saw) an SMS.
 * [gapAmountMinor] is signed: positive means unexplained money showed up, negative means
 * unexplained money left the account.
 */
@Entity(tableName = "balance_gaps")
data class BalanceGap(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val gapAmountMinor: Long,
    val detectedAtMillis: Long,
    val relatedRawSmsId: Long?,
    val resolved: Boolean = false
)
