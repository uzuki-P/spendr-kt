package com.spendr.app.kt.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountDraftTest {

    // --- tokenizer / append ---

    @Test
    fun `append builds digits with lone zero collapse`() {
        assertEquals("5", appendDigits(AmountDraft(), AmountField.AMOUNT, "5").amountStr)
        assertEquals("0", appendDigits(AmountDraft(), AmountField.AMOUNT, "0").amountStr)
        val zero = appendDigits(AmountDraft(), AmountField.AMOUNT, "0")
        assertEquals("5", appendDigits(zero, AmountField.AMOUNT, "5").amountStr)
    }

    @Test
    fun `append 000 keeps leading zeros that count toward the cap`() {
        val appended = appendDigits(AmountDraft(), AmountField.AMOUNT, "000")
        assertEquals("000", appended.amountStr)
        assertEquals("0005", appendDigits(appended, AmountField.AMOUNT, "5").amountStr)
    }

    @Test
    fun `append caps at 12 digits`() {
        val full = AmountDraft(amountStr = "123456789012")
        assertEquals("123456789012", appendDigits(full, AmountField.AMOUNT, "7").amountStr)
        assertEquals(
            "123456789010",
            appendDigits(
                AmountDraft(amountStr = "12345678901"),
                AmountField.AMOUNT,
                "000",
            ).amountStr,
        )
    }

    @Test
    fun `backspace drains to empty`() {
        val d = AmountDraft(amountStr = "30")
        assertEquals("3", backspace(d, AmountField.AMOUNT).amountStr)
        assertEquals("", backspace(backspace(d, AmountField.AMOUNT), AmountField.AMOUNT).amountStr)
        assertEquals("", backspace(AmountDraft(), AmountField.AMOUNT).amountStr)
    }

    // --- worked examples from docs/amount-fields.md (fixed mode) ---

    @Test
    fun `pay 50 with empty original fills original`() {
        var d = toggleDiscount(AmountDraft())
        d = appendDigits(d, AmountField.AMOUNT, "50")
        assertEquals("50", d.originalStr)
        assertEquals("", d.discountStr)
    }

    @Test
    fun `pay 30 with discount 20 fills original 50`() {
        var d = toggleDiscount(AmountDraft())
        d = appendDigits(d, AmountField.DISCOUNT, "20")
        d = appendDigits(d, AmountField.AMOUNT, "30")
        assertEquals("50", d.originalStr)
    }

    @Test
    fun `pay 30 with original 50 derives discount 20`() {
        var d = toggleDiscount(AmountDraft())
        d = appendDigits(d, AmountField.ORIGINAL, "50")
        d = clearField(d, AmountField.AMOUNT)
        d = appendDigits(d, AmountField.AMOUNT, "30")
        assertEquals("20", d.discountStr)
    }

    @Test
    fun `original 50 with discount 20 derives pay 30`() {
        var d = toggleDiscount(AmountDraft())
        d = appendDigits(d, AmountField.DISCOUNT, "20")
        d = appendDigits(d, AmountField.ORIGINAL, "50")
        assertEquals("30", d.amountStr)
    }

    @Test
    fun `original 50 with empty discount derives pay 50`() {
        var d = toggleDiscount(AmountDraft())
        d = appendDigits(d, AmountField.ORIGINAL, "50")
        assertEquals("50", d.amountStr)
    }

    // --- auto-calc edge cases ---

    @Test
    fun `pay above original clamps discount to empty`() {
        var d = toggleDiscount(AmountDraft())
        d = appendDigits(d, AmountField.ORIGINAL, "50")
        d = clearField(d, AmountField.AMOUNT)
        d = appendDigits(d, AmountField.AMOUNT, "60")
        assertEquals("", d.discountStr)
        assertEquals(0L, d.discountAmount)
    }

    @Test
    fun `percentage mode computes pay with half-up rounding`() {
        var d = toggleDiscount(AmountDraft())
        d = toggleDiscountType(d)
        d = appendDigits(d, AmountField.ORIGINAL, "200")
        d = appendDigits(d, AmountField.PCT, "10")
        assertEquals("180", d.amountStr)
    }

    @Test
    fun `pct field normalizes to max 100 and no zero`() {
        var d = toggleDiscount(AmountDraft())
        d = toggleDiscountType(d)
        d = appendDigits(d, AmountField.PCT, "150")
        assertEquals("100", d.pctStr)
        d = clearField(d, AmountField.PCT)
        d = appendDigits(d, AmountField.PCT, "0")
        assertEquals("", d.pctStr)
    }

    @Test
    fun `editing pay in percentage mode re-derives pct approximately`() {
        var d = toggleDiscount(AmountDraft())
        d = toggleDiscountType(d)
        d = appendDigits(d, AmountField.ORIGINAL, "200")
        d = appendDigits(d, AmountField.PCT, "10")
        d = clearField(d, AmountField.AMOUNT)
        d = appendDigits(d, AmountField.AMOUNT, "150")
        assertEquals("25", d.pctStr)
    }

    @Test
    fun `clearing original clears pay`() {
        var d = toggleDiscount(AmountDraft())
        d = appendDigits(d, AmountField.ORIGINAL, "50")
        d = clearField(d, AmountField.ORIGINAL)
        assertEquals("", d.amountStr)
    }

    @Test
    fun `deleting pay keeps original and discount`() {
        var d = toggleDiscount(AmountDraft())
        d = appendDigits(d, AmountField.ORIGINAL, "50")
        d = appendDigits(d, AmountField.DISCOUNT, "20")
        d = clearField(d, AmountField.AMOUNT)
        assertEquals("50", d.originalStr)
        assertEquals("20", d.discountStr)
    }

    @Test
    fun `discount before original has no effect on pay`() {
        var d = toggleDiscount(AmountDraft())
        d = appendDigits(d, AmountField.DISCOUNT, "20")
        assertEquals("", d.amountStr)
    }

    @Test
    fun `discount larger than original drives pay to empty`() {
        var d = toggleDiscount(AmountDraft())
        d = appendDigits(d, AmountField.ORIGINAL, "10")
        d = appendDigits(d, AmountField.DISCOUNT, "20")
        assertEquals("", d.amountStr)
        assertEquals("derived residual original - paid", 10L, d.discountAmount)
    }

    // --- toggles ---

    @Test
    fun `toggleDiscount on copies pay into original`() {
        val d = appendDigits(AmountDraft(), AmountField.AMOUNT, "40")
        val on = toggleDiscount(d)
        assertEquals("40", on.originalStr)
        assertTrue(on.hasDiscount)
        assertEquals(AmountField.DISCOUNT, on.activeField)
    }

    @Test
    fun `toggleDiscount off clears discount fields and keeps pay`() {
        var d = toggleDiscount(AmountDraft())
        d = appendDigits(d, AmountField.ORIGINAL, "50")
        d = clearField(d, AmountField.AMOUNT)
        d = appendDigits(d, AmountField.AMOUNT, "30")
        val off = toggleDiscount(d)
        assertFalse(off.hasDiscount)
        assertEquals("", off.originalStr)
        assertEquals("", off.discountStr)
        assertEquals("30", off.amountStr)
        assertEquals(AmountField.AMOUNT, off.activeField)
    }

    @Test
    fun `toggleDiscountType preserves paid amount`() {
        var d = toggleDiscount(AmountDraft())
        d = appendDigits(d, AmountField.ORIGINAL, "50")
        d = clearField(d, AmountField.AMOUNT)
        d = appendDigits(d, AmountField.AMOUNT, "30")

        val pct = toggleDiscountType(d)
        assertEquals("40", pct.pctStr)
        assertEquals(AmountField.PCT, pct.activeField)
        assertEquals("30", pct.amountStr)

        val backToFixed = toggleDiscountType(pct)
        assertEquals("20", backToFixed.discountStr)
        assertEquals(AmountField.DISCOUNT, backToFixed.activeField)
        assertEquals("30", backToFixed.amountStr)
    }

    // --- derived helpers and persistence ---

    @Test
    fun `discountAmount derives from strings in both modes`() {
        var d = toggleDiscount(AmountDraft())
        d = appendDigits(d, AmountField.ORIGINAL, "50")
        d = clearField(d, AmountField.AMOUNT)
        d = appendDigits(d, AmountField.AMOUNT, "30")
        assertEquals(20L, d.discountAmount)

        val pctDraft = toggleDiscountType(d)
        assertEquals(20L, pctDraft.discountAmount)

        val surcharge = clearField(d, AmountField.AMOUNT).let { appendDigits(it, AmountField.AMOUNT, "60") }
        assertEquals(0L, surcharge.discountAmount)
    }

    @Test
    fun `discountedAmountDraft prefills both modes`() {
        val fixed = discountedAmountDraft(30_000, 50_000, 20_000, DiscountMode.FIXED)
        assertEquals("30000", fixed.amountStr)
        assertEquals("50000", fixed.originalStr)
        assertEquals("20000", fixed.discountStr)
        assertEquals("", fixed.pctStr)
        assertTrue(fixed.hasDiscount)

        val pct = discountedAmountDraft(30_000, 50_000, 20_000, DiscountMode.PERCENTAGE)
        assertEquals("", pct.discountStr)
        assertEquals("40", pct.pctStr)
    }

    @Test
    fun `buildInput persists discount fields only when a discount is active`() {
        // plain draft
        val plain = buildInput(plainAmountDraft(50_000), 1L, " Coffee ", "  ", 1_000L)!!
        assertNull(plain.originalAmount)
        assertNull(plain.discountAmount)
        assertNull(plain.discountType)
        assertEquals(50_000L, plain.paidAmount)
        assertEquals("Coffee", plain.note)
        assertNull(plain.merchant)

        // hasDiscount but no real discount (pay >= original)
        var d = toggleDiscount(AmountDraft())
        d = editField(d, AmountField.ORIGINAL, "50")
        val noDiscount = buildInput(d, 1L, null, null, 1_000L)!!
        assertNull(noDiscount.originalAmount)
        assertNull(noDiscount.discountAmount)

        // active discount with populated original
        var d2 = toggleDiscount(AmountDraft())
        d2 = editField(d2, AmountField.ORIGINAL, "50000")
        d2 = clearField(d2, AmountField.AMOUNT)
        d2 = editField(d2, AmountField.AMOUNT, "30000")
        val active = buildInput(d2, 1L, null, null, 1_000L)!!
        assertEquals(50_000L, active.originalAmount)
        assertEquals(20_000L, active.discountAmount)
        assertEquals("fixed", active.discountType)
        assertEquals(30_000L, active.paidAmount)

        // percentage mode persists discountType percentage
        val pctDraft = discountedAmountDraft(30_000, 50_000, 20_000, DiscountMode.PERCENTAGE)
        val activePct = buildInput(pctDraft, 1L, null, null, 1_000L)!!
        assertEquals(50_000L, activePct.originalAmount)
        assertEquals(20_000L, activePct.discountAmount)
        assertEquals("percentage", activePct.discountType)

        // blockers
        assertNull(buildInput(plainAmountDraft(0), 1L, null, null, 1_000L))
        assertNull(buildInput(plainAmountDraft(50_000), null, null, null, 1_000L))
    }
}
