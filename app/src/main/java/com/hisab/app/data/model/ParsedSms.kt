package com.hisab.app.data.model

import com.hisab.app.data.local.SmsProvider

/** What a [com.hisab.app.sms.parsers.SmsParser] extracts from one SMS body. */
data class ParsedSms(
    val provider: SmsProvider,
    val detectedType: String,
    val amountMinor: Long,
    val reference: String?,
    /** The account's balance right after this transaction, per the SMS itself — used for gap detection. Null if the SMS didn't state one. */
    val balanceAfterMinor: Long? = null
) {
    /**
     * Stable dedup key independent of exact delivery timestamp (historical inbox import and
     * live broadcast receipt can see the same SMS with slightly different millisecond
     * timestamps). Prefers the bank/MFS reference number, which is unique per real
     * transaction. Falls back to sender+type+amount bucketed by day when no reference was
     * found in the SMS body — coarse, but still lets two genuinely separate same-amount
     * transactions on different days both come through.
     */
    fun dedupHash(sender: String, timestampMillis: Long): String =
        if (!reference.isNullOrBlank()) {
            "$sender|$reference|$amountMinor"
        } else {
            val dayBucket = timestampMillis / 86_400_000L
            "$sender|$detectedType|$amountMinor|$dayBucket"
        }
}
