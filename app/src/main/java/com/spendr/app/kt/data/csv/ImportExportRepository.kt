package com.spendr.app.kt.data.csv

import androidx.room.withTransaction
import com.spendr.app.kt.data.db.SpendrDatabase
import com.spendr.app.kt.data.repo.AUTO_CATEGORY_ICON
import com.spendr.app.kt.data.repo.CATEGORY_PALETTE
import com.spendr.app.kt.data.repo.CategoryRepository
import com.spendr.app.kt.data.repo.TransactionRepository
import com.spendr.app.kt.domain.formatAmount
import com.spendr.app.kt.domain.model.TransactionInput
import java.util.Locale

enum class CsvFormat { SPENDR, MONEY_LOVER }

data class ExportResult(val csv: String, val count: Int)

data class ImportResult(
    val added: Int,
    val skipped: Int,
    val categoriesCreated: Int,
)

/**
 * Export/import orchestration, ported from RN `importExportData.ts`: spendr CSV
 * (round-trippable, discounts preserved) and MoneyLover CSV (expenses negative,
 * discounts folded into the note).
 */
class ImportExportRepository(
    private val db: SpendrDatabase,
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
) {

    // --- export ---

    suspend fun exportTransactionsToCsv(format: CsvFormat): ExportResult {
        val rows = transactions.listTransactions()
        // chronological, id tiebreaker
        val ordered = rows.sortedWith(
            compareBy({ it.transaction.date }, { it.transaction.id }),
        )
        return when (format) {
            CsvFormat.SPENDR -> {
                val records = ordered.map { row ->
                    SpendrRecord(
                        version = SPENDR_CSV_VERSION,
                        date = row.transaction.date,
                        paidAmount = row.transaction.paidAmount,
                        originalAmount = row.transaction.originalAmount,
                        discountAmount = row.transaction.discountAmount,
                        discountType = row.transaction.discountType,
                        categoryName = row.categoryName,
                        categoryIcon = row.categoryIcon,
                        categoryColor = row.categoryColor,
                        note = row.transaction.note ?: "",
                        merchant = row.transaction.merchant ?: "",
                        tags = row.transaction.tags ?: "",
                        createdAt = row.transaction.createdAt,
                        updatedAt = row.transaction.updatedAt,
                    )
                }
                ExportResult(SpendrCsv.serialize(records), records.size)
            }
            CsvFormat.MONEY_LOVER -> {
                val records = ordered.map { row ->
                    MoneyLoverRecord(
                        note = buildExportNote(
                            row.transaction.note,
                            row.transaction.discountAmount,
                            row.transaction.originalAmount,
                        ),
                        amount = -maxOf(0, row.transaction.paidAmount),
                        category = row.categoryName,
                        account = EXPORT_ACCOUNT,
                        currency = EXPORT_CURRENCY,
                        date = row.transaction.date,
                    )
                }
                ExportResult(MoneyLoverCsv.serialize(records), records.size)
            }
        }
    }

    // --- import ---

    suspend fun importCsv(content: String, format: CsvFormat): ImportResult = db.withTransaction {
        val categoryList = categories.listCategories()
        val byName = categoryList.associateBy { it.name.trim().lowercase() }.toMutableMap()
        // RN buildImportContext: the palette cursor starts after the existing
        // categories, so auto-created colors continue the sequence
        var paletteCursor = categoryList.size
        var categoriesCreated = 0

        val existing = transactions.listTransactions()
        val seen = existing.mapTo(mutableSetOf()) { dedupeKey(it.transaction.date, it.transaction.paidAmount, it.transaction.note, it.categoryName) }

        var added = 0
        var skipped = 0

        suspend fun ensureCategory(name: String, icon: String?, color: String?): Long? {
            val trimmed = name.trim()
            if (trimmed.isNotEmpty()) {
                byName[trimmed.lowercase()]?.let { return it.id }
                val resolvedColor = color?.trim()?.takeIf { it.isNotEmpty() }
                    ?: CATEGORY_PALETTE[paletteCursor % CATEGORY_PALETTE.size].also { paletteCursor++ }
                val resolvedIcon = icon?.trim()?.takeIf { it.isNotEmpty() } ?: AUTO_CATEGORY_ICON
                val newId = categories.createCategory(trimmed, resolvedIcon, resolvedColor)
                // createCategory appends to max sort order; mirror into the cache
                categories.getCategory(newId)?.let { byName[trimmed.lowercase()] = it }
                categoriesCreated++
                return newId
            }
            return categoryList.firstOrNull()?.id
        }

        val inputs = when (format) {
            CsvFormat.SPENDR -> SpendrCsv.parse(content).map { record ->
                val categoryId = ensureCategory(record.categoryName, record.categoryIcon, record.categoryColor)
                    ?: return@map null
                Triple(
                    SpendrCsv.recordToInput(record, categoryId),
                    dedupeKey(record.date, record.paidAmount, record.note, record.categoryName),
                    record.merchant,
                )
            }
            CsvFormat.MONEY_LOVER -> MoneyLoverCsv.parse(content).map { record ->
                // RN importMoneyLoverCsv skip order: bad amount → bad date → non-positive
                val amount = record.amount ?: return@map null
                val dateMs = record.date ?: return@map null
                val paid = kotlin.math.abs(amount)
                if (paid <= 0) return@map null
                val categoryId = ensureCategory(record.category, null, null) ?: return@map null
                val note = record.note.ifEmpty { null }
                Triple(
                    TransactionInput(
                        paidAmount = paid,
                        originalAmount = null,
                        discountAmount = null,
                        discountType = null,
                        categoryId = categoryId,
                        note = note,
                        merchant = null,
                        tags = null,
                        date = dateMs,
                    ),
                    dedupeKey(dateMs, paid, record.note, record.category),
                    null,
                )
            }
        }

        for (entry in inputs) {
            if (entry == null) {
                skipped++
                continue
            }
            val (input, key, _) = entry
            if (!seen.add(key)) {
                skipped++
                continue
            }
            transactions.insertTransaction(input)
            added++
        }
        ImportResult(added, skipped, categoriesCreated)
    }
}

private fun dedupeKey(date: Long, paidAmount: Long, note: String?, categoryName: String?): String =
    "${localDay(date)}|$paidAmount|${note?.trim()?.lowercase()}|${categoryName?.trim()?.lowercase()}"

/** Discounts folded into the note text for MoneyLover (no discount columns). */
fun buildExportNote(note: String?, discountAmount: Long?, originalAmount: Long?): String {
    if (discountAmount == null || discountAmount <= 0) return note?.trim() ?: ""
    val parts = mutableListOf("discount Rp ${formatAmount(discountAmount)}")
    if (originalAmount != null && originalAmount > 0) {
        parts.add("original Rp ${formatAmount(originalAmount)}")
    }
    val suffix = parts.joinToString(", ")
    val trimmedNote = note?.trim().orEmpty()
    return if (trimmedNote.isEmpty()) "($suffix)" else "$trimmedNote ($suffix)"
}
