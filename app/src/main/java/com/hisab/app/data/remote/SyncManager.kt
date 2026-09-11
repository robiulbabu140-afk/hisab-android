package com.hisab.app.data.remote

import com.hisab.app.data.local.Account
import com.hisab.app.data.local.AccountType
import com.hisab.app.data.local.Category
import com.hisab.app.data.local.CategoryKind
import com.hisab.app.data.local.RawSmsStatus
import com.hisab.app.data.local.TxnSource
import com.hisab.app.data.local.TxnType
import com.hisab.app.data.repository.AccountRepository
import com.hisab.app.data.repository.CategoryRepository
import com.hisab.app.data.repository.SmsRepository
import com.hisab.app.data.repository.TransactionRepository
import org.json.JSONArray
import org.json.JSONObject

data class SyncResult(val success: Boolean, val message: String)

/**
 * Reconciles this device's Room database with the web backend (hisab-web's api PHP endpoints).
 *
 * Accounts/Categories: bidirectional, matched by name (both sides seed the same defaults
 * independently, so linking by name avoids duplicating them) — whichever side has a given
 * account/category first "wins" the create, the other side just links to it by [remoteId].
 *
 * Transactions: bidirectional by [remoteId] — local rows without one are pushed up; remote
 * rows not yet represented locally (e.g. added from the web dashboard) are pulled in and
 * replayed through the same balance-adjusting Ledger logic as a local entry, so both sides'
 * account balances stay consistent regardless of where a transaction was created.
 *
 * Raw SMS: push-only (only the phone ever receives real SMS), plus a light pull of just the
 * `status` field for rows already pushed — so classifying a pending SMS from the web
 * dashboard's SMS Review page removes it from the phone's pending list too, instead of
 * risking it being classified twice.
 */
