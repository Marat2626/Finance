// PlanSettings.kt - новая Entity
package com.example.finances

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plan_settings")
data class PlanSettings(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val importance: ImportanceType,
    val category: String = "",
    val plannedAmount: Double,   // план по сумме
    val isDetailed: Boolean = false  // true если детальная настройка
)