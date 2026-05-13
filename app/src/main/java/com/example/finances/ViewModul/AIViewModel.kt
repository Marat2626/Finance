package com.example.finances

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

    private val _expenses = MutableStateFlow<List<DateExcel>>(emptyList())
    val expenses: StateFlow<List<DateExcel>> = _expenses.asStateFlow()

    private val _categories = MutableStateFlow<Map<String, Int>>(emptyMap())
    val categories: StateFlow<Map<String, Int>> = _categories.asStateFlow()

    init {
        loadAllData()
    }

    fun loadAllData() {
        viewModelScope.launch {
            // 1. Баланс
            launch {
                db.balanceDao().getBalanceFlow().collectLatest { balance ->
                    _totalBalance.value = balance ?: 0.0
                    Log.d("AIViewModel", "Баланс загружен: ${_totalBalance.value}")
                }
            }

            // 2. Все транзакции
            launch {
                db.dataExcelDao().GetAll().collectLatest { allTransactions ->
                    _expenses.value = allTransactions
                    Log.d("AIViewModel", "Транзакций загружено: ${allTransactions.size}")

                    val now = LocalDateTime.now()
                    val startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)

                    // Доходы за месяц
                    val currentMonthIncome = allTransactions
                        .filter { it.typeOfOperation == "Зачисления" && it.date >= startOfMonth }
                        .sumOf { it.amount }

                    // Расходы за месяц
                    val currentMonthExpenses = allTransactions
                        .filter { it.typeOfOperation == "Расходы" && it.date >= startOfMonth }
                        .sumOf { it.amount }

                    _monthlyIncome.value = currentMonthIncome
                    _monthlyExpenses.value = currentMonthExpenses

                    Log.d("AIViewModel", "Доходы: $currentMonthIncome, Расходы: $currentMonthExpenses")

                    // Расходы по категориям
                    val categoryMap = allTransactions
                        .filter { it.typeOfOperation == "Расходы" }
                        .groupBy { it.category }
                        .mapValues { it.value.sumOf { exp -> exp.amount }.toInt() }

                    _categories.value = categoryMap
                    Log.d("AIViewModel", "Категорий: ${categoryMap.size}")
                }
            }
        }
    }

    fun refreshData() {
        loadAllData()
    }
}