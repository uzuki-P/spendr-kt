package com.spendr.app.kt.domain

import com.spendr.app.kt.domain.model.NoteSuggestion
import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestionRankingTest {

    private val now = 1_800_000_000_000L

    private fun suggestion(
        note: String,
        usage: Long = 1,
        lastDate: Long = now,
        categoryId: Long = 1,
    ) = NoteSuggestion(
        note = note,
        categoryId = categoryId,
        categoryName = "Cat",
        categoryIcon = "tag",
        categoryColor = "#A86086",
        merchant = null,
        lastPaidAmount = 10_000,
        lastOriginalAmount = null,
        lastDiscountAmount = null,
        lastDate = lastDate,
        usageCount = usage,
    )

    @Test
    fun `exact match outranks prefix outranks substring`() {
        val ranked = rankSuggestions(
            "kopi",
            listOf(suggestion("kopi susu"), suggestion("kopi"), suggestion("premium kopi")),
            nowMs = now,
        )
        assertEquals(listOf("kopi", "kopi susu", "premium kopi"), ranked.map { it.note })
    }

    @Test
    fun `all tokens matching note scores when no substring`() {
        val ranked = rankSuggestions(
            "kopi premium",
            listOf(suggestion("premium kopi susu")),
            nowMs = now,
        )
        assertEquals(listOf("premium kopi susu"), ranked.map { it.note })
    }

    @Test
    fun `non-matching notes are excluded`() {
        val ranked = rankSuggestions("kopi", listOf(suggestion("teh")), nowMs = now)
        assertEquals(0, ranked.size)
    }

    @Test
    fun `frequency is capped and recency decays`() {
        val highUsageOld = suggestion("n1", usage = 40, lastDate = now - 40L * 86_400_000)
        val lowUsageNew = suggestion("n2", usage = 4, lastDate = now)
        val ranked = rankSuggestions("n", listOf(highUsageOld, lowUsageNew), nowMs = now)
        // n1: 6 + 50 + 0 = 56; n2: 6 + 8 + 30 = 44
        assertEquals(listOf("n1", "n2"), ranked.map { it.note })
    }

    @Test
    fun `same category gets a boost and ties break on recency`() {
        val a = suggestion("kopi", usage = 1, lastDate = now - 1_000, categoryId = 2)
        val b = suggestion("kopi", usage = 1, lastDate = now, categoryId = 1)
        val ranked = rankSuggestions("kopi", listOf(a, b), categoryId = 1, nowMs = now)
        assertEquals("kopi", ranked[0].note)
        assertEquals(1L, ranked[0].categoryId)
    }

    @Test
    fun `empty query keeps all with base score`() {
        val ranked = rankSuggestions("", listOf(suggestion("a"), suggestion("b")), nowMs = now)
        assertEquals(2, ranked.size)
    }

    @Test
    fun `topSuggestionsForCategory filters by category scope boost`() {
        val other = suggestion("kopi", usage = 1, categoryId = 9)
        val same = suggestion("kopi dinkes", usage = 1, categoryId = 1)
        val ranked = topSuggestionsForCategory(1, listOf(other, same))
        assertEquals("kopi dinkes", ranked[0].note)
    }
}
