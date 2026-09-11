package com.hisab.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RawSmsDao {
    @Query("SELECT * FROM raw_sms WHERE status = 'PENDING' ORDER BY timestampMillis DESC")
    fun observePending(): Flow<List<RawSms>>

    @Query("SELECT COUNT(*) FROM raw_sms WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM raw_sms WHERE id = :id")
    suspend fun getById(id: Long): RawSms?

    @Query("SELECT * FROM raw_sms ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<RawSms>>

    @Query("SELECT * FROM raw_sms WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: Long): RawSms?

    /** Not yet pushed to the backend. */
    @Query("SELECT * FROM raw_sms WHERE remoteId IS NULL ORDER BY id ASC")
    suspend fun getUnsynced(): List<RawSms>

    /** Already pushed, but still pending locally — used to pick up a status change made from the web dashboard's SMS Review page. */
    @Query("SELECT * FROM raw_sms WHERE remoteId IS NOT NULL AND status = 'PENDING'")
    suspend fun getSyncedPending(): List<RawSms>

    /** Returns -1 (Room's IGNORE sentinel) if [RawSms.dedupHash] already exists — the caller treats that as "already imported". */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rawSms: RawSms): Long

    @Update
    suspend fun update(rawSms: RawSms)
}
