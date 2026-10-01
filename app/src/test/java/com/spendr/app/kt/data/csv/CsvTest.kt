package com.spendr.app.kt.data.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CsvCodecTest {

    @Test
    fun `quotes fields containing commas quotes or newlines`() {
        assertEquals("\"a,b\"", CsvCodec.quoteField("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvCodec.quoteField("say \"hi\""))
        assertEquals("\"line1\nline2\"", CsvCodec.quoteField("line1\nline2"))
        assertEquals(" plain ", CsvCodec.quoteField(" plain "))
    }

    @Test
    fun `serialize joins with CRLF and no trailing newline`() {
        val csv = CsvCodec.serializeRows(listOf(listOf("a", "b"), listOf("c,d", "e")))
        assertEquals("a,b\r\n\"c,d\",e", csv)
    }

    @Test
    fun `tokenize handles quoted commas escapes and newlines`() {
        val rows = CsvCodec.tokenize(
            "1,\"a,\"\"b\"\"\",x\n2,\"multi\nline\",y\r\n3,,z",
        )
        assertEquals(3, rows.size)
        assertEquals(listOf("1", "a,\"b\"", "x"), rows[0])
        assertEquals(listOf("2", "multi\nline", "y"), rows[1])
        assertEquals(listOf("3", "", "z"), rows[2])
    }

    @Test
    fun `tokenize drops empty lines but keeps whitespace-only fields`() {
        val rows = CsvCodec.tokenize("a,b\n\n  ,\nc,d")
        assertEquals(3, rows.size)
        assertEquals(listOf("a", "b"), rows[0])
        assertEquals(listOf("  ", ""), rows[1])
        assertEquals(listOf("c", "d"), rows[2])
    }

    @Test
    fun `resolve column is case-insensitive and trimmed`() {
        val header = listOf(" Note ", "AMOUNT")
        assertEquals(0, CsvCodec.resolveColumn(header, "note"))
        assertEquals(1, CsvCodec.resolveColumn(header, " amount "))
        assertEquals(-1, CsvCodec.resolveColumn(header, "missing"))
    }
}

class SpendrCsvTest {

    private val sample = SpendrRecord(
        version = "1",
        date = 1_783_560_600_000,
        paidAmount = 18_000,
        originalAmount = 20_000,
        discountAmount = 2_000,
        discountType = "fixed",
        categoryName = "Snack & Coffee",
        categoryIcon = "coffee-outline",
        categoryColor = "#A86086",
        note = "Coffee, premium",
        merchant = "Kopi \"Place\"",
        tags = "morning,work",
        createdAt = 10,
        updatedAt = 20,
    )

    @Test
    fun `round-trips records with quoted values`() {
        val csv = SpendrCsv.serialize(listOf(sample))
        val parsed = SpendrCsv.parse(csv)
        assertEquals(listOf(sample), parsed)
    }

    @Test
    fun `header retains the original fields and adds receipt columns`() {
        val csv = SpendrCsv.serialize(listOf(sample))
        assertEquals(
            "Spendr Version,Transaction Date,Paid Amount,Original Amount,Discount Amount," +
                "Discount Type,Category,Category Icon,Category Color,Note,Merchant,Tags," +
                "Created At,Updated At,Type,Receipt Items",
            csv.substringBefore("\r\n"),
        )
    }

    @Test
    fun `normalizes invalid discount metadata keeping paidAmount authoritative`() {
        val csv = SpendrCsv.serialize(listOf(sample)).replace(
            "20000,2000,fixed",
            "10000,2000,",
        )
        val parsed = SpendrCsv.parse(csv)
        assertEquals(1, parsed.size)
        val record = parsed[0]
        assertEquals(20_000L, record.originalAmount)
        assertEquals(2_000L, record.discountAmount)
        assertEquals("fixed", record.discountType)
        assertEquals(18_000L, record.paidAmount)
    }

    @Test
    fun `skips rows without valid paid amount or date`() {
        val header = SpendrCsv.serialize(emptyList())
        val badPaid = "$header\r\n1,1783560600000,0,,,,Snack,tag,#A86086,Coffee,,,10,20"
        val badDate = "$header\r\n1,not-a-date,18000,,,,Snack,tag,#A86086,Coffee,,,10,20"
        assertEquals(0, SpendrCsv.parse(badPaid).size)
        assertEquals(0, SpendrCsv.parse(badDate).size)
    }

    @Test
    fun `accepts ISO date fallback`() {
        val header = SpendrCsv.serialize(emptyList())
        val csv = "$header\r\n1,2026-07-09T08:30:00.000Z,18000,,,,Snack,tag,#A86086,,,,,"
        val parsed = SpendrCsv.parse(csv)
        assertEquals(1, parsed.size)
        assertEquals(1_783_585_800_000L, parsed[0].date)
    }

    @Test
    fun `maps empty optional strings to null`() {
        val header = SpendrCsv.serialize(emptyList())
        val csv = "$header\r\n1,1783560600000,18000,,,,Snack,tag,#A86086,,,,,"
        val input = SpendrCsv.recordToInput(SpendrCsv.parse(csv)[0], categoryId = 9)
        assertNull(input.note)
        assertNull(input.merchant)
        assertNull(input.tags)
        assertEquals(9L, input.categoryId)
        assertEquals(18_000L, input.paidAmount)
    }
}

class MoneyLoverCsvTest {

