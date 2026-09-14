package com.spendr.app.kt.domain

import com.spendr.app.kt.domain.model.TransactionInput
import kotlin.math.max
import kotlin.math.min

enum class AmountField { AMOUNT, ORIGINAL, DISCOUNT, PCT }

enum class DiscountMode(val wire: String) { FIXED("fixed"), PERCENTAGE("percentage") }

/**
 * Pure state machine for the three interdependent amount fields, ported from
 * the RN app's `amountDraft.ts`. Canonical spec: docs/amount-fields.md.
 *
 * Invariant: pay = original - discount, so pay <= original, discount >= 0.
 * Priority when fields are empty: pay > original > discount.
 */
data class AmountDraft(
    val amountStr: String = "",
    val originalStr: String = "",
    val discountStr: String = "",
    val pctStr: String = "",
    val hasDiscount: Boolean = false,
    val discountMode: DiscountMode = DiscountMode.FIXED,
    val activeField: AmountField = AmountField.AMOUNT,
) {
    val paidAmount: Long get() = parseDigits(amountStr)

    /** Derived from the strings, never read back from discountStr, so it is mode-agnostic. */
    val discountAmount: Long
        get() {
            if (!hasDiscount) return 0
            val original = parseDigits(originalStr)
            val paid = parseDigits(amountStr)
            return if (original > paid) original - paid else 0
        }

    val saveEnabled: Boolean get() = paidAmount > 0
}

const val MAX_DIGITS = 12

fun parseDigits(digits: String): Long = digits.toLongOrNull() ?: 0L

private fun fieldStr(draft: AmountDraft, field: AmountField): String = when (field) {
    AmountField.AMOUNT -> draft.amountStr
    AmountField.ORIGINAL -> draft.originalStr
    AmountField.DISCOUNT -> draft.discountStr
    AmountField.PCT -> draft.pctStr
}

private fun withFieldStr(draft: AmountDraft, field: AmountField, value: String): AmountDraft =
    when (field) {
        AmountField.AMOUNT -> draft.copy(amountStr = value)
        AmountField.ORIGINAL -> draft.copy(originalStr = value)
        AmountField.DISCOUNT -> draft.copy(discountStr = value)
        AmountField.PCT -> draft.copy(pctStr = value)
    }

/**
 * Applies an edit to one field, then runs the auto-calc derivation for the
 * other two. The pct field is normalized on every write (never > 100, never "0").
 */
fun editField(draft: AmountDraft, field: AmountField, rawValue: String): AmountDraft {
    var value = rawValue
    if (field == AmountField.PCT) {
        val normalized = min(100L, parseDigits(value))
        value = if (normalized == 0L) "" else normalized.toString()
    }
    val edited = withFieldStr(draft, field, value)
    if (!edited.hasDiscount) return edited
    return derive(edited, field)
}

/** Keypad append: lone leading zero collapses; string capped at MAX_DIGITS. */
fun appendDigits(draft: AmountDraft, field: AmountField, digits: String): AmountDraft {
    val current = fieldStr(draft, field)
    if (current.length >= MAX_DIGITS) return draft
    val base = if (current == "0") "" else current
    return editField(draft, field, (base + digits).take(MAX_DIGITS))
}

fun backspace(draft: AmountDraft, field: AmountField): AmountDraft {
    val current = fieldStr(draft, field)
    if (current.isEmpty()) return draft
    return editField(draft, field, current.dropLast(1))
}

fun clearField(draft: AmountDraft, field: AmountField): AmountDraft =
    editField(draft, field, "")

/**
 * Auto-calc. The edited field is preserved; of the remaining two, original is
 * sticky when editing pay, and pay is always recomputed when editing
 * original/discount/pct.
 */
private fun derive(draft: AmountDraft, edited: AmountField): AmountDraft {
    val paid = parseDigits(draft.amountStr)
    val original = parseDigits(draft.originalStr)

    return when (edited) {
        AmountField.AMOUNT -> {
            when {
                paid == 0L -> draft
                original == 0L -> {
                    val fixedDiscount = if (draft.discountMode == DiscountMode.FIXED) {
                        parseDigits(draft.discountStr)
                    } else {
                        0L
                    }
                    draft.copy(originalStr = (paid + fixedDiscount).toString())
                }
                else -> {
                    val discount = max(0L, original - paid)
                    when (draft.discountMode) {
                        DiscountMode.FIXED -> draft.copy(
                            discountStr = if (discount > 0) discount.toString() else "",
                        )
                        DiscountMode.PERCENTAGE -> {
                            val pct = min(100L, Math.round(discount * 100.0 / original))
                            draft.copy(pctStr = if (pct > 0L) pct.toString() else "")
                        }
                    }
                }
            }
        }
        AmountField.ORIGINAL, AmountField.DISCOUNT, AmountField.PCT -> {
            when {
                original == 0L -> draft.copy(amountStr = "")
                else -> {
                    val discount = when (draft.discountMode) {
                        DiscountMode.FIXED -> parseDigits(draft.discountStr)
                        DiscountMode.PERCENTAGE ->
                            Math.round(original * min(100L, parseDigits(draft.pctStr)) / 100.0)
                    }
                    val computed = max(0L, original - discount)
                    draft.copy(amountStr = if (computed > 0) computed.toString() else "")
                }
            }
        }
    }
}

