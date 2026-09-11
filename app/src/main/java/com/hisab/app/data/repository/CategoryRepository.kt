package com.hisab.app.data.repository

import com.hisab.app.data.local.Category
import com.hisab.app.data.local.CategoryDao
import com.hisab.app.data.local.CategoryKind
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val dao: CategoryDao) {
    fun observeAll(): Flow<List<Category>> = dao.observeAll()
    fun observeByKind(kind: CategoryKind): Flow<List<Category>> = dao.observeByKind(kind)
    suspend fun getById(id: Long): Category? = dao.getById(id)
    suspend fun create(category: Category): Long = dao.insert(category)

    suspend fun getAllOnce(): List<Category> = dao.getAllOnce()
    suspend fun getByRemoteId(remoteId: Long): Category? = dao.getByRemoteId(remoteId)
    suspend fun getUnsynced(): List<Category> = dao.getUnsynced()
    suspend fun markSynced(category: Category, remoteId: Long) = dao.update(category.copy(remoteId = remoteId))
    suspend fun insertFromRemote(category: Category): Long = dao.insert(category)
}
