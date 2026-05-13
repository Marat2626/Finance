package com.example.finances

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "balance")
data class Balance(
    @PrimaryKey val id: Int = 1,
    val currentBalance: Double = 0.0
)