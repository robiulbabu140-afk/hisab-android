package com.hisab.app.ui.navigation

object Dest {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val TRANSACTION_DETAIL = "transactionDetail/{id}"
    const val ADD = "add"
    const val SMS_REVIEW = "smsReview"
    const val SMS_DETAIL = "smsDetail/{id}"
    const val ACCOUNTS = "accounts"
    const val ACCOUNT_DETAIL = "accountDetail/{id}"
    const val CATEGORIES = "categories"
    const val TRANSFER = "transfer"
    const val NEUTRAL = "neutral"
    const val REPORTS = "reports"
    const val MORE = "more"
    const val SETTINGS = "settings"
    const val CLIENTS = "clients"
    const val DOLLAR_SALES = "dollarSales"
    const val MANAGED = "managed"
    const val SUPPLIERS = "suppliers"

    fun transactionDetail(id: Long) = "transactionDetail/$id"
    fun smsDetail(id: Long) = "smsDetail/$id"
    fun accountDetail(id: Long) = "accountDetail/$id"
}

/** Top-level tabs that show the bottom nav bar. */
val BOTTOM_NAV_ROUTES = setOf(Dest.DASHBOARD, Dest.TRANSACTIONS, Dest.REPORTS, Dest.MORE)
