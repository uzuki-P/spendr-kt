package com.spendr.app.kt.domain.model

data class NoteSuggestion(
    val note: String,
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val merchant: String?,
    val lastPaidAmount: Long,
    val lastOriginalAmount: Long?,
    val lastDiscountAmount: Long?,
    val lastDate: Long,
    val usageCount: Long,
)
