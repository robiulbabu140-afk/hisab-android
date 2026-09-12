package com.hisab.app.sms.parsers

import com.hisab.app.data.local.SmsProvider
import com.hisab.app.data.model.ParsedSms

/**
 * Matches typical bKash notification senders and wording. Real sender ids vary by operator
 * (often just "bKash" or a numeric shortcode) — [matches] is intentionally loose and relies
 * on the body keywords as the real signal; false positives just land as an extra pending
 * SMS Review row the user can Ignore.
 */
class BkashParser : SmsParser {
    override fun matches(sender: String): Boolean =
        sender.contains("bkash", ignoreCase = true) || sender.contains("bKash")

    override fun parse(sender: String, body: String): ParsedSms? {
        val amountMinor = extractAmountMinor(body) ?: return null
        val detectedType = when {
            body.contains("Cash Out", ignoreCase = true) -> "Cash Out"
            body.contains("Send Money", ignoreCase = true) -> "Send Money"
            body.contains("received", ignoreCase = true) || body.contains("Cash In", ignoreCase = true) -> "Cash In / Received"
            body.contains("Payment", ignoreCase = true) -> "Payment"
            else -> return null
        }
        return ParsedSms(
            provider = SmsProvider.BKASH,
            detectedType = detectedType,
            amountMinor = amountMinor,
            reference = extractReference(body),
            balanceAfterMinor = extractBalanceAfterMinor(body)
        )
    }
}
