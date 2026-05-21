package com.example.finances

import android.app.Application
import android.util.Log

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime


class GoalViewModel(application: Application) : AndroidViewModel(application){

    private val db = DataTransactions.getDatabase(application)
    private val _transactions = MutableStateFlow<List<DataGoal>>(emptyList())
    val transactions : StateFlow<List<DataGoal>> = _transactions.asStateFlow()

    private var lastCreatedGoalId: Int = 0

    init {
        loadGoalsFromDb()
    }
    fun loadGoalsFromDb() {
        viewModelScope.launch {
            db.goalDao().getAllGoals().collect {goalsList ->
                _transactions.value = goalsList
                Log.d("GoalVM", "Загружено целей: ${goalsList.size}")
                Log.d("GoalVM", "ID целей: ${goalsList.map { it.id }}")
                Log.d("GoalVM", "Названия целей: ${goalsList.map { it.name }}")
            }
        }
    }

    fun saveGoal(
        name: String,
        amount: Double?,
        savedAmount: Double = 0.0,
        monthsCount: Int,
        monthlyPayment: Double = 0.0,
        createdAt: LocalDateTime = LocalDateTime.now(),
        category: String = ""
    ) {
        viewModelScope.launch {
            try {
                val existingGoal = db.goalDao().getNameGoal(name)
                var goalId: Int

                if (existingGoal == null) {
                    val dataGoal = DataGoal(
                        name = name,
                        amount = amount,
                        savedAmount = savedAmount,
                        monthsCount = monthsCount,
                        monthlyPayment = monthlyPayment,
                        createdDate = createdAt,
                        category = category
                    )
                    goalId = db.goalDao().insert(dataGoal).toInt()
                    lastCreatedGoalId = goalId
                    Log.d("GoalVM", "=== NEW GOAL CREATED ===")
                    Log.d("GoalVM", "Name: $name, ID: $goalId")
                } else {
                    val updateGoal = existingGoal.copy(
                        savedAmount = existingGoal.savedAmount + savedAmount
                    )
                    db.goalDao().insertOrUpdate(updateGoal)
                    goalId = existingGoal.id
                    lastCreatedGoalId = goalId
                    Log.d("GoalVM", "=== GOAL UPDATED ===")
                    Log.d("GoalVM", "Name: $name, ID: $goalId")
                }

                if (monthlyPayment > 0) {
                    val planSettings = PlanSettings(
                        category = "goal_$goalId",  // ← вернуть goal_id
                        importance = ImportanceType.ESSENTIAL,
                        plannedAmount = monthlyPayment,
                        isDetailed = false
                    )
                    db.planSettingsDao().insertAll(listOf(planSettings))
                    Log.d("GoalVM", "✅ Цель добавлена в план: goal_$goalId = $monthlyPayment ₽")
                }

                loadGoalsFromDb()
            } catch (e: Exception) {
                Log.e("Goal", "Ошибка загрузки Goal", e)
            }
        }
    }
    fun getLastCreatedGoalId(): Int {
        return lastCreatedGoalId
    }

    fun updateAmount(category: String, amount: Double?) {

        viewModelScope.launch {
            Log.d("GoalVM", "Ищем цель с названием: '$category'")
            val existingGoal = db.goalDao().getNameGoal(category)
            Log.d("GoalVM", "Найдено: ${existingGoal != null}")

            if (existingGoal != null){
                val updateGoal = existingGoal.copy(
                    savedAmount = existingGoal.savedAmount + (amount ?: 0.0)
                )
                db.goalDao().insertOrUpdate(updateGoal)

                loadGoalsFromDb()
                Log.d("GoalVM", "накопление обновленно '$category' ")
            }
            else{
                Log.d("GoalVM", "Цель '$category' не найдена, накопление не привязано")
            }
        }
    }

    fun deleteGoal(goal: DataGoal) {
        viewModelScope.launch {
            val allPlans = db.planSettingsDao().getAll().first()
            val remaining = allPlans.filter { it.category != "goal_${goal.id}" }
            db.planSettingsDao().deleteAll()
            if (remaining.isNotEmpty()) {
                db.planSettingsDao().insertAll(remaining)
            }
            db.goalDao().deleteGoal(goal)
        }
    }

}