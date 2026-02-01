package com.example.finances.DataBase.Save

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Save")
data class Save(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val saver: Double
)