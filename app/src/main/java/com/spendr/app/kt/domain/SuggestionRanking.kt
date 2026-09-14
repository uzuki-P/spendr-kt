package com.spendr.app.kt.domain

import com.spendr.app.kt.domain.model.NoteSuggestion

private const val MS_PER_DAY = 86_400_000.0

/**
 * Fuzzy note-suggestion ranking, ported from RN `utils/suggestions.ts`:
 * exact > prefix > substring > all-tokens, boosted by frequency, recency,
 * and same-category scope.
 */
fun rankSuggestions(
    query: String,
    list: List<NoteSuggestion>,
    categoryId: Long? = null,
    limit: Int = 8,
    nowMs: Long = System.currentTimeMillis(),
): List<NoteSuggestion> {
    val q = query.trim().lowercase()
    val scored = mutableListOf<Pair<NoteSuggestion, Long>>()

    for (suggestion in list) {
        val note = suggestion.note.lowercase()
        var score = 0L
        var matched = true

        if (q.isNotEmpty()) {
            when {
                note == q -> score += 100
                note.startsWith(q) -> score += 60
                note.contains(q) -> score += 35
                else -> {
                    val tokens = q.split(Regex("\\s+")).filter { it.isNotEmpty() }
                    val allTokensMatch = tokens.isNotEmpty() && tokens.all { note.contains(it) }
                    if (allTokensMatch) {
                        score += 18
                    } else {
                        matched = false
                    }
                }
            }
        } else {
            score += 6
        }

        if (!matched) continue

        score += minOf(suggestion.usageCount, 25L) * 2

        val daysSince = (nowMs - suggestion.lastDate) / MS_PER_DAY
        score += maxOf(0.0, 30.0 - daysSince).toLong()

        if (categoryId != null && suggestion.categoryId == categoryId) {
            score += 30
        }

        scored.add(suggestion to score)
    }

    return scored
        .sortedWith(compareByDescending<Pair<NoteSuggestion, Long>> { it.second }.thenByDescending { it.first.lastDate })
        .take(limit)
        .map { it.first }
}

fun topSuggestionsForCategory(
    categoryId: Long,
    list: List<NoteSuggestion>,
    limit: Int = 6,
): List<NoteSuggestion> = rankSuggestions("", list, categoryId, limit)
