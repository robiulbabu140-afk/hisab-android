package com.hisab.app.data.local

/** Kind of account holding money. */
enum class AccountType { MOBILE_WALLET, BANK, CASH, OTHER }

/**
 * The four transaction kinds from the Hisab design: only INCOME/EXPENSE move the
 * Income/Expense totals. TRANSFER moves money between two of the user's own accounts
 * (no Income/Expense impact). NEUTRAL touches one account's balance but is excluded
 * from Income/Expense reporting (e.g. money that passed through on someone else's behalf).
 */
enum class TxnType { INCOME, EXPENSE, TRANSFER, NEUTRAL }

enum class CategoryKind { INCOME, EXPENSE }

/** Where a raw SMS is in the review pipeline. Only CONFIRMED ever creates a Transaction. */
enum class RawSmsStatus { PENDING, CONFIRMED, IGNORED }

enum class SmsProvider { BKASH, NAGAD, ROCKET, BANK, UNKNOWN }

enum class TxnSource { MANUAL, SMS }
