package com.spendr.app.kt.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached QuickAdd suggestion, ranked by [rank]. Every row carries the local
 * day it was computed on ([computedOn], ISO `yyyy-MM-dd`); rows from an
 * earlier day are stale. No FK to categories: the UI drops rows whose
 * Category no longer exists.
 */
@Entity(tableName = "quick_add_suggestions")
data class QuickAddSuggestionEntity(
    @PrimaryKey val rank: Int,
    val note: String,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "paid_amount") val paidAmount: Long,
    @ColumnInfo(name = "use_count") val useCount: Long,
    @ColumnInfo(name = "latest_date") val latestDate: Long,
    @ColumnInfo(name = "computed_on") val computedOn: String,
)
