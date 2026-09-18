package com.spendr.app.kt.data.csv

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val MONEYLOVER_HEADER = "ID,Note,Amount,Category,Account,Currency,Date,Event,Exclude Report"
const val EXPORT_ACCOUNT = "Spendr"
const val EXPORT_CURRENCY = "IDR"

data class MoneyLoverRecord(
    val note: String,
    /** Null when the cell is not a finite number (RN NaN). */
    val amount: Long?,
    val category: String,
    val account: String,
    val currency: String,
    /** Null when the date cell does not parse (RN raw string). */
    val date: Long?,
)

object MoneyLoverCsv {

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun formatMoneyLoverDate(epochMs: Long): String =
        DATE_FORMAT.format(java.time.Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))

    /** DD/MM/YYYY (or YYYY/MM/DD) → local noon of that day; null when invalid. */
    fun parseMoneyLoverDate(raw: String): Long? {
        val trimmed = raw.trim()
        val match = Regex("^(\\d{1,4})/(\\d{1,2})/(\\d{1,4})$").find(trimmed) ?: return null
        val (a, b, c) = match.destructured
        val year: Int
        val month: Int
        val day: Int
        if (a.length == 4) {
            year = a.toInt(); month = b.toInt(); day = c.toInt()
        } else {
            day = a.toInt(); month = b.toInt()
            year = if (c.length == 2) 2000 + c.toInt() else c.toInt()
        }
        if (month !in 1..12 || day !in 1..31) return null
        return try {
            LocalDateTime.of(year, month, day, 12, 0, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    fun serialize(records: List<MoneyLoverRecord>): String {
        val rows = buildList {
            add(MONEYLOVER_HEADER.split(","))
            records.forEachIndexed { index, r ->
                add(
                    listOf(
                        (index + 1).toString(),
                        r.note,
                        (r.amount ?: 0L).toString(),
                        r.category,
                        r.account,
                        r.currency,
                        formatMoneyLoverDate(r.date ?: 0L),
                        "",
                        "False",
                    ),
                )
            }
        }
        return CsvCodec.serializeRows(rows)
    }

    /**
     * Name-matched columns with positional fallback; headerless data is detected
     * when the first row's Amount cell parses as a number.
     */
    fun parse(content: String): List<MoneyLoverRecord> {
        val rows = CsvCodec.tokenize(CsvCodec.stripBom(content))
        if (rows.isEmpty()) return emptyList()

        val headerRow = rows[0].map { it.trim() }
        val firstRowIsData = CsvCodec.numericCandidates(rows[0].getOrNull(2) ?: "")?.toDoubleOrNull() != null

        val noteIdx: Int
        val amountIdx: Int
        val categoryIdx: Int
        val accountIdx: Int
        val currencyIdx: Int
        val dateIdx: Int
        if (firstRowIsData) {
            noteIdx = 1; amountIdx = 2; categoryIdx = 3; accountIdx = 4; currencyIdx = 5; dateIdx = 6
        } else {
            noteIdx = firstOf(headerRow, "Note").let { if (it >= 0) it else 1 }
            amountIdx = firstOf(headerRow, "Amount").let { if (it >= 0) it else 2 }
            categoryIdx = firstOf(headerRow, "Category").let { if (it >= 0) it else 3 }
            accountIdx = firstOf(headerRow, "Account").let { if (it >= 0) it else 4 }
            currencyIdx = firstOf(headerRow, "Currency").let { if (it >= 0) it else 5 }
            dateIdx = firstOf(headerRow, "Date").let { if (it >= 0) it else 6 }
        }

        // RN keeps rows with NaN amounts / unparseable dates so the import
        // loop can count them as skipped; null fields mirror NaN here.
        return rows.drop(if (firstRowIsData) 0 else 1).map { row ->
            val amountRaw = CsvCodec.numericCandidates(CsvCodec.cell(row, amountIdx))
                ?.toDoubleOrNull()?.takeIf { it.isFinite() }?.toLong()
            MoneyLoverRecord(
                note = CsvCodec.cell(row, noteIdx).trim(),
                amount = amountRaw,
                category = CsvCodec.cell(row, categoryIdx).trim(),
                account = CsvCodec.cell(row, accountIdx).trim(),
                currency = CsvCodec.cell(row, currencyIdx).trim(),
                date = parseMoneyLoverDate(CsvCodec.cell(row, dateIdx)),
            )
        }
    }
}

/** Local calendar day key "YYYY-MM-DD", port of RN `localDay`. */
fun localDay(epochMs: Long): String {
    val d = java.time.Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate()
    return "%04d-%02d-%02d".format(d.year, d.monthValue, d.dayOfMonth)
}
