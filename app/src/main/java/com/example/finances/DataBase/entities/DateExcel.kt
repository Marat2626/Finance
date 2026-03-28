package com.example.finances

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity
data class DateExcel (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: LocalDateTime,
    val typeOfOperation: String,
    val category: String,
    val amount: Double,
    val currency: String,
    val description: String,
    val status: String,
    val debitCardNumber: String
)