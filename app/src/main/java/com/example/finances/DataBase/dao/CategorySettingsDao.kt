package com.example.finances

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.finances.CategorySettings
import com.example.finances.ImportanceType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategorySettingsDao {
    @Query("SELECT * FROM category_settings ORDER BY category")
    fun getAllSettings(): Flow<List<CategorySettings>>

    @Query("SELECT * FROM category_settings WHERE importance = :importance")
    fun getByImportance(importance: ImportanceType): Flow<List<CategorySettings>>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertOrUpdate(settings: CategorySettings)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAll(settings: List<CategorySettings>)

    @Query("DELETE FROM category_settings")
    suspend fun deleteAll()

    // В CategorySettingsDao.kt добавьте:
    @Query("SELECT * FROM category_settings")
    suspend fun getAllSync(): List<CategorySettings>
    @Query("SELECT * FROM category_settings WHERE category = :category")
    suspend fun getByCategory(category: String): CategorySettings?

    @Query("DELETE FROM category_settings WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    @Query("SELECT COUNT(*) FROM category_settings WHERE category = :category")
    suspend fun getCountByCategory(category: String): Int
}