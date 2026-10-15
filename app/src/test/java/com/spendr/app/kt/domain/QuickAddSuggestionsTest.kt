package com.spendr.app.kt.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickAddSuggestionsTest {

    private fun row(note: String, amount: Long, count: Long, date: Long = 1, categoryId: Long = 1) =
        NoteAmountCount(note, categoryId, amount, count, date)

    @Test
    fun `merges amounts per note and category and picks the usual amount`() {
        val ranked = rankQuickAddSuggestions(
            listOf(
                row("Kopi", 20_000, 3, date = 10),
                row("kopi", 25_000, 1, date = 20),
                row("Kopi", 20_000, 2, date = 5, categoryId = 2),
            ),
        )
        assertEquals(2, ranked.size)
        val first = ranked[0]
        assertEquals("kopi", first.note)
        assertEquals(1L, first.categoryId)
        assertEquals(20_000L, first.paidAmount)
        assertEquals(4L, first.useCount)
        assertEquals(20L, first.latestDate)
    }

    @Test
    fun `drops one-off notes and ranks by count then recency`() {
        val ranked = rankQuickAddSuggestions(
            listOf(
                row("Once", 5_000, 1),
                row("Parking", 2_000, 2, date = 1),
                row("Bus", 3_500, 2, date = 9),
                row("Lunch", 30_000, 5),
            ),
        )
        assertEquals(listOf("Lunch", "Bus", "Parking"), ranked.map { it.note })
    }
}
