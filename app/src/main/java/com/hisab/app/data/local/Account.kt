package com.hisab.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * [balanceMinor] is stored in poisha (1 taka = 100 poisha) as a Long so balance math never
 * suffers floating-point rounding drift across thousands of small transactions.
 */
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val icon: String,
    val balanceMinor: Long = 0,
    val isArchived: Boolean = false,
    /** Row id on the web backend once pushed/linked there; null means not yet synced. */
    val remoteId: Long? = null
)
