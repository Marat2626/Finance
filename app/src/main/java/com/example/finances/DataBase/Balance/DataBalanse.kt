package com.example.finances

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.apache.poi.ss.formula.functions.Days360

@Entity(tableName = "balance")
data class Balance(
    @PrimaryKey
    val id: Int = 0,
    val amount: Double,
    val salary: Double?
)