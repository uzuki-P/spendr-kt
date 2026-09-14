package com.spendr.app.kt.data.csv

/**
 * Minimal RFC-4180-like CSV codec, ported from RN `csv.ts`: comma delimiter,
 * quote a field iff it contains quote/CR/LF/comma, CRLF on export, tolerant
 * tokenizer on parse. No BOM, no trailing newline.
 */
object CsvCodec {

    fun quoteField(value: String): String =
        if (value.contains('"') || value.contains('\r') || value.contains('\n') || value.contains(',')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    fun serializeRows(rows: List<List<String>>): String =
        rows.joinToString("\r\n") { row -> row.joinToString(",") { quoteField(it) } }

    /** Char-by-char state machine; supports quoted newlines and "" escapes. */
    fun tokenize(content: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val field = StringBuilder()
        val current = mutableListOf<String>()
        var inQuotes = false
        var rowIsEmpty = true

        fun pushField() {
            current.add(field.toString())
            field.setLength(0)
        }

        fun pushRow() {
            pushField()
            if (!rowIsEmpty || current.any { it.isNotEmpty() }) {
                rows.add(current.toList())
            }
            current.clear()
            rowIsEmpty = true
        }

        var i = 0
        while (i < content.length) {
            val c = content[i]
            when {
                inQuotes -> when (c) {
                    '"' -> {
                        if (i + 1 < content.length && content[i + 1] == '"') {
                            field.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    }
                    else -> field.append(c)
                }
                c == '"' -> {
                    inQuotes = true
                    rowIsEmpty = false
                }
                c == ',' -> pushField()
                c == '\r' -> {
                    if (i + 1 < content.length && content[i + 1] == '\n') i++
                    pushRow()
                }
                c == '\n' -> pushRow()
                else -> {
                    field.append(c)
                    if (c != ' ' && c != '\t') rowIsEmpty = false
                }
            }
            i++
        }
        if (!rowIsEmpty || field.isNotEmpty() || current.isNotEmpty()) {
            pushRow()
        }
        return rows
    }

    fun stripBom(content: String): String =
        if (content.startsWith("\uFEFF")) content.substring(1) else content

    /** Case-insensitive, trim-tolerant header resolution; -1 when absent. */
    fun resolveColumn(header: List<String>, name: String): Int {
        val target = name.trim().lowercase()
        return header.indexOfFirst { it.trim().lowercase() == target }
    }

    fun cell(row: List<String>, index: Int): String =
        if (index in row.indices) row[index] else ""

    /** Removes all whitespace and grouping commas; empty → null. */
    fun numericCandidates(raw: String): String? {
        val cleaned = raw.filter { !it.isWhitespace() && it != ',' }
        return cleaned.ifEmpty { null }
    }
}
