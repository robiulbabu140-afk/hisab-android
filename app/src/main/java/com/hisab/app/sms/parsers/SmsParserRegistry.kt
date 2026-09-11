package com.hisab.app.sms.parsers

import com.hisab.app.data.model.ParsedSms

object SmsParserRegistry {
    private val parsers: List<SmsParser> = listOf(BkashParser(), NagadParser(), BankGenericParser())

    /** Tries each known provider parser in order; returns null if nothing recognized this SMS. */
    fun tryParse(sender: String, body: String): ParsedSms? =
        parsers.firstOrNull { it.matches(sender) }?.parse(sender, body)
}
