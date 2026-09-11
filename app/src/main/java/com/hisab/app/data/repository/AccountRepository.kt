package com.hisab.app.data.repository

import com.hisab.app.data.local.Account
import com.hisab.app.data.local.AccountDao
import kotlinx.coroutines.flow.Flow

class AccountRepository(private val dao: AccountDao) {
    fun observeAll(): Flow<List<Account>> = dao.observeAll()
    fun observeTotalBalance(): Flow<Long> = dao.observeTotalBalance()
    suspend fun getById(id: Long): Account? = dao.getById(id)
    suspend fun create(account: Account): Long = dao.insert(account)

    suspend fun getAllOnce(): List<Account> = dao.getAllOnce()
    suspend fun getByRemoteId(remoteId: Long): Account? = dao.getByRemoteId(remoteId)
    suspend fun getUnsynced(): List<Account> = dao.getUnsynced()
    suspend fun markSynced(account: Account, remoteId: Long) = dao.update(account.copy(remoteId = remoteId))
    suspend fun insertFromRemote(account: Account): Long = dao.insert(account)
}
