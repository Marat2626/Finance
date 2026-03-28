package com.example.finances

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class AIViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DataTransactions.getDatabase(application)

    private val _totalBalance = MutableStateFlow(0.0)
    val totalBalance: StateFlow<Double> = _totalBalance.asStateFlow()

    private val _monthlyIncome = MutableStateFlow(0.0)
    val monthlyIncome: StateFlow<Double> = _monthlyIncome.asStateFlow()

    private val _monthlyExpenses = MutableStateFlow(0.0)
    val monthlyExpenses: StateFlow<Double> = _monthlyExpenses.asStateFlow()

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    private val _expenses = MutableStateFlow<List<DateExcel>>(emptyList())
    val expenses: StateFlow<List<DateExcel>> = _expenses.asStateFlow()

    private val _categories = MutableStateFlow<Map<String, Int>>(emptyMap())
    val categories: StateFlow<Map<String, Int>> = _categories.asStateFlow()

    init {
        loadAllData()
    }

    fun loadAllData() {
        viewModelScope.launch {
            // 1. Загружаем баланс
            val balanceEntity = db.balanceDao().getBalance()
            _totalBalance.value = balanceEntity?.amount ?: 0.0
            println("AIViewModel: Баланс = ${_totalBalance.value}")

            // 2. Загружаем цели
            db.goalDao().getAllGoals().collect { goalsList ->
                _goals.value = goalsList.map { dataGoal ->
                    Goal(
                        name = dataGoal.name,
                        targetAmount = dataGoal.amount ?: 0.0,
                        currentAmount = dataGoal.savedAmount,
                        deadline = dataGoal.createdDate?.let {
                            if (dataGoal.monthsCount > 0) {
                                it.plusMonths(dataGoal.monthsCount.toLong()).toString()
                            } else null
                        }
                    )
                }
                println("AIViewModel: Загружено целей = ${_goals.value.size}")
                _goals.value.forEach { goal ->
                    println("  Цель: ${goal.name}, нужно: ${goal.targetAmount}, накоплено: ${goal.currentAmount}")
                }
            }

            // 3. Загружаем все транзакции
            db.dataExcelDao().getAllExpenses().collect { allExpenses ->
                _expenses.value = allExpenses

                val now = LocalDateTime.now()
                val startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)

                val income = allExpenses
                    .filter { it.typeOfOperation == "Доходы" && it.date.isAfter(startOfMonth) }
                    .sumOf { it.amount }

                val expense = allExpenses
                    .filter { it.typeOfOperation == "Расходы" && it.date.isAfter(startOfMonth) }
                    .sumOf { it.amount }

                _monthlyIncome.value = income
                _monthlyExpenses.value = expense

                println("AIViewModel: Доходы за месяц = $income")
                println("AIViewModel: Расходы за месяц = $expense")

                // 4. Считаем расходы по категориям
                val categoryMap = allExpenses
                    .filter { it.typeOfOperation == "Расходы" }
                    .groupBy { it.category }
                    .mapValues { it.value.sumOf { exp -> exp.amount } }
                    .mapValues { it.value.toInt() }

                _categories.value = categoryMap
                println("AIViewModel: Категорий = ${categoryMap.size}")
            }
        }
    }

    fun refreshData() {
        loadAllData()
    }
}