    @Test
    fun `writes header and sequential negative-amount rows`() {
        val csv = MoneyLoverCsv.serialize(
            listOf(
                MoneyLoverRecord("Jajan susu", -107_000, "Shopping", "Spendr", "IDR", 0),
                MoneyLoverRecord("Croissant", -22_000, "Snack & Coffee", "Spendr", "IDR", 0),
            ),
        )
        val lines = csv.split("\r\n")
        assertEquals(MONEYLOVER_HEADER, lines[0])
        assertEquals("1,Jajan susu,-107000,Shopping,Spendr,IDR,01/01/1970,,False", lines[1])
        assertEquals("2,Croissant,-22000,Snack & Coffee,Spendr,IDR,01/01/1970,,False", lines[2])
    }

    @Test
    fun `quotes fields with commas and quotes`() {
        val csv = MoneyLoverCsv.serialize(
            listOf(MoneyLoverRecord("Rem sepeda \"premium\"", -94_000, "Health & Fitness", "Spendr", "IDR", 0)),
        )
        assertTrue(csv.contains("\"Rem sepeda \"\"premium\"\"\",-94000"))
    }

    @Test
    fun `truncates non-integer amounts toward zero`() {
        assertEquals(-123L, CsvCodec.numericCandidates("-123.9")!!.toDoubleOrNull()!!.toLong())
    }

    @Test
    fun `parses standard export with header`() {
        val csv = MONEYLOVER_HEADER + "\r\n" +
            "1,note,-100,C,Aceknya Bank,IDR,01/01/2026,,False\r\n" +
            "2,m,-200,C,Aceknya Bank,IDR,02/01/2026,,False"
        val records = MoneyLoverCsv.parse(csv)
        assertEquals(2, records.size)
        assertEquals("Aceknya Bank", records[0].account)
        assertEquals(-100L, records[0].amount)
        assertEquals(-200L, records[1].amount)
    }

    @Test
    fun `strips BOM and tolerates mixed line endings`() {
        val csv = "\uFEFFID,Note,Amount,Category,Account,Currency,Date,Event,Exclude Report\n" +
            "1,n,-100,C,A,IDR,01/01/2026,,False\r\n" +
            "2,m,-200,C,A,IDR,02/01/2026,,False"
        val records = MoneyLoverCsv.parse(csv)
        assertEquals(2, records.size)
    }

    @Test
    fun `parses quoted amounts with grouping commas`() {
        val csv = MONEYLOVER_HEADER + "\r\n" +
            "1,x,\"-1,305,000\",C,A,IDR,01/01/2026,,False"
        assertEquals(-1_305_000L, MoneyLoverCsv.parse(csv)[0].amount)
    }

    @Test
    fun `detects headerless data`() {
        val csv = "1,Croissant,-22000,Snack & Coffee,Spendr,IDR,11/06/2026,,False"
        val records = MoneyLoverCsv.parse(csv)
        assertEquals(1, records.size)
        assertEquals(-22_000L, records[0].amount)
    }

    @Test
    fun `round-trips tricky notes`() {
        val date = MoneyLoverCsv.parseMoneyLoverDate("01/01/2026")!!
        val records = listOf(
            MoneyLoverRecord("a, b, \"c\"", -1_000, "X", "Spendr", "IDR", date),
            MoneyLoverRecord("", -2_000, "X", "Spendr", "IDR", date),
            MoneyLoverRecord("newline\nin note", -3_000, "X", "Spendr", "IDR", date),
        )
        assertEquals(records, MoneyLoverCsv.parse(MoneyLoverCsv.serialize(records)))
    }

    @Test
    fun `date helpers format and parse local noon`() {
        assertEquals("09/07/2026", MoneyLoverCsv.formatMoneyLoverDate(epochOf(2026, 7, 9, 8, 30)))
        val parsed = MoneyLoverCsv.parseMoneyLoverDate("09/07/2026")
        assertEquals(12, hourOfDay(parsed!!))
        assertEquals(2026, MoneyLoverCsv.parseMoneyLoverDate("09/07/26")!!.let { yearOf(it) })
        assertNull(MoneyLoverCsv.parseMoneyLoverDate("31/02/2026"))
        assertNull(MoneyLoverCsv.parseMoneyLoverDate("not-a-date"))
        assertNull(MoneyLoverCsv.parseMoneyLoverDate(""))
        assertEquals(2026, yearOf(MoneyLoverCsv.parseMoneyLoverDate("2026/07/09")!!))
    }

    private fun epochOf(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        java.time.LocalDateTime.of(y, m, d, h, min).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun hourOfDay(ms: Long): Int =
        java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).hour

    private fun yearOf(ms: Long): Int =
        java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).year

    private fun assertTrue(b: Boolean) = org.junit.Assert.assertTrue(b)
}

class BuildExportNoteTest {

    @Test
    fun `returns note unchanged without discount`() {
        assertEquals("Coffee", buildExportNote("Coffee", null, null))
        assertEquals("Coffee", buildExportNote("Coffee", 0, null))
        assertEquals("Coffee", buildExportNote("Coffee", null, 20_000))
    }

    @Test
    fun `appends readable discount suffix`() {
        assertEquals(
            "Coffee (discount Rp 2.000, original Rp 20.000)",
            buildExportNote("Coffee", 2_000, 20_000),
        )
    }

    @Test
    fun `omits original when unavailable`() {
        assertEquals("Coffee (discount Rp 2.000)", buildExportNote("Coffee", 2_000, null))
    }

    @Test
    fun `empty note leads with the suffix`() {
        assertEquals("(discount Rp 2.000, original Rp 20.000)", buildExportNote("", 2_000, 20_000))
        assertEquals("(discount Rp 2.000)", buildExportNote(null, 2_000, null))
    }
}
