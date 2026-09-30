package com.spendr.app.kt.data.csv

import com.spendr.app.kt.domain.model.TransactionInput

const val SPENDR_CSV_VERSION = "1"

/** Spendr CSV record (camelCase mirror of the TS SpendrRecord). */
data class SpendrRecord(
    val version: String,
    val date: Long,
    val paidAmount: Long,
    val originalAmount: Long?,
    val discountAmount: Long?,
    val discountType: String?,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val note: String,
    val merchant: String,
    val tags: String,
    val createdAt: Long,
    val updatedAt: Long,
    val type: String = "standard",
    val receiptItems: String = "",
)

private val EXPORT_HEADER = listOf(
    "Spendr Version", "Transaction Date", "Paid Amount", "Original Amount",
    "Discount Amount", "Discount Type", "Category", "Category Icon",
    "Category Color", "Note", "Merchant", "Tags", "Created At", "Updated At", "Type", "Receipt Items",
)

private data class HeaderIndex(
    val version: Int,
    val date: Int,
    val paidAmount: Int,
    val originalAmount: Int,
    val discountAmount: Int,
    val discountType: Int,
    val categoryName: Int,
    val categoryIcon: Int,
    val categoryColor: Int,
    val note: Int,
    val merchant: Int,
    val tags: Int,
    val createdAt: Int,
    val updatedAt: Int,
    val type: Int,
    val receiptItems: Int,
)

object SpendrCsv {

    fun serialize(records: List<SpendrRecord>): String {
        val rows = buildList {
            add(EXPORT_HEADER)
            for (r in records) {
                add(
                    listOf(
                        SPENDR_CSV_VERSION,
                        r.date.toString(),
                        maxOf(0, r.paidAmount).toString(),
                        r.originalAmount?.toString() ?: "",
                        r.discountAmount?.toString() ?: "",
                        r.discountType ?: "",
                        r.categoryName,
                        r.categoryIcon,
                        r.categoryColor,
                        r.note,
                        r.merchant,
                        r.tags,
                        r.createdAt.toString(),
                        r.updatedAt.toString(),
                        r.type,
                        r.receiptItems,
                    ),
                )
            }
        }
        return CsvCodec.serializeRows(rows)
    }

    /** Alias-tolerant header resolution, same order as RN `parseSpendrCsv`. */
    fun parse(content: String): List<SpendrRecord> {
        val rows = CsvCodec.tokenize(CsvCodec.stripBom(content))
        if (rows.isEmpty()) return emptyList()

        val header = rows[0].map { it.trim() }
        val idx = HeaderIndex(
            version = firstOf(header, "Spendr Version", "Version"),
            date = firstOf(header, "Transaction Date", "Date", "Date Ms"),
            paidAmount = firstOf(header, "Paid Amount", "paidAmount", "Amount"),
            originalAmount = firstOf(header, "Original Amount", "originalAmount"),
            discountAmount = firstOf(header, "Discount Amount", "discountAmount"),
            discountType = firstOf(header, "Discount Type", "discountType"),
            categoryName = firstOf(header, "Category", "Category Name", "categoryName"),
            categoryIcon = firstOf(header, "Category Icon", "categoryIcon"),
            categoryColor = firstOf(header, "Category Color", "categoryColor"),
            note = firstOf(header, "Note"),
            merchant = firstOf(header, "Merchant"),
            tags = firstOf(header, "Tags"),
            createdAt = firstOf(header, "Created At", "createdAt"),
            updatedAt = firstOf(header, "Updated At", "updatedAt"),
            type = firstOf(header, "Type"),
            receiptItems = firstOf(header, "Receipt Items"),
        )
        val now = System.currentTimeMillis()

        return rows.drop(1).mapNotNull { row ->
            val paidRaw = parseInteger(CsvCodec.cell(row, idx.paidAmount)) ?: return@mapNotNull null
            val paid = maxOf(0, paidRaw)
            if (paid <= 0) return@mapNotNull null
            val dateRaw = parseEpochOrIsoDate(CsvCodec.cell(row, idx.date)) ?: return@mapNotNull null
            if (dateRaw <= 0) return@mapNotNull null

            var discount = parseInteger(CsvCodec.cell(row, idx.discountAmount))?.takeIf { it > 0 }
            var original = parseInteger(CsvCodec.cell(row, idx.originalAmount))?.takeIf { it > 0 }
            var discountType = parseDiscountType(CsvCodec.cell(row, idx.discountType))

            if (discount == null) {
                original = null
                discountType = null
            } else {
                if (original == null || original!! - paid != discount) {
                    original = paid + discount!!
                }
                if (discountType == null) discountType = "fixed"
            }

            SpendrRecord(
                version = CsvCodec.cell(row, idx.version).trim().ifEmpty { SPENDR_CSV_VERSION },
                date = dateRaw,
                paidAmount = paid,
                originalAmount = original,
                discountAmount = discount,
                discountType = discountType,
                categoryName = CsvCodec.cell(row, idx.categoryName).trim(),
                categoryIcon = CsvCodec.cell(row, idx.categoryIcon).trim(),
                categoryColor = CsvCodec.cell(row, idx.categoryColor).trim(),
                note = CsvCodec.cell(row, idx.note).trim(),
                merchant = CsvCodec.cell(row, idx.merchant).trim(),
                tags = CsvCodec.cell(row, idx.tags).trim(),
                createdAt = parseInteger(CsvCodec.cell(row, idx.createdAt)) ?: now,
                updatedAt = parseInteger(CsvCodec.cell(row, idx.updatedAt)) ?: now,
                type = CsvCodec.cell(row, idx.type).trim().ifEmpty { "standard" },
                receiptItems = CsvCodec.cell(row, idx.receiptItems),
            )
        }
    }

    fun recordToInput(record: SpendrRecord, categoryId: Long): TransactionInput =
        TransactionInput(
            paidAmount = record.paidAmount,
            originalAmount = record.originalAmount,
            discountAmount = record.discountAmount,
            discountType = record.discountType,
            categoryId = categoryId,
            note = record.note.ifEmpty { null },
            merchant = record.merchant.ifEmpty { null },
            tags = record.tags.ifEmpty { null },
            date = record.date,
        )
}

internal fun firstOf(header: List<String>, vararg names: String): Int {
    for (name in names) {
        val index = CsvCodec.resolveColumn(header, name)
        if (index >= 0) return index
    }
    return -1
}

/** JS Number()-ish parsing: strip whitespace/grouping commas, truncate toward zero. */
internal fun parseInteger(raw: String): Long? {
    val cleaned = CsvCodec.numericCandidates(raw) ?: return null
    val value = cleaned.toDoubleOrNull() ?: return null
    if (!value.isFinite()) return null
    return value.toLong()
}

internal fun parseEpochOrIsoDate(raw: String): Long? {
    val trimmed = raw.trim()
    parseInteger(trimmed)?.let { return it }
    if (trimmed.isEmpty()) return null
    return try {
        java.time.Instant.parse(trimmed).toEpochMilli()
    } catch (_: Exception) {
        try {
            java.time.LocalDateTime.parse(trimmed.take(19))
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}

internal fun parseDiscountType(raw: String): String? =
    when (raw.trim().lowercase()) {
        "fixed" -> "fixed"
        "percentage" -> "percentage"
        else -> null
    }
