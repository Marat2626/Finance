package com.example.finances

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.finances.ImportanceType

@Entity(tableName = "expense_settings")
data class ExpenseSettings(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val category: String,
    val description: String,
    val importance: ImportanceType,
    val isUserDefined: Boolean = true
)