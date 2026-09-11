package com.hisab.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllOnce(): List<Category>

    @Query("SELECT * FROM categories WHERE kind = :kind ORDER BY name ASC")
    fun observeByKind(kind: CategoryKind): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): Category?

    @Query("SELECT * FROM categories WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: Long): Category?

    @Query("SELECT * FROM categories WHERE remoteId IS NULL")
    suspend fun getUnsynced(): List<Category>

    @Insert
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}
