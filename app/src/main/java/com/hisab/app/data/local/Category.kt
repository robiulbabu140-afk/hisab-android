package com.hisab.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val kind: CategoryKind,
    val isCustom: Boolean = false,
    val remoteId: Long? = null
)
