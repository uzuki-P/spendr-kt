package com.spendr.app.kt.data.db

import com.spendr.app.kt.data.repo.TransactionRepository
import com.spendr.app.kt.domain.monthCursor
import com.spendr.app.kt.domain.monthRange
import com.spendr.app.kt.domain.shiftMonth
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * Debug seeder, ported from RN `seedDebugData.ts`: 2–3M IDR per month over the
 * last 3 months, weekday/weekend probability, category weights, 10% fixed
 * discounts, per-category merchant/note pools. Never deletes existing data.
 */
object DebugSeeder {

    suspend fun seed(db: SpendrDatabase, now: Long = System.currentTimeMillis()) {
        val categories = db.categoryDao().list()
        require(categories.isNotEmpty()) { "No categories found. Seed categories first." }

        val repo = TransactionRepository(db)
        val zone = ZoneId.systemDefault()
        val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val random = Random(now)

        val merchantPools = mapOf(
            "food" to listOf("Warung Bu Sari", "Padang Sederhana", "Ayam Geprek", "Warkop"),
            "snack" to listOf("Kopi Kenangan", "Janji Jiwa", "Starbucks", "Kopi Toko Djawa"),
            "coffee" to listOf("Kopi Kenangan", "Janji Jiwa", "Starbucks"),
            "transport" to listOf("Gojek", "Grab", "BPJS Parkir"),
            "gojek" to listOf("Gojek"),
            "ojek" to listOf("Gojek", "Grab"),
            "shopping" to listOf("Indomaret", "Alfamart", "Tokopedia"),
            "indomaret" to listOf("Indomaret"),
            "alfamart" to listOf("Alfamart"),
            "bills" to listOf("PLN", "Telkomsel", "Indihome"),
            "health" to listOf("Apotek K24", "Halodoc"),
            "game" to listOf<String>(),
            "entertainment" to listOf("Cinepolis", "Netflix"),
        )
        val notePools = mapOf(
            "food" to listOf("Lunch", "Dinner", "Nasi padang", "Ayam geprek"),
            "snack" to listOf("Coffee", "Croissant", "Matcha latte"),
            "coffee" to listOf("Coffee", "Kopi susu"),
            "transport" to listOf("Go to office", "Go home", "Parking"),
            "gojek" to listOf("Ojek online"),
            "ojek" to listOf("Ojek online"),
            "shopping" to listOf("Weekly groceries", "Skincare", "Household items"),
            "indomaret" to listOf("Weekly groceries"),
            "alfamart" to listOf("Weekly groceries"),
            "bills" to listOf("Electricity", "Internet", "Phone credit"),
            "health" to listOf("Vitamins", "Doctor visit", "Medicine"),
            "game" to listOf("Steam sale", "Game top-up"),
            "entertainment" to listOf("Movie ticket", "Streaming"),
        )
        val amountRanges = mapOf(
            "snack" to 15_000L..50_000L,
            "coffee" to 15_000L..50_000L,
            "food" to 18_000L..80_000L,
            "transport" to 5_000L..50_000L,
            "shopping" to 30_000L..200_000L,
            "bills" to 50_000L..300_000L,
            "entertainment" to 30_000L..150_000L,
            "game" to 45_000L..200_000L,
            "health" to 20_000L..150_000L,
            "groceries" to 100_000L..400_000L,
        )
        val fallbackRange = 10_000L..100_000L

        fun weight(categoryName: String): Int {
            val n = categoryName.lowercase()
            return when {
                "food" in n || "makan" in n -> 3
                "snack" in n || "coffee" in n || "kopi" in n -> 3
                "transport" in n || "gojek" in n || "ojek" in n -> 2
                "shopping" in n || "indomaret" in n || "alfamart" in n -> 2
                else -> 1
            }
        }

        val weighted = buildList {
            categories.forEach { c -> repeat(weight(c.name)) { add(c) } }
        }

        for (monthOffset in 0..2) {
            val cursor = shiftMonth(monthCursor(now), -monthOffset)
            val range = monthRange(cursor)
            val yearMonth = java.time.YearMonth.from(
                java.time.Instant.ofEpochMilli(range.start).atZone(zone).toLocalDate(),
            )
            var monthTotal = 0L
            val target = random.nextLong(2_000_000, 3_000_000)

            for (dayOfMonth in 1..yearMonth.lengthOfMonth()) {
                val date = yearMonth.atDay(dayOfMonth)
                if (date > today) break
                val isWeekend = date.dayOfWeek == java.time.DayOfWeek.SATURDAY ||
                    date.dayOfWeek == java.time.DayOfWeek.SUNDAY

                val count = when {
                    isWeekend -> when {
                        random.nextDouble() < 0.5 -> random.nextInt(1, 3)
                        random.nextDouble() < 0.3 -> random.nextInt(2, 4)
                        else -> 0
                    }
                    random.nextDouble() < 0.6 -> random.nextInt(1, 4)
                    random.nextDouble() < 0.2 -> 1
                    else -> 0
                }

                repeat(count) {
                    val category = weighted.random(random)
                    val key = category.name.lowercase()
                    val rangePair = amountRanges.entries.firstOrNull { key.contains(it.key) }?.value
                        ?: fallbackRange
                    val amount = (random.nextLong(rangePair.first, rangePair.last + 1) / 5_000) * 5_000
                    if (amount <= 0) return@repeat

                    val hour = if (random.nextDouble() < 0.7) 10 else 19
                    val minute = random.nextInt(0, 120)
                    val dateMs = java.time.LocalDateTime.of(
                        yearMonth.year,
                        yearMonth.monthValue,
                        dayOfMonth,
                        hour + minute / 60,
                        minute % 60,
                    ).atZone(zone).toInstant().toEpochMilli()

                    val discounted = random.nextDouble() < 0.10
                    val paid = if (discounted) {
                        val factor = 0.80 + random.nextDouble() * 0.15
                        ((amount * factor).toLong() / 1_000) * 1_000
                    } else {
                        amount
                    }
                    val merchantPool = merchantPools.entries.firstOrNull { key.contains(it.key) }?.value
                    val merchant = merchantPool?.takeIf { it.isNotEmpty() }?.random(random)
                    val notePool = notePools.entries.firstOrNull { key.contains(it.key) }?.value
                    val note = notePool?.takeIf { it.isNotEmpty() }?.random(random)

                    repo.insertTransaction(
                        com.spendr.app.kt.domain.model.TransactionInput(
                            paidAmount = paid,
                            originalAmount = if (discounted) amount else null,
                            discountAmount = if (discounted) amount - paid else null,
                            discountType = if (discounted) "fixed" else null,
                            categoryId = category.id,
                            note = note,
                            merchant = merchant,
                            tags = null,
                            date = dateMs,
                        ),
                    )
                    monthTotal += paid
                }

                if (monthTotal >= target && dayOfMonth > yearMonth.lengthOfMonth() * 0.7) break
            }
        }
        repo.rebuildNoteStats()
    }
}
