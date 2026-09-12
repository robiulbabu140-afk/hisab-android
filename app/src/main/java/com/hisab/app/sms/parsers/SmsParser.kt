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

/**
 * Most bKash/Nagad/bank alert SMS also state the account's balance *after* this transaction —
 * that's the raw material for gap detection (comparing what the app already has on record for
 * an account against what the bank/wallet itself just said the balance is, right after this
 * transaction, catches any transaction whose SMS was missed in between).
 *
 * Two orderings show up in the wild: "Balance is/: Tk X" (bKash, Nagad, EBL) and "Tk X Balance"
 * (some banks put the running balance right after the transaction amount, e.g. City Bank).
 */
internal fun extractBalanceAfterMinor(body: String): Long? {
    val afterWord = Regex("""Balance(?:\s+is)?[:\s]*(?:Tk|TK|BDT)?\.?\s?([0-9][0-9,]*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
        .find(body)
    if (afterWord != null) return parseAmountTokenToMinor(afterWord.groupValues[1])

    val beforeWord = Regex("""(?:Tk|TK|BDT)\.?\s?([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*Balance""", RegexOption.IGNORE_CASE)
        .find(body)
    return beforeWord?.let { parseAmountTokenToMinor(it.groupValues[1]) }
}

private fun parseAmountTokenToMinor(token: String): Long? {
    val cleaned = token.replace(",", "")
    val amountTaka = cleaned.toDoubleOrNull() ?: return null
    return Math.round(amountTaka * 100)
}
