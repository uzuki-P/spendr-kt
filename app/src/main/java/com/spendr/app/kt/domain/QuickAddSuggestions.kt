package com.spendr.app.kt.domain

/** One `(note, category, paidAmount)` group of recent Transactions. */
data class NoteAmountCount(
    val note: String,
    val categoryId: Long,
    val paidAmount: Long,
    val useCount: Long,
    val latestDate: Long,
)

/**
 * A frequently recorded `(note, category)` pair offered when creating a QuickAdd.
 * [paidAmount] is the amount used most often for that pair.
 */
data class QuickAddSuggestion(
    val note: String,
    val categoryId: Long,
    val paidAmount: Long,
    val useCount: Long,
    val latestDate: Long,
)

/**
 * Merges per-amount counts into one suggestion per `(note, category)`, keeps
 * pairs recorded at least [minCount] times, and ranks by count, then recency.
 */
fun rankQuickAddSuggestions(
    rows: List<NoteAmountCount>,
    minCount: Long = 2,
    limit: Int = 6,
): List<QuickAddSuggestion> =
    rows
        .groupBy { it.note.trim().lowercase() to it.categoryId }
        .values
        .map { group ->
            val usual = group.maxWith(compareBy<NoteAmountCount>({ it.useCount }, { it.latestDate }))
            QuickAddSuggestion(
                note = group.maxBy { it.latestDate }.note.trim(),
                categoryId = usual.categoryId,
                paidAmount = usual.paidAmount,
                useCount = group.sumOf { it.useCount },
                latestDate = group.maxOf { it.latestDate },
            )
        }
        .filter { it.useCount >= minCount }
        .sortedWith(compareByDescending<QuickAddSuggestion> { it.useCount }.thenByDescending { it.latestDate })
        .take(limit)
