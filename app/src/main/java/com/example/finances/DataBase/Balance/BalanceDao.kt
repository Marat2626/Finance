package com.example.finances


import androidx.room.*

@Dao
interface BalanceDao {
    @Query("SELECT * FROM balance WHERE id = 1")
    suspend fun getBalance(): Balance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(balance: Balance)

    @Insert
    suspend fun insert(database: Balance)
}