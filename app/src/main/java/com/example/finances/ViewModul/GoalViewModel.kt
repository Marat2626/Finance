package com.example.finances

import android.app.Application
import android.util.Log

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class GoalViewModel(application: Application) : AndroidViewModel(application){

    private val db = DataTransactions.getDatabase(application)
    private val _transactions = MutableStateFlow<List<DataGoal>>(emptyList())
    val transactions : StateFlow<List<DataGoal>> = _transactions.asStateFlow()


    init {
        loadGoalsFromDb()
    }

    fun loadGoalsFromDb() {
        viewModelScope.launch {
            db.goalDao().getAllGoals().collect {goalsList ->
                _transactions.value = goalsList
                Log.d("GoalVM", "Загружено целей: ${goalsList.size}")
            }
        }
    }

    fun saveGoal(name: String, amount: Double?){

        viewModelScope.launch {

            try {
                val dataGoal = DataGoal(
                    id = 0,
                    name = name,
                    amount = amount
                )
                db.goalDao().insert(dataGoal)
                Log.e("Goal", "Загружено Goal")
            }catch (e: Exception) {
                Log.e("Goal", "Ошибка загрузки Goal", e)
            }

        }
    }

}