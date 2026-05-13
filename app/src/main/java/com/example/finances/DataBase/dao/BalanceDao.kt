package com.example.finances

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.finances.Balance
import kotlinx.coroutines.flow.Flow

@Dao
interface BalanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBalance(balance: Balance)

    @Query("SELECT currentBalance FROM balance WHERE id = 1")
    suspend fun getBalance(): Double?

    @Query("SELECT currentBalance FROM balance WHERE id = 1")
    fun getBalanceFlow(): Flow<Double?>

    @Query("UPDATE balance SET currentBalance = :balance WHERE id = 1")
    suspend fun updateBalance(balance: Double)

    @Query("DELETE FROM balance")
    suspend fun deleteAll()
}