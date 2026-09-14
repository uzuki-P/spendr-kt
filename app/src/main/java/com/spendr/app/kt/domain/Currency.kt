package com.spendr.app.kt.domain

import java.util.Locale

/** Dot-grouped integer formatting, deterministic (no locale lookup), port of RN `formatAmount`. */
fun formatAmount(amount: Long): String {
    val negative = amount < 0
    val digits = if (negative) (-amount).toString() else amount.toString()
    val grouped = buildString {
        for ((index, c) in digits.withIndex()) {
            if (index > 0 && (digits.length - index) % 3 == 0) append('.')
            append(c)
        }
    }
    return if (negative) "-$grouped" else grouped
}

fun formatRupiah(amount: Long): String =
    (if (amount < 0) "-" else "") + "Rp " + formatAmount(if (amount < 0) -amount else amount)

/** "Rp 2,50 jt" / "Rp 150rb" / full, port of RN `formatRupiahCompact`. */
fun formatRupiahCompact(amount: Long): String {
    val abs = if (amount < 0) -amount else amount
    return when {
        abs >= 1_000_000 -> {
            val jt = String.format(Locale.US, "%.2f", abs / 1_000_000.0).replace('.', ',')
            "Rp ${jt}jt"
        }
        abs >= 100_000 -> "Rp ${abs / 1000}rb"
        else -> formatRupiah(abs)
    }
}
