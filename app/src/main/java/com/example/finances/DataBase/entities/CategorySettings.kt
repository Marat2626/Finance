
package com.example.finances

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_settings")
data class CategorySettings(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val category: String,
    val importance: ImportanceType, // ESSENTIAL, NEUTRAL, NON_ESSENTIAL
    val isUserDefined: Boolean = false // пользователь сам отметил или системная
)

enum class ImportanceType {
    ESSENTIAL,      // Обязательные траты (квартира, коммуналка, еда)
    NEUTRAL,        // Нейтральные (учеба, здоровье, транспорт)
    NON_ESSENTIAL   // Необязательные (кафе, развлечения, фастфуд)
}