package com.example.finances

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DataExellDao {
    @Query("SELECT * FROM DateExcel")
    fun GetAll(): Flow<List<DateExcel>>
    @Insert
    suspend fun insert(dateExcel: DateExcel)

    @Query("SELECT * FROM DateExcel WHERE typeOfOperation = 'Расходы'")
    fun getAllExpenses(): Flow<List<DateExcel>>

    @Query("SELECT * FROM DateExcel WHERE typeOfOperation = 'Зачисления'")
    fun getAllIncome(): Flow<List<DateExcel>>

    @Query("SELECT * FROM DateExcel WHERE category = 'На инвестиции'")
    fun getAllSave(): Flow<List<DateExcel>>


    @Query("SELECT SUM(amount) FROM DateExcel WHERE category = 'На инвестиции'")
    fun getTotalSave(): Flow<Double?>
    @Query("SELECT SUM(amount) FROM DateExcel WHERE typeOfOperation = 'Расходы'")
    fun getTotalExpenses(): Flow<Double?>



    @Query("SELECT SUM(amount) FROM DateExcel WHERE typeOfOperation = 'Зачисления'")
    fun getTotalIncomes(): Flow<Double?>

}