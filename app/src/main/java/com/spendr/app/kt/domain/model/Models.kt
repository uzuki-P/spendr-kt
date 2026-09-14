package com.spendr.app.kt.domain.model

data class TransactionInput(
    val paidAmount: Long,
    val originalAmount: Long?,
    val discountAmount: Long?,
    val discountType: String?,
    val categoryId: Long,
    val note: String?,
    val merchant: String?,
    val tags: String?,
    val date: Long,
)

data class DateRange(val start: Long, val end: Long)

data class TotalSummary(val total: Long, val count: Long)

data class DailyTotal(
    val day: String,
    val total: Long,
    val count: Long,
)
