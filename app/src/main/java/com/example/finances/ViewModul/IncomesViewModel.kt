package com.example.finances

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IncomesViewModel(application: Application): AndroidViewModel(application) {
    private val db = DataTransactions.Companion.getDatabase(application)
    private val _transactions = MutableStateFlow<List<DateExcel>>(emptyList())
    val transactions: StateFlow<List<DateExcel>> = _transactions.asStateFlow()

    init {
        loadTransactionsFromDb()
    }

    fun loadTransactionsFromDb(){
        viewModelScope.launch {
            db.dataExcelDao().getAllIncome().collect { transactionList ->
                _transactions.value = transactionList
                Log.d("ViewModel", "Загружено из БД: ${transactionList.size} записей")

                transactionList.take(3).forEachIndexed { i, item ->
                    Log.d("ExpensesVM", "Расход $i: ${item.typeOfOperation} - ${item.amount}")
                }
            }
        }
    }
    fun addTransactionsFromExcel(fileName: String) {
        viewModelScope.launch {
            try {
                Log.d("Excel", "Начинаем загрузку $fileName")
                val context = getApplication<Application>()
                val excelTransactions = readExcelFiles(context, fileName)

                Log.d("Excel", "Прочитано из Excel: ${excelTransactions.size} записей")

                excelTransactions.forEach { transaction ->
                    db.dataExcelDao().insert(transaction)
                }
                loadTransactionsFromDb()
            }
            catch (e: Exception) {
                Log.e("Excel", "Ошибка загрузки Excel", e)
            }
        }
    }

    fun addManualExpense(amount: Double){
        viewModelScope.launch {
            try {
                Log.d("Excel", "Начинаем загрузку $amount")

                val expense = DateExcel(
                    date = "",
                    id = 0,
                    typeOfOperation = "Зачисления",
                    category = "Что то там",
                    amount = amount,
                    currency = "Руб",
                    description = "",
                    status = "Выполнено",
                    debitCardNumber = "",
                )
                db.dataExcelDao().insert(expense)
                loadTransactionsFromDb()
            }catch (e: Exception) {
                Log.e("Excel", "Ошибка загрузки Excel", e)
            }
        }
    }

    val totalAmount: StateFlow<Double> = _transactions
        .map { transactions ->
            // Трансформируем список в сумму
            transactions.sumOf { it.amount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Оптимизация
            initialValue = 0.0
        )
}