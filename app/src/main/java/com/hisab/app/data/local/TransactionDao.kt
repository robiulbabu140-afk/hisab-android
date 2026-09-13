package com.hisab.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId ORDER BY timestampMillis DESC")
    fun observeForAccount(accountId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Query(
        "SELECT COALESCE(SUM(amountMinor), 0) FROM transactions " +
            "WHERE type = :type AND timestampMillis BETWEEN :startMillis AND :endMillis"
    )
    fun observeSumForRange(type: TxnType, startMillis: Long, endMillis: Long): Flow<Long>

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    /** Rows not yet pushed to the backend. */
    @Query("SELECT * FROM transactions WHERE remoteId IS NULL ORDER BY id ASC")
    suspend fun getUnsynced(): List<Transaction>

    /** Every remote id already represented locally, so a pull can skip them cheaply. */
    @Query("SELECT remoteId FROM transactions WHERE remoteId IS NOT NULL")
    suspend fun getSyncedRemoteIds(): List<Long>

    /** Full rows for every already-synced transaction, so a pull can detect ones deleted server-side. */
    @Query("SELECT * FROM transactions WHERE remoteId IS NOT NULL")
    suspend fun getAllSynced(): List<Transaction>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions WHERE timestampMillis >= :startMillis AND timestampMillis < :endMillis")
    suspend fun getBetween(startMillis: Long, endMillis: Long): List<Transaction>
}
