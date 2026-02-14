package com.example.finances

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.finances.DataTransactions
import com.example.finances.DateExcel
import com.example.finances.readExcelFiles
import com.example.finances.twoReadExcelFiles
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

open class EISviewModel(
    application: Application,
    protected val daoMethod: suspend (DataExellDao) -> Flow<List<DateExcel>>,
    protected val operationType: String
) : AndroidViewModel(application) {

    protected val db = DataTransactions.Companion.getDatabase(application)
    private val _transactions = MutableStateFlow<List<DateExcel>>(emptyList())
    val transactions: StateFlow<List<DateExcel>> = _transactions.asStateFlow()


    init {
        viewModelScope.launch {
            daoMethod(db.dataExcelDao()).collect { list ->
                _transactions.value = list
                Log.d("EISvm", "Загружено ${list.size} записей типа $operationType")
            }
        }
    }


    fun addManualSave(amount: Double, category: String, ) {
        viewModelScope.launch {
            Log.d("DEBUG", "=== addManualSave ===")
            Log.d("DEBUG", "operationType: $operationType")
            Log.d("DEBUG", "category: $category")
            Log.d("DEBUG", "amount: $amount")
            try {
                Log.d("Excel", "Начинаем загрузку $amount")
                val saveEntity = DateExcel(
                    date = "",
                    id = 0,
                    typeOfOperation = operationType,
                    category = category,
                    amount = amount,
                    currency = "Руб",
                    description = "",
                    status = "Выполнено",
                    debitCardNumber = "",
                )
                Log.d("DEBUG", "Сохраняю: typeOfOperation=${saveEntity.typeOfOperation}")
                db.dataExcelDao().insert(saveEntity)
            } catch (e: Exception) {
                Log.e("save", "Ошибка загрузки save", e)
            }
        }
    }

    fun addTransactionsFromExcel(fileName1: String, fileName2: String) {
        viewModelScope.launch {
            try {
                Log.d("Excel", "Начинаем загрузку $fileName1 и $fileName2")
                val context = getApplication<Application>()
                val excelTransactions = twoReadExcelFiles(context, fileName1, fileName2)

                excelTransactions.forEach { transaction ->
                    // Сохраняем оригинал
                    db.dataExcelDao().insert(transaction)

                    // Если это накопление - дублируем как Накопление
                    if (transaction.typeOfOperation == "Расходы" && transaction.category == "На инвестиции") {
                        val savingTransaction = transaction.copy(
                            typeOfOperation = "Накопление"
                        )
                        db.dataExcelDao().insert(savingTransaction)
                    }
                }

                Log.d("Excel", "Импортировано ${excelTransactions.size} записей")

            } catch (e: Exception) {
                Log.e("Excel", "Ошибка загрузки Excel", e)
            }
        }
    }
}
//
//    val totalAmount: StateFlow<Double> = _transactions
//
//        .map { transactions ->
//            transactions
//                .filter { it.typeOfOperation == operationType }
//                .sumOf { it.amount }
//
//        }
//        .stateIn(
//            scope = viewModelScope,
//            started = SharingStarted.WhileSubscribed(5000), // Оптимизация
//            initialValue = 0.0
//        )
//
//
//
//}