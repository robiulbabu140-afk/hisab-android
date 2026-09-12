package com.hisab.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BalanceGapDao {
    @Insert
    suspend fun insert(gap: BalanceGap): Long

    @Query("SELECT * FROM balance_gaps WHERE resolved = 0 ORDER BY detectedAtMillis DESC")
    fun observeUnresolved(): Flow<List<BalanceGap>>

    @Query("SELECT COUNT(*) FROM balance_gaps WHERE resolved = 0")
    fun observeUnresolvedCount(): Flow<Int>

    @Update
    suspend fun update(gap: BalanceGap)
}
