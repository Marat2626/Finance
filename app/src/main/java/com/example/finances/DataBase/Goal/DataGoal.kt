package com.example.finances

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DataGoal (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val amount: Double?
)
