package com.hisab.app.sms.parsers

import com.hisab.app.data.local.SmsProvider
import com.hisab.app.data.model.ParsedSms

/**
 * Loose fallback for bank alert SMS, which vary a lot by bank. Only fires on senders that
 * look like a bank shortcode/alert id AND contain a recognizable debit/credit keyword —
 * kept last in [com.hisab.app.sms.parsers.SmsParserRegistry] so bKash/Nagad are tried first.
 *
 * The specific bank (EBL, City Bank, DBBL, ...) is never hardcoded — every BD bank sends
 * alerts from a sender id that already reads as the bank's own name (e.g. "EBL", "CITY BANK"),
 * so [bankLabel] just cleans that sender id up and prefixes it onto the detected type
 * ("EBL Debit", "City Bank Withdrawal") — this works for any bank, not just the ones seen so far.
 */
class BankGenericParser : SmsParser {
    override fun matches(sender: String): Boolean =
        sender.contains("bank", ignoreCase = true) ||
            Regex("""^[A-Z]{2,4}(BANK)?$""").matches(sender) ||
            Regex("""^\d{4,6}$""").matches(sender)

    override fun parse(sender: String, body: String): ParsedSms? {
        val amountMinor = extractAmountMinor(body) ?: return null
        val action = when {
            body.contains("withdraw", ignoreCase = true) -> "Withdrawal"
            body.contains("debit", ignoreCase = true) -> "Debit"
            body.contains("deposit", ignoreCase = true) -> "Deposit"
            body.contains("credit", ignoreCase = true) -> "Credit"
            else -> return null
        }
        return ParsedSms(
            provider = SmsProvider.BANK,
            detectedType = "${bankLabel(sender)} $action".trim(),
            amountMinor = amountMinor,
            reference = extractReference(body)
        )
    }

    /** "EBL" -> "EBL", "CITY BANK" -> "City Bank", "16230" (a numeric shortcode) -> "Bank". */
    private fun bankLabel(sender: String): String {
        if (Regex("""^\d+$""").matches(sender)) return "Bank"
        if (Regex("""^[A-Z]{2,4}$""").matches(sender)) return sender
        return sender.lowercase()
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
    }
}
