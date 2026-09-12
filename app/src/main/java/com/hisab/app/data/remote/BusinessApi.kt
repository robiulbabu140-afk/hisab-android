package com.hisab.app.data.remote

import org.json.JSONObject

data class ClientDue(val id: Long, val name: String, val contractRateMinor: Long, val dueMinor: Long)
data class BuyerDue(val buyerName: String, val dollarCents: Long, val dueMinor: Long)
data class ManagedPersonDue(val id: Long, val name: String, val remainingMinor: Long)

/**
 * The Boosting/Dollar-Sale/Managed-Money "business" screens are online-only — they read and
 * write straight from the web backend's ledger logic (weighted-average dollar cost, due
 * calculations) instead of duplicating that math into a local Room sync, since these entities
 * only ever get created/managed from either the phone or the web dashboard, never both offline.
 */
class BusinessApi(private val api: ApiClient) {
    suspend fun getClientsWithDue(): List<ClientDue> {
        val arr = api.getArray("clients.php")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ClientDue(o.getLong("id"), o.getString("name"), o.optLong("contract_rate_minor", 0), o.optLong("due_minor", 0))
        }
    }

    suspend fun recordClientPayment(clientId: Long, accountId: Long, amountMinor: Long, note: String?) {
        api.postForObject(
            "clients.php",
            JSONObject().put("action", "record_payment").put("client_id", clientId)
                .put("account_id", accountId).put("amount_minor", amountMinor).put("note", note ?: JSONObject.NULL)
        )
    }

    suspend fun getBuyersWithDue(): List<BuyerDue> {
        val arr = api.getArray("dollar_sales.php")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            BuyerDue(o.getString("buyer_name"), o.optLong("dollar_cents", 0), o.optLong("due_minor", 0))
        }
    }

    suspend fun recordDollarSalePayment(buyerName: String, accountId: Long, amountMinor: Long, note: String?) {
        api.postForObject(
            "dollar_sales.php",
            JSONObject().put("action", "record_payment").put("buyer_name", buyerName)
                .put("account_id", accountId).put("amount_minor", amountMinor).put("note", note ?: JSONObject.NULL)
        )
    }

    suspend fun getManagedPersons(): List<ManagedPersonDue> {
        val arr = api.getArray("managed.php")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ManagedPersonDue(o.getLong("id"), o.getString("name"), o.optLong("remaining_minor", 0))
        }
    }

    suspend fun recordManagedReceived(personId: Long, accountId: Long, amountMinor: Long, note: String?) =
        recordManaged("record_received", personId, accountId, amountMinor, note)

    suspend fun recordManagedPaid(personId: Long, accountId: Long, amountMinor: Long, note: String?) =
        recordManaged("record_paid", personId, accountId, amountMinor, note)

    private suspend fun recordManaged(action: String, personId: Long, accountId: Long, amountMinor: Long, note: String?) {
        api.postForObject(
            "managed.php",
            JSONObject().put("action", action).put("person_id", personId)
                .put("account_id", accountId).put("amount_minor", amountMinor).put("note", note ?: JSONObject.NULL)
        )
    }
}
