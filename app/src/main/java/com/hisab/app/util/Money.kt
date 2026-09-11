package com.hisab.app.util

/** Formats poisha (1 taka = 100 poisha) as a whole-taka amount with South-Asian digit
 * grouping, e.g. 44875000 -> "৳4,48,750", matching the design mockup's number style. */
object Money {
    fun format(minor: Long): String {
        val negative = minor < 0
        val taka = kotlin.math.abs(minor) / 100
        val digits = taka.toString()
        val grouped = if (digits.length <= 3) {
            digits
        } else {
            val lastThree = digits.substring(digits.length - 3)
            var remaining = digits.substring(0, digits.length - 3)
            val groups = mutableListOf<String>()
            while (remaining.length > 2) {
                groups.add(0, remaining.substring(remaining.length - 2))
                remaining = remaining.substring(0, remaining.length - 2)
            }
            if (remaining.isNotEmpty()) groups.add(0, remaining)
            groups.joinToString(",") + "," + lastThree
        }
        return (if (negative) "-৳" else "৳") + grouped
    }

    fun parseTakaInputToMinor(input: String): Long? {
        val cleaned = input.replace(",", "").trim()
        val taka = cleaned.toDoubleOrNull() ?: return null
        return Math.round(taka * 100)
    }
}
