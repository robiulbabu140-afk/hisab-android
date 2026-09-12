package com.hisab.app.sms.parsers

import com.hisab.app.data.local.SmsProvider
import com.hisab.app.data.model.ParsedSms

class NagadParser : SmsParser {
    override fun matches(sender: String): Boolean = sender.contains("nagad", ignoreCase = true)

    override fun parse(sender: String, body: String): ParsedSms? {
        val amountMinor = extractAmountMinor(body) ?: return null
        val detectedType = when {
            body.contains("Cash Out", ignoreCase = true) -> "Cash Out"
            body.contains("Send Money", ignoreCase = true) -> "Send Money"
            body.contains("received", ignoreCase = true) -> "Received"
            body.contains("Payment", ignoreCase = true) -> "Payment"
            else -> return null
        }
        return ParsedSms(
            provider = SmsProvider.NAGAD,
            detectedType = detectedType,
            amountMinor = amountMinor,
            reference = extractReference(body),
            balanceAfterMinor = extractBalanceAfterMinor(body)
        )
    }
}
