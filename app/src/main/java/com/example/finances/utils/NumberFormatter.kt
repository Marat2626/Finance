// utils/NumberFormatter.kt
package com.example.finances

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/**
 * Форматирует Double в строку с пробелом как разделитель тысяч
 * Пример: 100000 -> "100 000"
 */
fun Double.formatWithSpace(): String {
    val formatter = DecimalFormat("#,###").apply {
        decimalFormatSymbols = DecimalFormatSymbols().apply {
            groupingSeparator = ' '
        }
        maximumFractionDigits = 0
    }
    return formatter.format(this)
}

/**
 * Форматирует Double в строку с пробелом (с копейками, если нужно)
 */
fun Double.formatWithSpaceFull(): String {
    val formatter = DecimalFormat("#,###.##").apply {
        decimalFormatSymbols = DecimalFormatSymbols().apply {
            groupingSeparator = ' '
        }
    }
    return formatter.format(this)
}

/**
 * Форматирует Int в строку с пробелом как разделитель тысяч
 * Пример: 100000 -> "100 000"
 */
fun Int.formatWithSpace(): String {
    val formatter = DecimalFormat("#,###").apply {
        decimalFormatSymbols = DecimalFormatSymbols().apply {
            groupingSeparator = ' '
        }
    }
    return formatter.format(this)
}

/**
 * Форматирует Long в строку с пробелом как разделитель тысяч
 */
fun Long.formatWithSpace(): String {
    val formatter = DecimalFormat("#,###").apply {
        decimalFormatSymbols = DecimalFormatSymbols().apply {
            groupingSeparator = ' '
        }
    }
    return formatter.format(this)
}