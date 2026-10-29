package com.example.finances

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class MlGoalPredictor(context: Context) {

    private val forest = RandomForestPort(context)
    private val scaleFactor = 50.0
    private val forecastCache: MutableMap<String, List<ForecastPoint>> = mutableMapOf()

    init {
        loadForecastData(context)
    }

    // ========== СТРУКТУРЫ ДАННЫХ ==========

    data class MlPredictionResult(
        val probability: Int,
        val title: String,
        val emoji: String,
        val color: androidx.compose.ui.graphics.Color,
        val advice: String
    )

    data class MlInflationResult(
        val futureAmount: Double,
        val growthPercent: String,
        val months: Int,
        val recommendedMonthly: Double,
        val onAccept: () -> Unit
    )

    data class ForecastPoint(
        val date: String,
        val trend: Double,
        val yearly: Double,
        val yhat: Double,
        val yhatLower: Double,
        val yhatUpper: Double
    )

    // ========== ЗАГРУЗКА JSON ==========

    private fun loadForecastData(context: Context) {
        try {
            val inputStream = context.assets.open("forecast_data.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.readText()
            reader.close()

            val json = JSONObject(jsonString)
            val categories = listOf("Одежда", "Квартира", "Мебель", "Машина", "Телефон", "Отпуск")

            for (cat in categories) {
                val points = mutableListOf<ForecastPoint>()
                val arr = json.optJSONArray(cat)
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        points.add(
                            ForecastPoint(
                                date = obj.getString("date"),
                                trend = obj.getDouble("trend"),
                                yearly = obj.getDouble("yearly"),
                                yhat = obj.getDouble("yhat"),
                                yhatLower = obj.getDouble("yhat_lower"),
                                yhatUpper = obj.getDouble("yhat_upper")
                            )
                        )
                    }
                }
                forecastCache[cat] = points
            }
            Log.d("ML_INFLATION", "Загружено категорий: ${forecastCache.size}")
        } catch (e: Exception) {
            Log.e("ML_INFLATION", "Ошибка загрузки forecast_data.json: ${e.message}")
        }
    }

    // ========== ПРОГНОЗ ИНФЛЯЦИИ ==========

    fun predictInflation(
        category: String,
        currentAmount: Double,
        months: Int = 12,
        onAcceptInflation: (StrategyRecommendation) -> Unit  // ← callback для создания цели
    ): MlInflationResult {

        if (category.isBlank()) {
            return MlInflationResult(
                futureAmount = currentAmount,
                growthPercent = "0.0",
                months = months,
                recommendedMonthly = currentAmount / months,
                onAccept = {}
            )
        }

        val points = forecastCache[category]

        if (points == null || points.size < 2) {
            return MlInflationResult(
                futureAmount = currentAmount,
                growthPercent = "0.0",
                months = months,
                recommendedMonthly = currentAmount / months,
                onAccept = {}
            )
        }

        val historySize = 12.coerceAtMost(points.size)
        val currentIdx = points[historySize - 1].yhat
        val targetIndex = (historySize + months - 1).coerceIn(0, points.size - 1)
        val futureIdx = points[targetIndex].yhat

        val growth = futureIdx / currentIdx
        val futureAmount = currentAmount * growth
        val recommendedMonthly = futureAmount / months

        return MlInflationResult(
            futureAmount = futureAmount,
            growthPercent = "%.1f".format((growth - 1) * 100),
            months = months,
            recommendedMonthly = recommendedMonthly,
            onAccept = {
                onAcceptInflation(
                    StrategyRecommendation(
                        id = R.drawable.ves4,
                        name = "С учётом инфляции",
                        description = "ML-прогноз Prophet",
                        months = months,
                        monthlyPayment = recommendedMonthly,
                        goalAmount = futureAmount  // ← передаём ML-сумму
                    )
                )
            }
        )
    }

    // ========== СТАРЫЙ МЕТОД (RandomForest) ==========

    private fun getImportance(
        expense: DateExcel,
        categorySettings: List<CategorySettings>,
        expenseSettings: List<ExpenseSettings>
    ): String? {
        val expSetting = expenseSettings.find {
            it.category == expense.category && it.description == expense.description
        }
        if (expSetting != null) return expSetting.importance.name
        val catSetting = categorySettings.find { it.category == expense.category }
        if (catSetting != null) return catSetting.importance.name
        return null
    }

    suspend fun predict(
        db: DataTransactions,
        goal: DataGoal
    ): MlPredictionResult {
        // ... существующий код без изменений ...
        val expenses = db.dataExcelDao().getAllExpenses().first()
        val incomes = db.dataExcelDao().getAllIncome().first()
        val savings = db.dataExcelDao().getAllSave().first()

        val sixMonthsAgo = java.time.LocalDate.now().minusMonths(6)
        val recentIncomes = incomes.filter { it.date.toLocalDate().isAfter(sixMonthsAgo) }
        val monthsCount = recentIncomes.map { it.date.toLocalDate().withDayOfMonth(1) }
            .distinct().size.coerceAtLeast(1)
        val avgIncomeRub = recentIncomes.sumOf { it.amount } / monthsCount

        val recentExpenses = expenses.filter { it.date.toLocalDate().isAfter(sixMonthsAgo) }
        val avgExpensesRub = recentExpenses.sumOf { it.amount } / monthsCount

        val categorySettings = db.categorySettingsDao().getAllSync()
        val expenseSettings = db.expenseSettingsDao().getAllSync()

        var essentialSum = 0.0
        var nonEssentialSum = 0.0

        for (expense in recentExpenses) {
            val importance = getImportance(expense, categorySettings, expenseSettings)
            when (importance) {
                "ESSENTIAL" -> essentialSum += expense.amount
                "NON_ESSENTIAL", null -> nonEssentialSum += expense.amount
            }
        }

        val essentialMonthly = essentialSum / monthsCount
        val freeMoneyRub = avgIncomeRub - essentialMonthly
        val totalSavingsRub = savings.sumOf { it.amount }

        val avgIncomeScaled = avgIncomeRub / scaleFactor
        val avgExpensesScaled = avgExpensesRub / scaleFactor
        val totalSavingsScaled = totalSavingsRub / scaleFactor

        val hasLoan = if (expenses.any { it.category.contains("Кредит", ignoreCase = true) }) 1f else 0f

        val creditScoreEstimate = when {
            freeMoneyRub > 50000 -> 750f
            freeMoneyRub > 20000 -> 650f
            freeMoneyRub > 5000 -> 550f
            else -> 400f
        }

        val inputArray = floatArrayOf(
            35f,
            avgIncomeScaled.toFloat(),
            avgExpensesScaled.toFloat(),
            totalSavingsScaled.toFloat(),
            2f, 1f, hasLoan, creditScoreEstimate
        )

        val mlProbability = (forest.predictProbability(inputArray) * 100).toInt()

        val monthlyNeedRub = (goal.amount ?: 0.0) / goal.monthsCount.coerceAtLeast(1)
        var finalProb = mlProbability.coerceIn(0, 100)

        if (monthlyNeedRub > 0 && freeMoneyRub > 0) {
            val ratio = (freeMoneyRub / monthlyNeedRub).coerceIn(0.0, 3.0)
            finalProb = when {
                ratio >= 2.0 -> (finalProb + 30).coerceIn(0, 100)
                ratio >= 1.5 -> (finalProb + 20).coerceIn(0, 100)
                ratio >= 1.0 -> (finalProb + 10).coerceIn(0, 100)
                ratio >= 0.5 -> finalProb
                else -> (finalProb - 15).coerceIn(0, 100)
            }
        }

        val savedRatio = totalSavingsRub / (goal.amount ?: 1.0)
        if (savedRatio >= 0.5) finalProb = (finalProb + 15).coerceIn(0, 100)

        return when {
            finalProb >= 85 -> MlPredictionResult(finalProb, "Отлично!", "", androidx.compose.ui.graphics.Color(0xFF035715), "Вы на верном пути!")
            finalProb >= 65 -> MlPredictionResult(finalProb, "Хороший шанс", "", androidx.compose.ui.graphics.Color(0xFF124370), "Нужно немного поднажать")
            finalProb >= 40 -> MlPredictionResult(finalProb, "Средний шанс", "⚠", androidx.compose.ui.graphics.Color(0xFFC49F41), "Риск срыва")
            else -> MlPredictionResult(finalProb, "Высокий риск", "", androidx.compose.ui.graphics.Color(0xFF940C1B), "Пересмотрите стратегию")
        }
    }
}