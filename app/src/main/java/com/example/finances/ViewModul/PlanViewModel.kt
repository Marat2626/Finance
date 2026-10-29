package com.example.finances

import android.app.Application
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PlanViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DataTransactions.getDatabase(application)

    data class PlanWithSpent(
        val category: String,
        val importance: ImportanceType,
        val planned: Double,
        val spent: Double,
        val isDetailed: Boolean
    )

    private val _planItems = MutableStateFlow<List<PlanWithSpent>>(emptyList())
    val planItems: StateFlow<List<PlanWithSpent>> = _planItems.asStateFlow()




    fun loadPlanFromDb() {
        viewModelScope.launch {

            val categorySettings = db.categorySettingsDao().getAllSettings().first()
            val expenseSettings = db.expenseSettingsDao().getAllSettings().first()


            val allExpenses = db.dataExcelDao().getAllExpenses().first()
            val now = java.time.LocalDate.now()

            // Только за текущий месяц
            val currentMonthExpenses = allExpenses.filter { expense ->
                val expenseDate = expense.date.toLocalDate()
                expenseDate.month == now.month && expenseDate.year == now.year
            }



            val planSettings = db.planSettingsDao().getAll().first()

            // Собираем категории по важностям (общие)
            val essentialCategories = categorySettings
                .filter { it.importance == ImportanceType.ESSENTIAL }
                .map { it.category }.toSet()
            val neutralCategories = categorySettings
                .filter { it.importance == ImportanceType.NEUTRAL }
                .map { it.category }.toSet()
            val nonEssentialCategories = categorySettings
                .filter { it.importance == ImportanceType.NON_ESSENTIAL }
                .map { it.category }.toSet()

// Собираем детальные по важностям
            val essentialDetailed = expenseSettings
                .filter { it.importance == ImportanceType.ESSENTIAL }
                .map { "${it.category}|||${it.description}" }.toSet()
            val neutralDetailed = expenseSettings
                .filter { it.importance == ImportanceType.NEUTRAL }
                .map { "${it.category}|||${it.description}" }.toSet()
            val nonEssentialDetailed = expenseSettings
                .filter { it.importance == ImportanceType.NON_ESSENTIAL }
                .map { "${it.category}|||${it.description}" }.toSet()

// Категории, у которых есть детальные настройки
            val categoriesWithDetailed = expenseSettings.map { it.category }.toSet()

            val result = planSettings.map { plan ->
                val spent = when (plan.importance) {
                    ImportanceType.ESSENTIAL -> currentMonthExpenses.filter { expense ->
                        if (categoriesWithDetailed.contains(expense.category)) {
                            // Есть детальные — проверяем конкретное описание
                            essentialDetailed.contains("${expense.category}|||${expense.description}")
                        } else {
                            // Нет детальных — проверяем общую категорию
                            essentialCategories.contains(expense.category)
                        }
                    }.sumOf { it.amount }

                    ImportanceType.NEUTRAL -> currentMonthExpenses.filter { expense ->
                        if (categoriesWithDetailed.contains(expense.category)) {
                            neutralDetailed.contains("${expense.category}|||${expense.description}")
                        } else {
                            neutralCategories.contains(expense.category)
                        }
                    }.sumOf { it.amount }

                    ImportanceType.NON_ESSENTIAL -> currentMonthExpenses.filter { expense ->
                        if (categoriesWithDetailed.contains(expense.category)) {
                            nonEssentialDetailed.contains("${expense.category}|||${expense.description}")
                        } else {
                            nonEssentialCategories.contains(expense.category)
                        }
                    }.sumOf { it.amount }
                }

                PlanWithSpent(
                    category = plan.category,
                    importance = plan.importance,
                    planned = plan.plannedAmount,
                    spent = spent,
                    isDetailed = plan.isDetailed
                )
            }
            _planItems.value = result
            Log.d("PlanVM", "Загружено ${result.size} записей плана")
        }
    }

    fun createPlanFromDb(selections: Map<String, ImportanceType>) {
        viewModelScope.launch {
            try {
                val allExpenses = db.dataExcelDao().getAllExpenses().first()
                val allPlans = db.planSettingsDao().getAll().first()
                val goalPlans = allPlans.filter { it.category.startsWith("goal_") }

                db.planSettingsDao().deleteAll()
                if (goalPlans.isNotEmpty()) {
                    db.planSettingsDao().insertAll(goalPlans)
                }

                var essentialSum = 0.0
                var neutralSum = 0.0
                var nonEssentialSum = 0.0

                selections.forEach { (key, importance) ->
                    val sum = if (key.contains("|||")) {
                        val parts = key.split("|||")
                        allExpenses.filter { it.category == parts[0] && it.description == parts[1] }.sumOf { it.amount }
                    } else {
                        allExpenses.filter { it.category == key }.sumOf { it.amount }
                    }
                    when (importance) {
                        ImportanceType.ESSENTIAL -> essentialSum += sum
                        ImportanceType.NEUTRAL -> neutralSum += sum
                        ImportanceType.NON_ESSENTIAL -> nonEssentialSum += sum
                    }
                }

                if (essentialSum > 0) db.planSettingsDao().insertAll(listOf(PlanSettings(importance = ImportanceType.ESSENTIAL, plannedAmount = essentialSum)))
                if (neutralSum > 0) db.planSettingsDao().insertAll(listOf(PlanSettings(importance = ImportanceType.NEUTRAL, plannedAmount = neutralSum)))
                if (nonEssentialSum > 0) db.planSettingsDao().insertAll(listOf(PlanSettings(importance = ImportanceType.NON_ESSENTIAL, plannedAmount = nonEssentialSum)))
                loadPlanFromDb()
            } catch (e: Exception) {
                Log.e("PlanVM", "Ошибка", e)
            }
        }
    }
}