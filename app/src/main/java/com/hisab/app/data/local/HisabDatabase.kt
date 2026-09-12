package com.hisab.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities = [Account::class, Category::class, RawSms::class, Transaction::class, CustomRule::class, BalanceGap::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HisabDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun rawSmsDao(): RawSmsDao
    abstract fun transactionDao(): TransactionDao
    abstract fun customRuleDao(): CustomRuleDao
    abstract fun balanceGapDao(): BalanceGapDao

    companion object {
        @Volatile private var instance: HisabDatabase? = null

        fun getInstance(context: Context, seedScope: CoroutineScope): HisabDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context, HisabDatabase::class.java, "hisab.db")
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seedScope.launch { instance?.let { seedDefaults(it) } }
                        }
                    })
                    // No one has real data in an old schema yet (pre-release Phase 1 app), so a
                    // destructive fallback is fine here instead of writing a real Migration.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }

        private suspend fun seedDefaults(db: HisabDatabase) {
            val accountDao = db.accountDao()
            accountDao.insert(Account(name = "bKash", type = AccountType.MOBILE_WALLET, icon = "📱"))
            accountDao.insert(Account(name = "Nagad", type = AccountType.MOBILE_WALLET, icon = "🟠"))
            accountDao.insert(Account(name = "Bank", type = AccountType.BANK, icon = "🏦"))
            accountDao.insert(Account(name = "Cash", type = AccountType.CASH, icon = "💵"))

            val categoryDao = db.categoryDao()
            listOf(
                Category(name = "মাল / Stock", icon = "📦", kind = CategoryKind.EXPENSE),
                Category(name = "Facebook Ads", icon = "📢", kind = CategoryKind.EXPENSE),
                Category(name = "Salary", icon = "💼", kind = CategoryKind.EXPENSE),
                Category(name = "Courier", icon = "🚚", kind = CategoryKind.EXPENSE),
                Category(name = "Personal", icon = "🏠", kind = CategoryKind.EXPENSE),
                Category(name = "Food", icon = "🍛", kind = CategoryKind.EXPENSE),
                Category(name = "Transport", icon = "🚗", kind = CategoryKind.EXPENSE),
                Category(name = "Other", icon = "❓", kind = CategoryKind.EXPENSE),
                Category(name = "Business Sales", icon = "💼", kind = CategoryKind.INCOME),
                Category(name = "Customer Payment", icon = "🤝", kind = CategoryKind.INCOME),
                Category(name = "Other Income", icon = "❓", kind = CategoryKind.INCOME)
            ).forEach { categoryDao.insert(it) }
        }
    }
}
