package com.example.finances


import android.util.Log
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import kotlin.math.ceil


class goalProgressPredictor(private val db: DataTransactions) {


    data class PredictionResult(
        val probability: Int,      // 0–100%
        val title: String,
        val emoji: String,
        val color: Color,
        val advice: String
    )


    suspend fun predict(
        goal: DataGoal,
    ): PredictionResult {

        val expenses = db.dataExcelDao().getAllExpenses().first()
        val incomes = db.dataExcelDao().getAllIncome().first()

        val sixMonthsAgo = LocalDate.now().minusMonths(6)

        val incomesLast6Months = incomes.filter { it.date.toLocalDate().isAfter(sixMonthsAgo) }
        val expensesLast6Months = expenses.filter { it.date.toLocalDate().isAfter(sixMonthsAgo) }

        val monthsCount = (incomesLast6Months.map { it.date.toLocalDate().withDayOfMonth(1) } +
                expensesLast6Months.map { it.date.toLocalDate().withDayOfMonth(1) })
            .distinct()
            .size
            .coerceAtLeast(1)

        val totalIncome6Months = incomesLast6Months.sumOf { it.amount }
        val totalExpenses6Months = expensesLast6Months.sumOf { it.amount }

        val avgMonthlyIncome = totalIncome6Months / monthsCount
        val avgMonthlyExpense = totalExpenses6Months / monthsCount
        val realRemaining = avgMonthlyIncome - avgMonthlyExpense

        var nonEssentialSum = 0.0
        var neutralSum = 0.0


        val baseComfort = realRemaining + nonEssentialSum
        val baseBalanced = realRemaining + nonEssentialSum + neutralSum
        val baseHard = realRemaining + nonEssentialSum + neutralSum


        val goalAmounts = goal.amount ?: 0.0
        val monthlyPayment = goal.monthlyPayment

        if (monthlyPayment == 0.0) {
            // Если ежемесячный взнос не задан, используем базовый расчет
            return PredictionResult(50, "Нет данных", "❓", Color.Gray, "Укажите ежемесячный взнос")
        }


        val monthsNeeded = ceil((goalAmounts / monthlyPayment)).toInt()

        val effortRatio = (monthlyPayment / baseBalanced).coerceIn(0.0, 1.0)



        val probability = (100 / (1 + monthsNeeded * 0.12)) *
                (1 + (1 - effortRatio) * 0.5 )


        var finalProb = 0

        if (baseBalanced < 70000) {

            val adjustment = when {
                effortRatio <= 0.3 -> 54
                effortRatio <= 0.35 -> 53
                effortRatio <= 0.4 -> 52
                effortRatio <= 0.45 -> 51
                effortRatio <= 0.5 -> 50
                effortRatio <= 0.55 -> 48
                effortRatio <= 0.60 -> 46
                effortRatio <= 0.65 -> 43
                effortRatio <= 0.70 -> 38
                effortRatio <= 0.75 -> 33
                effortRatio <= 0.8 -> 27
                effortRatio <= 0.85 -> 24
                effortRatio <= 0.9 -> 20
                else -> 15
            }
            if (effortRatio <= 0.5) {

                val baseChance = when {

                    monthsNeeded <= 2 -> 45
                    monthsNeeded <= 4 -> 40
                    monthsNeeded <= 6 -> 30
                    monthsNeeded <= 12 -> 25
                    monthsNeeded <= 24 -> 20
                    monthsNeeded <= 36 -> 15
                    else -> 5
                }

                finalProb = (baseChance + adjustment).coerceIn(0, 100)

            } else {

                val baseChance = when {

                    monthsNeeded <= 2 -> 45
                    monthsNeeded <= 4 -> 40
                    monthsNeeded <= 6 -> 25
                    monthsNeeded <= 12 -> 15
                    monthsNeeded <= 24 -> 8
                    monthsNeeded <= 36 -> 0
                    else -> -5
                }

                finalProb = (baseChance + adjustment).coerceIn(0, 100)
            }


            Log.d("GOAL_CALC", "$finalProb свободный остаток $baseBalanced <= 70.000")

        }





        return when {
            finalProb >= 85 -> PredictionResult(
                finalProb, "Отличный шанс!", "🚀", Color(0xFF6BCB6B),
                "Вы идёте с опережением. Продолжайте в том же темпе!"
            )
            finalProb >= 65 -> PredictionResult(
                finalProb, "Хороший шанс", "👍", Color(0xFF6B9CFF),
                "Нужно немного поднажать в следующие 2–3 месяца"
            )
            finalProb >= 40 -> PredictionResult(
                finalProb, "Средний шанс", "⚠️", Color(0xFFFFD700),
                "Риск срыва. Старайтесь не пропускать взносы"
            )
            else -> PredictionResult(
                finalProb, "Высокий риск", "❌", Color(0xFFFF6B6B),
                "Нужно увеличить взнос или пересмотреть стратегию"
            )
        }
    }

    private fun getMonthsPassed(created: java.time.LocalDateTime): Int {
        val now = java.time.LocalDateTime.now()
        return java.time.temporal.ChronoUnit.MONTHS.between(created, now).toInt()
    }
}
