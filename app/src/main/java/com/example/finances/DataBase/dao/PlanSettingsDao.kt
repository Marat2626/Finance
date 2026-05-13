package com.example.finances
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.finances.PlanSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanSettingsDao {
    @Query("SELECT * FROM plan_settings ORDER BY category")
    fun getAll(): Flow<List<PlanSettings>>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAll(settings: List<PlanSettings>)

    @Query("DELETE FROM plan_settings")
    suspend fun deleteAll()

    @Query("SELECT * FROM plan_settings WHERE category = :category")
    suspend fun getByCategory(category: String): List<PlanSettings>
}