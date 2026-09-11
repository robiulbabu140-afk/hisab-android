package com.hisab.app.sms.parsers

import com.hisab.app.data.model.ParsedSms

/** One parser per provider. [matches] checks the sender id; [parse] extracts the amount etc. */
interface SmsParser {
    fun matches(sender: String): Boolean
    fun parse(sender: String, body: String): ParsedSms?
}

/** Shared amount-extraction helper: finds the first "Tk 12,345.00" / "BDT 500" style number. */
internal fun extractAmountMinor(body: String): Long? {
    val match = Regex("""(?:Tk|TK|BDT)\.?\s?([0-9][0-9,]*(?:\.[0-9]{1,2})?)""").find(body)
        ?: return null
    val cleaned = match.groupValues[1].replace(",", "")
    val amountTaka = cleaned.toDoubleOrNull() ?: return null
    return Math.round(amountTaka * 100)
}

internal fun extractReference(body: String): String? =
    Regex("""(?:Ref(?:erence)?|TrxID|TXN ID)[.:]?\s*([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE)
        .find(body)?.groupValues?.get(1)
