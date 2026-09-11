package com.hisab.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.hisab.app.HisabApp
import com.hisab.app.sms.parsers.SmsParserRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires on every incoming SMS. Only recognized bKash/Nagad/bank transaction SMS get queued
 * into `raw_sms` for review — everything else (OTPs, promos, personal texts) is ignored here
 * and never touches the app's data at all.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val app = context.applicationContext as? HisabApp ?: return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }
        val sender = messages.firstOrNull()?.originatingAddress ?: return
        val timestamp = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

        val parsed = SmsParserRegistry.tryParse(sender, fullBody) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.container.smsRepository.importParsed(sender, fullBody, parsed, timestamp)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
