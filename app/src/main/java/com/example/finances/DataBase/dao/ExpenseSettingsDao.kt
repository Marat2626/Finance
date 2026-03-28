// ExpenseSettingsDao.kt
package com.example.finances

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseSettingsDao {
    @Query("SELECT * FROM expense_settings ORDER BY category, description")
    fun getAllSettings(): Flow<List<ExpenseSettings>>

    @Query("SELECT * FROM expense_settings WHERE category = :category")
    fun getByCategory(category: String): Flow<List<ExpenseSettings>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: ExpenseSettings)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(settings: List<ExpenseSettings>)

    @Query("DELETE FROM expense_settings")
    suspend fun deleteAll()

    @Query("DELETE FROM expense_settings WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    @Query("SELECT * FROM expense_settings WHERE category = :category AND description = :description")
    suspend fun getByCategoryAndDescription(category: String, description: String): ExpenseSettings?
}