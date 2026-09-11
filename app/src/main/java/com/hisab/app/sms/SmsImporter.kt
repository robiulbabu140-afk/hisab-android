package com.hisab.app.sms

import android.content.Context
import android.provider.Telephony
import com.hisab.app.data.repository.SmsRepository
import com.hisab.app.sms.parsers.SmsParserRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One-time backfill: reads the existing SMS inbox (requires READ_SMS, granted during the
 * onboarding flow) and queues any recognizable transaction SMS the live [SmsReceiver] never
 * saw because it arrived before the app was installed. Safe to re-run — [SmsRepository]'s
 * dedup hash silently skips anything already imported.
 */
object SmsImporter {
    suspend fun importHistorical(context: Context, smsRepository: SmsRepository, limit: Int = 500): Int =
        withContext(Dispatchers.IO) {
            var imported = 0
            val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT $limit"
            ) ?: return@withContext 0

            cursor.use {
                val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
                while (it.moveToNext()) {
                    val sender = it.getString(addressIdx) ?: continue
                    val body = it.getString(bodyIdx) ?: continue
                    val timestamp = it.getLong(dateIdx)
                    val parsed = SmsParserRegistry.tryParse(sender, body) ?: continue
                    val wasNew = smsRepository.importParsed(sender, body, parsed, timestamp)
                    if (wasNew) imported++
                }
            }
            imported
        }
}
