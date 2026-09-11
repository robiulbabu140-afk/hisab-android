package com.hisab.app.sms.parsers

import com.hisab.app.data.local.SmsProvider
import com.hisab.app.data.model.ParsedSms

/**
 * Loose fallback for bank alert SMS, which vary a lot by bank. Only fires on senders that
 * look like a bank shortcode/alert id AND contain a recognizable debit/credit keyword —
 * kept last in [com.hisab.app.sms.parsers.SmsParserRegistry] so bKash/Nagad are tried first.
 */
class BankGenericParser : SmsParser {
    override fun matches(sender: String): Boolean =
        sender.contains("bank", ignoreCase = true) ||
            Regex("""^[A-Z]{2,3}(BANK)?$""").matches(sender) ||
            Regex("""^\d{4,6}$""").matches(sender)

    override fun parse(sender: String, body: String): ParsedSms? {
        val amountMinor = extractAmountMinor(body) ?: return null
        val detectedType = when {
            body.contains("debit", ignoreCase = true) || body.contains("withdraw", ignoreCase = true) -> "Debit"
            body.contains("credit", ignoreCase = true) || body.contains("deposit", ignoreCase = true) -> "Credit"
            else -> return null
        }
        return ParsedSms(
            provider = SmsProvider.BANK,
            detectedType = detectedType,
            amountMinor = amountMinor,
            reference = extractReference(body)
        )
    }
}
