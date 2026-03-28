package com.example.finances

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "DataGoal",
    indices = [Index(value = ["name"], unique = true)]
)
data class DataGoal (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "name")
    val name: String,
    val amount: Double?,
    var savedAmount: Double = 0.0,
    val monthsCount: Int,
    val monthlyPayment: Double = 0.0,
    val createdDate: LocalDateTime = LocalDateTime.now()
)