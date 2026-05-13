package com.example.finances

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.finances.DateExcel
import kotlinx.coroutines.flow.Flow

@Dao
interface DataExellDao {

    @Query("DELETE FROM DateExcel")
    suspend fun deleteAll()
    @Query("SELECT * FROM DateExcel")
    fun GetAll(): Flow<List<DateExcel>>
    @Insert
    suspend fun insert(dateExcel: DateExcel)

    @Query("SELECT * FROM DateExcel WHERE typeOfOperation = 'Расходы'")
    fun getAllExpenses(): Flow<List<DateExcel>>

    @Query("SELECT * FROM DateExcel WHERE typeOfOperation = 'Зачисления'")
    fun getAllIncome(): Flow<List<DateExcel>>

    @Query("SELECT * FROM DateExcel WHERE typeOfOperation = 'Накопление'")
    fun getAllSave(): Flow<List<DateExcel>>


    @Query("SELECT SUM(amount) FROM DateExcel WHERE typeOfOperation = 'Накопление'")
    fun getTotalSave(): Flow<Double?>
    @Query("SELECT SUM(amount) FROM DateExcel WHERE typeOfOperation = 'Расходы'")
    fun getTotalExpenses(): Flow<Double?>



    @Query("SELECT * FROM DateExcel WHERE typeOfOperation = 'Расходы'")
    suspend fun getAllExpensesList(): List<DateExcel>




    @Query("SELECT SUM(amount) FROM DateExcel WHERE typeOfOperation = 'Зачисления'")
    fun getTotalIncomes(): Flow<Double?>


    @Query("SELECT COALESCE(SUM(amount), 0) FROM DateExcel WHERE typeOfOperation = 'Зачисления'")
    suspend fun getTotalIncomesSum(): Double

    @Query("SELECT COALESCE(SUM(amount), 0) FROM DateExcel WHERE typeOfOperation = 'Расходы'")
    suspend fun getTotalExpensesSum(): Double
    @Query("""
        SELECT COALESCE(
            (SELECT SUM(amount) FROM DateExcel WHERE typeOfOperation = 'Зачисления'), 0
        ) - COALESCE(
            (SELECT SUM(amount) FROM DateExcel WHERE typeOfOperation = 'Расходы'), 0
        )
    """)
    fun getCalculatedBalance(): Flow<Double>

}