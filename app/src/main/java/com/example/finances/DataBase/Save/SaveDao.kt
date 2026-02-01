package com.example.finances.DataBase.Save

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SaveDao {

    @Query("SELECT * FROM Save WHERE id = 1")
    suspend fun getSave(): Save?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(save: Save)
}