class SyncManager(
    private val syncPrefs: SyncPrefs,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val smsRepository: SmsRepository
) {
    private val api = ApiClient(syncPrefs)

    suspend fun sync(): SyncResult {
        if (!syncPrefs.isConfigured()) {
            return SyncResult(false, "Backend URL/API Key সেট করা হয়নি — Settings-এ দিন")
        }
        return try {
            syncAccounts()
            syncCategories()
            // Push SMS before transactions: a transaction created from an SMS needs its raw_sms
            // row's *remote* id already resolved so the server can link the two (see push loop
            // in syncTransactions). Status pull happens after, once any web-side confirmations
            // have had a chance to show up in the transaction pull below.
            pushRawSms()
            syncTransactions()
            pullRawSmsStatus()
            syncPrefs.lastSyncMillis = System.currentTimeMillis()
            SyncResult(true, "Sync সম্পন্ন হয়েছে")
        } catch (e: ApiException) {
            SyncResult(false, "Sync ব্যর্থ: ${e.message}")
        } catch (e: Exception) {
            SyncResult(false, "Sync ব্যর্থ: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private suspend fun syncAccounts() {
        val remoteAccounts = api.getArray("accounts.php").toObjectList()
        val remoteByName = remoteAccounts.associateBy { it.getString("name").trim().lowercase() }

        for (local in accountRepository.getUnsynced()) {
            val match = remoteByName[local.name.trim().lowercase()]
            val remoteId = match?.getLong("id") ?: api.postForObject(
                "accounts.php",
                JSONObject().put("name", local.name).put("type", local.type.name).put("icon", local.icon)
            ).getLong("id")
            accountRepository.markSynced(local, remoteId)
        }

        val localAll = accountRepository.getAllOnce()
        val localRemoteIds = localAll.mapNotNull { it.remoteId }.toHashSet()
        val localNames = localAll.map { it.name.trim().lowercase() }.toHashSet()
        for (remote in remoteAccounts) {
            val remoteId = remote.getLong("id")
            if (remoteId in localRemoteIds || remote.getString("name").trim().lowercase() in localNames) continue
            accountRepository.insertFromRemote(
                Account(
                    name = remote.getString("name"),
                    type = runCatching { AccountType.valueOf(remote.getString("type")) }.getOrDefault(AccountType.OTHER),
                    icon = remote.optString("icon", "💳"),
                    balanceMinor = remote.optLong("balance_minor", 0),
                    remoteId = remoteId
                )
            )
        }
    }

    private suspend fun syncCategories() {
        val remoteCategories = api.getArray("categories.php").toObjectList()
        val remoteByKey = remoteCategories.associateBy { it.categoryKey() }

        for (local in categoryRepository.getUnsynced()) {
            val key = "${local.kind.name}|${local.name.trim().lowercase()}"
            val match = remoteByKey[key]
            val remoteId = match?.getLong("id") ?: api.postForObject(
                "categories.php",
                JSONObject().put("name", local.name).put("kind", local.kind.name).put("icon", local.icon)
            ).getLong("id")
            categoryRepository.markSynced(local, remoteId)
        }

        val localAll = categoryRepository.getAllOnce()
        val localRemoteIds = localAll.mapNotNull { it.remoteId }.toHashSet()
        val localKeys = localAll.map { "${it.kind.name}|${it.name.trim().lowercase()}" }.toHashSet()
        for (remote in remoteCategories) {
            val remoteId = remote.getLong("id")
            if (remoteId in localRemoteIds) continue
            val kind = runCatching { CategoryKind.valueOf(remote.getString("kind")) }.getOrNull() ?: continue
            if (remote.categoryKey() in localKeys) continue
            categoryRepository.insertFromRemote(
                Category(
                    name = remote.getString("name"),
                    icon = remote.optString("icon", "🏷️"),
                    kind = kind,
                    isCustom = remote.optInt("is_custom", 0) == 1,
                    remoteId = remoteId
                )
            )
        }
    }

    private fun JSONObject.categoryKey() = "${getString("kind")}|${getString("name").trim().lowercase()}"

    private suspend fun syncTransactions() {
        for (txn in transactionRepository.getUnsynced()) {
            val fromRemoteId = accountRepository.getById(txn.accountId)?.remoteId ?: continue
            val rawSmsRemoteId = txn.rawSmsId?.let { smsRepository.getById(it)?.remoteId }
            val body = JSONObject()
                .put("amount_minor", txn.amountMinor)
                .put("timestamp_millis", txn.timestampMillis)
                .put("source", txn.source.name)
                .put("note", txn.note ?: JSONObject.NULL)
                .put("raw_sms_id", rawSmsRemoteId ?: JSONObject.NULL)

            when (txn.type) {
                TxnType.INCOME, TxnType.EXPENSE -> {
                    val categoryRemoteId = txn.categoryId?.let { categoryRepository.getById(it)?.remoteId } ?: continue
                    body.put("action", if (txn.type == TxnType.INCOME) "income" else "expense")
                        .put("account_id", fromRemoteId)
                        .put("category_id", categoryRemoteId)
                }
                TxnType.TRANSFER -> {
                    val toRemoteId = txn.toAccountId?.let { accountRepository.getById(it)?.remoteId } ?: continue
                    body.put("action", "transfer").put("from_account_id", fromRemoteId).put("to_account_id", toRemoteId)
                }
                TxnType.NEUTRAL -> {
                    body.put("action", "neutral").put("account_id", fromRemoteId).put("is_inflow", txn.isInflow)
                }
            }
            val remoteId = api.postForObject("transactions.php", body).getLong("id")
            transactionRepository.markSynced(txn, remoteId)
        }

        val alreadyPulled = transactionRepository.getSyncedRemoteIds().toHashSet()
        for (remote in api.getArray("transactions.php").toObjectList()) {
            val remoteId = remote.getLong("id")
            if (remoteId in alreadyPulled) continue

            val localAccountId = accountRepository.getByRemoteId(remote.getLong("account_id"))?.id ?: continue
            val localToAccountId = remote.optLongOrNull("to_account_id")?.let { accountRepository.getByRemoteId(it)?.id }
            val localCategoryId = remote.optLongOrNull("category_id")?.let { categoryRepository.getByRemoteId(it)?.id }
            val localRawSmsId = remote.optLongOrNull("raw_sms_id")?.let { smsRepository.getByRemoteId(it)?.id }

            val amountMinor = remote.getLong("amount_minor")
            val note = if (remote.isNull("note")) null else remote.getString("note")
            val timestampMillis = remote.getLong("timestamp_millis")
            val source = runCatching { TxnSource.valueOf(remote.getString("source")) }.getOrDefault(TxnSource.MANUAL)

            when (runCatching { TxnType.valueOf(remote.getString("type")) }.getOrNull()) {
                TxnType.INCOME -> {
                    val categoryId = localCategoryId ?: continue
                    transactionRepository.recordIncome(localAccountId, categoryId, amountMinor, note, timestampMillis, source, localRawSmsId, remoteId)
                }
                TxnType.EXPENSE -> {
                    val categoryId = localCategoryId ?: continue
                    transactionRepository.recordExpense(localAccountId, categoryId, amountMinor, note, timestampMillis, source, localRawSmsId, remoteId)
                }
                TxnType.TRANSFER -> {
                    val toAccountId = localToAccountId ?: continue
                    transactionRepository.recordTransfer(localAccountId, toAccountId, amountMinor, note, timestampMillis, source, localRawSmsId, remoteId)
                }
                TxnType.NEUTRAL -> {
                    val isInflow = remote.optInt("is_inflow", 1) == 1
                    transactionRepository.recordNeutral(localAccountId, amountMinor, isInflow, note, timestampMillis, source, localRawSmsId, remoteId)
                }
                null -> continue
            }
        }
    }

    private suspend fun pushRawSms() {
        val unsynced = smsRepository.getUnsynced()
        if (unsynced.isEmpty()) return

        val items = JSONArray()
        unsynced.forEach { raw ->
            items.put(
                JSONObject()
                    .put("sender", raw.sender)
                    .put("body", raw.body)
                    .put("provider", raw.provider.name)
                    .put("detected_type", raw.detectedType ?: "")
                    .put("amount_minor", raw.amountMinor)
                    .put("timestamp_millis", raw.timestampMillis)
                    .put("reference", raw.reference ?: JSONObject.NULL)
                    .put("dedup_hash", raw.dedupHash)
            )
        }
        val response = api.postForObject("sms.php", JSONObject().put("action", "import").put("items", items))
        val hashToRemoteId = HashMap<String, Long>()
        response.getJSONArray("items").toObjectList().forEach { item ->
            hashToRemoteId[item.getString("dedup_hash")] = item.getLong("id")
        }
        unsynced.forEach { raw ->
            hashToRemoteId[raw.dedupHash]?.let { smsRepository.markSynced(raw, it) }
        }
    }

    /** Picks up a status change made from the web dashboard's SMS Review page (e.g. classified
     * or ignored there) for rows this device already pushed — see the class doc above. */
    private suspend fun pullRawSmsStatus() {
        val pendingSynced = smsRepository.getSyncedPending()
        if (pendingSynced.isEmpty()) return
        val remoteStatusById = api.getArray("sms.php").toObjectList().associate { it.getLong("id") to it.getString("status") }
        pendingSynced.forEach { raw ->
            val remoteId = raw.remoteId ?: return@forEach
            val remoteStatus = remoteStatusById[remoteId] ?: return@forEach
            if (remoteStatus != "PENDING") {
                val status = runCatching { RawSmsStatus.valueOf(remoteStatus) }.getOrDefault(RawSmsStatus.IGNORED)
                smsRepository.updateStatus(raw, status)
            }
        }
    }

    private fun JSONArray.toObjectList(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key)) null else optLong(key)
}
