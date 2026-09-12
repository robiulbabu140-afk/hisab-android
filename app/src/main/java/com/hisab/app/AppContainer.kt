package com.hisab.app

import android.content.Context
import com.hisab.app.data.local.HisabDatabase
import com.hisab.app.data.remote.ApiClient
import com.hisab.app.data.remote.BusinessApi
import com.hisab.app.data.remote.SyncManager
import com.hisab.app.data.remote.SyncPrefs
import com.hisab.app.data.repository.AccountRepository
import com.hisab.app.data.repository.CategoryRepository
import com.hisab.app.data.repository.SmsRepository
import com.hisab.app.data.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Hand-rolled dependency container (no Hilt) so the dependency graph is easy to read and
 * doesn't add an annotation-processor build step I can't verify compiles on this machine.
 */
class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: HisabDatabase = HisabDatabase.getInstance(context, appScope)

    val accountRepository = AccountRepository(database.accountDao())
    val categoryRepository = CategoryRepository(database.categoryDao())
    val smsRepository = SmsRepository(database.rawSmsDao())
    val transactionRepository = TransactionRepository(database)
    val balanceGapDao = database.balanceGapDao()

    val syncPrefs = SyncPrefs(context)
    val syncManager = SyncManager(syncPrefs, accountRepository, categoryRepository, transactionRepository, smsRepository)
    val apiClient = ApiClient(syncPrefs)
    val businessApi = BusinessApi(apiClient)

    /**
     * Fire-and-forget sync using the app-level [appScope] instead of a screen's own
     * `rememberCoroutineScope()` — the latter gets cancelled the moment its composable leaves
     * composition (e.g. right after `navController.popBackStack()`), which would kill an
     * in-flight sync call before it finishes. This is what every screen should call for an
     * immediate "push what I just did" sync right after a local write.
     */
    fun syncInBackground() {
        if (!syncPrefs.isConfigured()) return
        appScope.launch { runCatching { syncManager.sync() } }
    }
}
