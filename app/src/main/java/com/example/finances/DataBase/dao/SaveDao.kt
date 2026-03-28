package com.example.finances

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface SaveDao {

    @Query("SELECT * FROM Save WHERE id = 1")
    suspend fun getSave(): Save?


    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertOrUpdate(save: Save)
}