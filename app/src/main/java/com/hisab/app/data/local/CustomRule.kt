package com.hisab.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Phase 2 feature: stored now so the schema doesn't need a migration later, but nothing
 * in Phase 1 reads or applies these rules yet — SMS review is always manual.
 */
@Entity(tableName = "custom_rules")
data class CustomRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchPattern: String,
    val suggestedType: TxnType,
    val suggestedCategoryId: Long?,
    val enabled: Boolean = true
)
