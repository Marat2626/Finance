package com.example.finances

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.finances.Balance

@Dao
interface BalanceDao {
    @Query("SELECT * FROM balance WHERE id = 1")
    suspend fun getBalance(): Balance?

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertOrUpdate(balance: Balance)

    @Insert
    suspend fun insert(database: Balance)
}