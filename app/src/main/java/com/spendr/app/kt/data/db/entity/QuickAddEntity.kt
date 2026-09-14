package com.spendr.app.kt.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "quick_add",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class QuickAddEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val note: String?,
    @ColumnInfo(name = "paid_amount") val paidAmount: Long?,
    val merchant: String?,
    val tags: String?,
    @ColumnInfo(name = "sort_order") val sortOrder: Double,
)
