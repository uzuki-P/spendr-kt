package com.spendr.app.kt.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = androidx.room.ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("date"),
        Index("category_id"),
        // Ported from migration v2: backs note suggestions.
        Index(value = ["note", "date", "id"], orders = [Index.Order.ASC, Index.Order.DESC, Index.Order.DESC]),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "paid_amount") val paidAmount: Long,
    @ColumnInfo(name = "original_amount") val originalAmount: Long?,
    @ColumnInfo(name = "discount_amount") val discountAmount: Long?,
    @ColumnInfo(name = "discount_type") val discountType: String?,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val note: String?,
    val merchant: String?,
    val tags: String?,
    val date: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(defaultValue = "'standard'") val type: String = "standard",
)
