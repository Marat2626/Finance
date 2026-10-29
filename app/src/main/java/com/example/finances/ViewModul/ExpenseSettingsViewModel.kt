package com.example.finances

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ExpenseSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DataTransactions.getDatabase(application)

    data class ExpenseWithImportance(
        val description: String,
        val amount: Double = 0.0,
        val importance: ImportanceType? = null,
        val isUserDefined: Boolean = false
    )

    data class CategoryWithExpenses(
        val category: String,
        val expenses: List<ExpenseWithImportance>,
        val totalCount: Int
    )

    private val _categoriesWithExpenses = MutableStateFlow<List<CategoryWithExpenses>>(emptyList())
    val categoriesWithExpenses: StateFlow<List<CategoryWithExpenses>> = _categoriesWithExpenses.asStateFlow()

    private val _selectedExpenses = MutableStateFlow<Map<String, ImportanceType>>(emptyMap())
    val selectedExpenses: StateFlow<Map<String, ImportanceType>> = _selectedExpenses.asStateFlow()


    fun loadExpensesWithSettings() {
        viewModelScope.launch {
            val allExpenses = db.dataExcelDao().getAllExpenses().first()
            val savedSettings = db.expenseSettingsDao().getAllSettings().first()

            val expensesByCategory = allExpenses.groupBy { it.category }

            val result = expensesByCategory.map { (category, expenses) ->
                val uniqueDescriptions = expenses.map { it.description }.distinct()

                val expensesWithImportance = uniqueDescriptions.map { description ->
                    val saved = savedSettings.find {
                        it.category == category && it.description == description
                    }
                    val amount = expenses.filter { it.description == description }.sumOf { it.amount }
                    ExpenseWithImportance(
                        description = description,
                        amount = amount,
                        importance = saved?.importance,
                        isUserDefined = saved != null,
                    )
                }

                CategoryWithExpenses(
                    category = category,
                    expenses = expensesWithImportance,
                    totalCount = uniqueDescriptions.size
                )
            }.sortedBy { it.category }

            _categoriesWithExpenses.value = result
            Log.d("ExpenseVM", "Загружено ${result.size} категорий")
        }
    }

    /**
     * Сохраняет выбранные подкатегории.
     * При сохранении подкатегорий удаляет общую категорию из category_settings
     */
    fun saveSelectedExpenses(selections: Map<String, ImportanceType>) {
        viewModelScope.launch {
            try {
                if (selections.isEmpty()) return@launch

                // 1. Определяем категорию (берем из первого ключа)
                val category = selections.keys.first().split("|||")[0]

                // 2. Удаляем общую настройку для этой категории из category_settings
                db.categorySettingsDao().deleteByCategory(category)
                Log.d("ExpenseVM", "Удалена общая категория: $category")

                // 3. Удаляем старые детальные настройки для этой категории
                db.expenseSettingsDao().deleteByCategory(category)

                // 4. Сохраняем новые детальные настройки
                val toSave = selections.map { (key, importance) ->
                    val parts = key.split("|||")
                    ExpenseSettings(
                        category = parts[0],
                        description = parts[1],
                        importance = importance,
                        isUserDefined = true
                    )
                }

                db.expenseSettingsDao().insertAll(toSave)

                Log.d("ExpenseVM", "Сохранено ${toSave.size} подкатегорий для: $category")

                // 5. Перезагружаем данные
                loadExpensesWithSettings()

            } catch (e: Exception) {
                Log.e("ExpenseVM_SAVE", "Ошибка сохранения", e)
            }
        }
    }

//    private suspend fun calculateAndLogDetailedSums(category: String) {
//        val allExpenses = db.dataExcelDao().getAllExpenses().first()
//        val allSettings = db.expenseSettingsDao().getAllSettings().first()
//
//        val expensesInThisCategory = allExpenses.filter { it.category == category }
//
//        var sumEssential = 0.0
//        var sumNeutral = 0.0
//        var sumNonEssential = 0.0
//
////        val settingsByKey = allSettings
////            .filter { it.category == category }
////            .associateBy { getTransactionKey(it.description) }
//
//        Log.d("ExpenseVM_SAVE", "Настройки по ключам:")
//        settingsByKey.forEach { (key, setting) ->
//            Log.d("ExpenseVM_SAVE", "   $key → ${setting.importance}")
//        }
//
//        expensesInThisCategory.forEach { expense ->
//            val key = getTransactionKey(expense.description)
//            val setting = settingsByKey[key]
//
//            Log.d("ExpenseVM_SAVE_DETAIL", "Транзакция: $key → ${expense.amount} ₽, настройка: ${setting?.importance ?: "НЕТ"}")
//
//            when (setting?.importance) {
//                ImportanceType.ESSENTIAL -> sumEssential += expense.amount
//                ImportanceType.NEUTRAL -> sumNeutral += expense.amount
//                ImportanceType.NON_ESSENTIAL -> sumNonEssential += expense.amount
//                else -> { /* нет настройки */ }
//            }
//        }
//
//        Log.d("ExpenseVM_SAVE", "СУММЫ ПО ВАЖНОСТИ (только детальные настройки):")
//        Log.d("ExpenseVM_SAVE", "   Обязательные     : ${String.format("%,.0f", sumEssential)} ₽")
//        Log.d("ExpenseVM_SAVE", "   Нейтральные      : ${String.format("%,.0f", sumNeutral)} ₽")
//        Log.d("ExpenseVM_SAVE", "   Необязательные   : ${String.format("%,.0f", sumNonEssential)} ₽")
//        Log.d("ExpenseVM_SAVE", "   ───────────────────────────────")
//        Log.d("ExpenseVM_SAVE", "   ИТОГО по категории: ${String.format("%,.0f", sumEssential + sumNeutral + sumNonEssential)} ₽")
//    }
//
//    fun getImportanceForExpense(category: String, description: String): ImportanceType? {
//        return _selectedExpenses.value["$category|||$description"]
//    }
}