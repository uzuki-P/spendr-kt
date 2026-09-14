package com.spendr.app.kt.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded

/** Flat joined row: transaction columns + denormalized category display fields. */
data class TransactionWithCategoryRow(
    @Embedded val transaction: TransactionEntity,
    @ColumnInfo(name = "category_name") val categoryName: String,
    @ColumnInfo(name = "category_icon") val categoryIcon: String,
    @ColumnInfo(name = "category_color") val categoryColor: String,
)
