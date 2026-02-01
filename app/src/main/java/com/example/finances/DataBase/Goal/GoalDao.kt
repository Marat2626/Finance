package com.example.finances

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Dao
interface GoalDao {

    @Insert
    suspend fun insert(dateGoal: DataGoal)

    @Query("SELECT * FROM DataGoal ORDER BY id DESC")
    fun getAllGoals(): Flow<List<DataGoal>>

    @Query("SELECT * FROM Datagoal WHERE id = 1")
    suspend fun getGoals(): DataGoal?
}