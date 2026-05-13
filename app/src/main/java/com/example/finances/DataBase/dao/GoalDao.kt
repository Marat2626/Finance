package com.example.finances

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {


    @Insert
    suspend fun insert(dateGoal: DataGoal): Long
//    @Insert
//    suspend fun insert(dateGoal: DataGoal)

    @Query("DELETE FROM DataGoal")
    suspend fun deleteAll()

    @Query("SELECT * FROM DataGoal ORDER BY id DESC")
    fun getAllGoals(): Flow<List<DataGoal>>

    @Query("SELECT * FROM Datagoal WHERE id = 1")
    suspend fun getGoals(): DataGoal?


    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertOrUpdate(goal: DataGoal)

    @Query("SELECT * FROM DataGoal WHERE name = :goal")
    suspend fun getNameGoal(goal: String): DataGoal?

    @Delete
    suspend fun deleteGoal(goal: DataGoal)
}