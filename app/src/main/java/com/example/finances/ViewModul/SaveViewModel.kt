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


class SaveViewModel(application: Application) : AndroidViewModel(application){

    private val db = DataTransactions.getDatabase(application)

    private val _transactions = MutableStateFlow<List<DateExcel>>(emptyList())
    val transactions: StateFlow<List<DateExcel>> = _transactions.asStateFlow()


    init {
        loadSaveFromDb()
    }
    fun loadSaveFromDb() {
        viewModelScope.launch {
           db.dataExcelDao().getAllSave().collect { saveList ->
               _transactions.value = saveList
           }
        }
    }

    fun addManualSave(amount: Double) {
        viewModelScope.launch {
            try {
                val saveEntity = DateExcel(
                    date = "",
                    id = 0,
                    typeOfOperation = "Зачисления",
                    category = "На инвестиции",
                    amount = amount,
                    currency = "Руб",
                    description = "",
                    status = "Выполнено",
                    debitCardNumber = "",
                )
                db.dataExcelDao().insert(saveEntity)
                loadSaveFromDb()
            } catch (e: Exception) {
                Log.e("save", "Ошибка загрузки save", e)
            }
        }
    }



    val totalAmount: StateFlow<Double> = _transactions
        .map { transactions ->
            transactions.sumOf { it.amount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Оптимизация
            initialValue = 0.0
        )
}