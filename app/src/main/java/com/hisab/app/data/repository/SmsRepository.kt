package com.hisab.app.data.repository

import com.hisab.app.data.local.RawSms
import com.hisab.app.data.local.RawSmsDao
import com.hisab.app.data.local.RawSmsStatus
import com.hisab.app.data.model.ParsedSms
import kotlinx.coroutines.flow.Flow

class SmsRepository(private val dao: RawSmsDao) {
    fun observePending(): Flow<List<RawSms>> = dao.observePending()
    fun observePendingCount(): Flow<Int> = dao.observePendingCount()
    fun observeAll(): Flow<List<RawSms>> = dao.observeAll()
    suspend fun getById(id: Long): RawSms? = dao.getById(id)

    /** Inserts a newly parsed SMS into the review queue. Returns false if it was already imported. */
    suspend fun importParsed(sender: String, body: String, parsed: ParsedSms, timestampMillis: Long): Boolean {
        val rowId = dao.insert(
            RawSms(
                sender = sender,
                body = body,
                provider = parsed.provider,
                detectedType = parsed.detectedType,
                amountMinor = parsed.amountMinor,
                timestampMillis = timestampMillis,
                reference = parsed.reference,
                dedupHash = parsed.dedupHash(sender, timestampMillis)
            )
        )
        return rowId != -1L
    }

    suspend fun ignore(rawSmsId: Long) {
        val raw = dao.getById(rawSmsId) ?: return
        dao.update(raw.copy(status = RawSmsStatus.IGNORED))
    }

    suspend fun getUnsynced(): List<RawSms> = dao.getUnsynced()
    suspend fun getSyncedPending(): List<RawSms> = dao.getSyncedPending()
    suspend fun getByRemoteId(remoteId: Long): RawSms? = dao.getByRemoteId(remoteId)
    suspend fun markSynced(raw: RawSms, remoteId: Long) = dao.update(raw.copy(remoteId = remoteId))
    suspend fun updateStatus(raw: RawSms, status: RawSmsStatus) = dao.update(raw.copy(status = status))
}
