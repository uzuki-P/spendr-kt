package com.spendr.app.kt.data.db.dao

import androidx.room.ColumnInfo

data class TotalRow(val total: Long, val count: Long)

data class DailyTotalRow(
    val day: String,
    val total: Long,
    val count: Long,
)

data class MonthlyTotalRow(
    val month: String,
    val total: Long,
    val count: Long,
)

data class SuggestionRow(
    val note: String,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "category_name") val categoryName: String,
    @ColumnInfo(name = "category_icon") val categoryIcon: String,
    @ColumnInfo(name = "category_color") val categoryColor: String,
    val merchant: String?,
    @ColumnInfo(name = "paid_amount") val paidAmount: Long,
    @ColumnInfo(name = "original_amount") val originalAmount: Long?,
    @ColumnInfo(name = "discount_amount") val discountAmount: Long?,
    val date: Long,
    @ColumnInfo(name = "usage_count") val usageCount: Long,
)

data class CategoryTotalRow(
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "category_name") val categoryName: String,
    @ColumnInfo(name = "category_icon") val categoryIcon: String,
    @ColumnInfo(name = "category_color") val categoryColor: String,
    val total: Long,
    val count: Long,
)
