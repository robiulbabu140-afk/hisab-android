package com.hisab.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY id ASC")
    fun observeAll(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): Account?

    @Query("SELECT * FROM accounts WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: Long): Account?

    @Query("SELECT * FROM accounts WHERE remoteId IS NULL AND isArchived = 0")
    suspend fun getUnsynced(): List<Account>

    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY id ASC")
    suspend fun getAllOnce(): List<Account>

    @Query("SELECT COALESCE(SUM(balanceMinor), 0) FROM accounts WHERE isArchived = 0")
    fun observeTotalBalance(): Flow<Long>

    @Insert
    suspend fun insert(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Query("UPDATE accounts SET balanceMinor = balanceMinor + :deltaMinor WHERE id = :accountId")
    suspend fun adjustBalance(accountId: Long, deltaMinor: Long)
}
