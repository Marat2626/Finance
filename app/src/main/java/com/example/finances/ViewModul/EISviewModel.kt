package com.example.finances

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

open class EISviewModel(
    application: Application,
    protected val daoMethod: suspend (DataExellDao) -> Flow<List<DateExcel>>,
    protected val operationType: String,
) : AndroidViewModel(application) {


    protected val db = DataTransactions.Companion.getDatabase(application)
    private val _transactions = MutableStateFlow<List<DateExcel>>(emptyList())
    val transactions: StateFlow<List<DateExcel>> = _transactions.asStateFlow()


    private val balanceDao = db.balanceDao()

    init {
        viewModelScope.launch {
            daoMethod(db.dataExcelDao()).collect { list ->
                _transactions.value = list
                Log.d("EISvm", "Загружено ${list.size} записей типа $operationType")
            }
        }
    }

    fun getCategory ():Flow<List<DateExcel>> {

        return db.dataExcelDao().GetAll()

    }

    fun addManualSave(amount: Double, category: String, description: String ) {
        viewModelScope.launch {
            Log.d("DEBUG", "=== addManualSave ===")
            Log.d("DEBUG", "operationType: $operationType")
            Log.d("DEBUG", "category: $category")
            Log.d("DEBUG", "amount: $amount")
            try {
                Log.d("Excel", "Начинаем загрузку $amount")
                val saveEntity = DateExcel(
                    date = LocalDateTime.now(),
                    typeOfOperation = operationType,
                    category = category,
                    amount = amount,
                    currency = "Руб",
                    description = description,
                    status = "Выполнено",
                    debitCardNumber = "",
                )
                Log.d("DEBUG", "Сохраняю: typeOfOperation=${saveEntity.typeOfOperation} $operationType")
                db.dataExcelDao().insert(saveEntity)


                val currentBalance = db.balanceDao().getBalance() ?: 0.0

                val newBalance = when (operationType) {
                    "Зачисления" -> {
                        val updated = currentBalance + amount
                        Log.d("BALANCE", "➕ Доход: $currentBalance + $amount = $updated")
                        updated
                    }
                    "Расходы" -> {
                        val updated = currentBalance - amount
                        Log.d("BALANCE", "➖ Расход: $currentBalance - $amount = $updated")
                        updated
                    }
                    "Накопление" -> {
                        val updated = currentBalance - amount
                        Log.d("BALANCE", "🏦 Накопление: $currentBalance - $amount = $updated")
                        updated
                    }
                    else -> currentBalance
                }

                db.balanceDao().saveBalance(Balance(id = 1, currentBalance = newBalance))


            } catch (e: Exception) {
                Log.e("save", "Ошибка загрузки save", e)
            }
        }
    }
    fun loadData() {
        viewModelScope.launch {
            daoMethod(db.dataExcelDao()).collect { list ->
                _transactions.value = list
            }
        }
    }

    // Добавьте в EISviewModel.kt
    fun addTransactionsFromUri(context: Context, uri: Uri) {


        viewModelScope.launch {
            try {
                Log.d("Excel", "Начинаем загрузку из URI")
                val inputStream = context.contentResolver.openInputStream(uri)

                if (inputStream != null) {
                    // Используем существующий метод парсинга из Table_XML.kt
                    val excelTransactions = readExcelFromStream(inputStream, uri.toString())


                    Log.d("Excel", "📊 ИМПОРТ ДАННЫХ ИЗ EXCEL")

                    Log.d("Excel", "Всего транзакций: ${excelTransactions.size}")


                    val expensesSum = excelTransactions.filter { it.typeOfOperation == "Расходы" }.sumOf { it.amount }
                    val incomesSum = excelTransactions.filter { it.typeOfOperation == "Зачисления" }.sumOf { it.amount }
                    val savingsSum = excelTransactions.filter { it.typeOfOperation == "Накопление" }.sumOf { it.amount }

                    Log.d("Excel", "")
                    Log.d("Excel", "💰 СУММЫ ПО ТИПАМ:")
                    Log.d("Excel", "   • Расходы:     ${String.format("%,.2f", expensesSum)} ₽")
                    Log.d("Excel", "   • Доходы:      ${String.format("%,.2f", incomesSum)} ₽")
                    Log.d("Excel", "   • Накопления:  ${String.format("%,.2f", savingsSum)} ₽")
                    Log.d("Excel", "   • ИТОГО:       ${String.format("%,.2f", expensesSum + incomesSum + savingsSum)} ₽")


                    val minDate = excelTransactions.minByOrNull { it.date }?.date
                    val maxDate = excelTransactions.maxByOrNull { it.date }?.date
                    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

                    Log.d("Excel", "")
                    Log.d("Excel", "📅 ДИАПАЗОН ДАТ:")
                    Log.d("Excel", "   • С: ${minDate?.format(dateFormatter) ?: "нет данных"}")
                    Log.d("Excel", "   • По: ${maxDate?.format(dateFormatter) ?: "нет данных"}")



                    var importedIncomes = 0.0
                    var importedExpenses = 0.0

                    excelTransactions.forEach { transaction ->
                        db.dataExcelDao().insert(transaction)

                        when (transaction.typeOfOperation) {
                            "Зачисления" -> importedIncomes += transaction.amount
                            "Расходы" -> importedExpenses += transaction.amount
                        }



                        // Если это накопление - дублируем как Накопление
                        if (transaction.typeOfOperation == "Расходы" && transaction.category == "На инвестиции") {
                            val savingTransaction = transaction.copy(
                                typeOfOperation = "Накопление"
                            )
                            db.dataExcelDao().insert(savingTransaction)
                        }
                    }


                    updateBalanceAfterImport(importedIncomes, importedExpenses)

                    Log.d("Excel", "Импортировано ${excelTransactions.size} записей из URI")
                    inputStream.close()



                } else {
                    Log.e("Excel", "Не удалось открыть файл")
                }

            } catch (e: Exception) {
                Log.e("Excel", "Ошибка загрузки Excel из URI", e)
            }
        }
    }

    private fun updateBalanceAfterImport(incomes: Double, expenses: Double) {
        viewModelScope.launch {
            try {
                val currentBalance = balanceDao.getBalance() ?: 0.0
                val newBalance = currentBalance + incomes - expenses
                balanceDao.saveBalance(Balance(id = 1, currentBalance = newBalance))
                Log.d("BALANCE", "📥 Импорт: $currentBalance + $incomes - $expenses = $newBalance")
            } catch (e: Exception) {
                Log.e("EISviewModel", "Ошибка обновления баланса", e)
            }
        }
    }

}