/** Turns discount mode on (pay copied into Original) or off (discount fields cleared, pay kept). */
fun toggleDiscount(draft: AmountDraft): AmountDraft =
    if (!draft.hasDiscount) {
        draft.copy(
            hasDiscount = true,
            originalStr = draft.amountStr,
            activeField = if (draft.discountMode == DiscountMode.PERCENTAGE) {
                AmountField.PCT
            } else {
                AmountField.DISCOUNT
            },
        )
    } else {
        draft.copy(
            hasDiscount = false,
            originalStr = "",
            discountStr = "",
            pctStr = "",
            activeField = AmountField.AMOUNT,
        )
    }

/** Switches fixed <-> percentage, preserving the paid amount across the switch. */
fun toggleDiscountType(draft: AmountDraft): AmountDraft {
    val original = parseDigits(draft.originalStr)
    val paid = parseDigits(draft.amountStr)
    val discount = max(0L, original - paid)
    return when (draft.discountMode) {
        DiscountMode.FIXED -> {
            val pct = if (original > 0) min(100L, Math.round(discount * 100.0 / original)) else 0L
            draft.copy(
                discountMode = DiscountMode.PERCENTAGE,
                pctStr = if (pct > 0) pct.toString() else "",
                activeField = if (draft.activeField == AmountField.DISCOUNT) AmountField.PCT else draft.activeField,
            )
        }
        DiscountMode.PERCENTAGE -> draft.copy(
            discountMode = DiscountMode.FIXED,
            discountStr = if (discount > 0) discount.toString() else "",
            activeField = if (draft.activeField == AmountField.PCT) AmountField.DISCOUNT else draft.activeField,
        )
    }
}

fun selectField(draft: AmountDraft, field: AmountField): AmountDraft =
    draft.copy(activeField = field)

/** Initial draft pre-filled with a plain amount (QuickAdd prefill). */
fun plainAmountDraft(amount: Long): AmountDraft =
    AmountDraft(amountStr = if (amount > 0) amount.toString() else "")

/** Initial draft pre-filled with a discounted transaction (edit / duplicate / full suggestion). */
fun discountedAmountDraft(
    paidAmount: Long,
    originalAmount: Long?,
    discountAmount: Long?,
    mode: DiscountMode,
): AmountDraft {
    val original = originalAmount ?: 0L
    val discount = discountAmount ?: 0L
    val pct = if (original > 0) min(100L, Math.round(discount * 100.0 / original)) else 0L
    return AmountDraft(
        amountStr = if (paidAmount > 0) paidAmount.toString() else "",
        originalStr = if (original > 0) original.toString() else "",
        discountStr = if (mode == DiscountMode.FIXED && discount > 0) discount.toString() else "",
        pctStr = if (mode == DiscountMode.PERCENTAGE && pct > 0) pct.toString() else "",
        hasDiscount = true,
        discountMode = mode,
        activeField = AmountField.ORIGINAL,
    )
}

/**
 * Builds the persistable input. Discount fields are written only when a real
 * discount > 0 is active; a plain entry stores only paidAmount.
 */
fun buildInput(
    draft: AmountDraft,
    categoryId: Long?,
    note: String?,
    merchant: String?,
    date: Long,
): TransactionInput? {
    val paid = draft.paidAmount
    val discount = draft.discountAmount
    if (paid <= 0 || categoryId == null) return null

    val discountActive = draft.hasDiscount && discount > 0
    val original = parseDigits(draft.originalStr)

    return TransactionInput(
        paidAmount = paid,
        originalAmount = if (discountActive && original > 0) original else null,
        discountAmount = if (discountActive) discount else null,
        discountType = if (discountActive) draft.discountMode.wire else null,
        categoryId = categoryId,
        note = note?.trim()?.ifEmpty { null },
        merchant = merchant?.trim()?.ifEmpty { null },
        tags = null,
        date = date,
    )
}
