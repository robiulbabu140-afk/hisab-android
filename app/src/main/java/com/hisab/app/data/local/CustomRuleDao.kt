package com.hisab.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomRuleDao {
    @Query("SELECT * FROM custom_rules ORDER BY id ASC")
    fun observeAll(): Flow<List<CustomRule>>

    @Insert
    suspend fun insert(rule: CustomRule): Long
}
