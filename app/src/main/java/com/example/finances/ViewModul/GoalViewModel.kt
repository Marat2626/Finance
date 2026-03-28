package com.example.finances

import android.app.Application
import android.util.Log

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
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
        monthsCount: Int ,
        monthlyPayment: Double = 0.0,
        createdAt: LocalDateTime = LocalDateTime.now()){

        viewModelScope.launch {

            try {
                val existingGoal = db.goalDao().getNameGoal(name)

                if (existingGoal == null) {
                    val dataGoal = DataGoal(
                        name = name,
                        amount = amount,
                        savedAmount = savedAmount,
                        monthsCount = monthsCount,
                        monthlyPayment = monthlyPayment,
                        createdDate = createdAt

                    )
                    val insertedId = db.goalDao().insert(dataGoal)
                    lastCreatedGoalId = insertedId.toInt() // сохраняем ID
                    Log.d("GoalVM", "=== NEW GOAL CREATED ===")
                    Log.d("GoalVM", "Name: $name")
                    Log.d("GoalVM", "Generated ID: $lastCreatedGoalId")
                }
                else {
                    val updateGoal = existingGoal.copy(
                        savedAmount = existingGoal.savedAmount + savedAmount
                    )

                    db.goalDao().insertOrUpdate(updateGoal)
                    lastCreatedGoalId = existingGoal.id
                    Log.d("GoalVM", "=== GOAL UPDATED ===")
                    Log.d("GoalVM", "Name: $name")
                    Log.d("GoalVM", "ID: $lastCreatedGoalId")
                }
                loadGoalsFromDb()
            }catch (e: Exception) {
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

    fun deleteGoal(goal: DataGoal){
        viewModelScope.launch {
            db.goalDao().deleteGoal(goal)
            Log.d("GoalVM", "Цель '$goal' удалена ")

        }
    }

    fun calculateMonthlySavings(goal: DataGoal): Double {
        // Проверяем, что цель существует и у нее есть сумма
        if (goal.amount == null || goal.amount <= 0) return 0.0

        // Проверяем, что указан срок
        if (goal.monthsCount <= 0) return 0.0

        // Сколько осталось накопить
        val remainingAmount = goal.amount - goal.savedAmount

        // Если уже накопили или перевыполнили план
        if (remainingAmount <= 0) return 0.0

        // Ежемесячный платеж
        return remainingAmount / goal.monthsCount
    }


}