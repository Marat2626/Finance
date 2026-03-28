package com.example.finances

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

fun getTimePassed(createdDate: LocalDateTime): String {
    val now = LocalDateTime.now()

    val daysPassed = ChronoUnit.DAYS.between(createdDate, now)
    val weeksPassed = ChronoUnit.WEEKS.between(createdDate, now)
    val monthsPassed = ChronoUnit.MONTHS.between(createdDate, now)
    val yearsPassed = ChronoUnit.YEARS.between(createdDate, now)

    return when {
        yearsPassed > 0 -> "${yearsPassed} г. ${monthsPassed % 12} мес."
        monthsPassed > 0 -> {
            val remainingDays = daysPassed - (monthsPassed * 30)
            "${monthsPassed} мес. ${remainingDays} дн."
        }
        weeksPassed > 0 -> {
            val remainingDays = daysPassed - (weeksPassed * 7)
            "${weeksPassed} нед. ${remainingDays} дн."
        }
        daysPassed > 0 -> "${daysPassed} дн."
        daysPassed == 0L -> "Сегодня"
        else -> "Только что"
    }
}

fun getMonthsPassed(createdDate: LocalDateTime): Int {
    return ChronoUnit.MONTHS.between(createdDate, LocalDateTime.now()).toInt()
}

fun getDaysPassed(createdDate: LocalDateTime): Long {
    return ChronoUnit.DAYS.between(createdDate, LocalDateTime.now())
}

fun getRemainingTime(createdDate: LocalDateTime, totalMonths: Int): String {
    val monthsPassed = getMonthsPassed(createdDate)
    val remainingMonths = (totalMonths - monthsPassed).coerceAtLeast(0)

    return when {
        remainingMonths > 12 -> {
            val years = remainingMonths / 12
            val months = remainingMonths % 12
            "${years} г. ${months} мес."
        }
        remainingMonths > 0 -> "${remainingMonths} мес."
        remainingMonths == 0 -> "Последний месяц"
        else -> "Срок истек"
    }
}