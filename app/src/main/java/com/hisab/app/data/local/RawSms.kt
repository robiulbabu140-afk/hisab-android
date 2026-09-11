package com.hisab.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An SMS the parser recognized as a possible transaction, sitting in the review queue.
 * Nothing here affects Income/Expense/balances until the user confirms it — see
 * [status] and [linkedTransactionId]. [dedupHash] has a unique index so the same SMS
 * (re-delivered, or re-imported from the inbox) is never queued twice.
 */
@Entity(tableName = "raw_sms", indices = [Index(value = ["dedupHash"], unique = true)])
data class RawSms(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    val provider: SmsProvider,
    val detectedType: String?,
    val amountMinor: Long,
    val timestampMillis: Long,
    val reference: String?,
    val dedupHash: String,
    val status: RawSmsStatus = RawSmsStatus.PENDING,
    val linkedTransactionId: Long? = null,
    val remoteId: Long? = null
)
