package com.example.finances


import android.util.Log
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.max


class GoalSavingsCalculator(private val db: DataTransactions) {

    suspend fun calculateStrategies(goalAmount: Double): List<StrategyRecommendation> {

        val settings = db.categorySettingsDao().getAllSettings().first()
        val expenses = db.dataExcelDao().getAllExpenses().first()
        val incomes = db.dataExcelDao().getAllIncome().first()

        val sixMonthsAgo = LocalDate.now().minusMonths(6)

        val incomesLast6Months = incomes.filter { it.date.toLocalDate().isAfter(sixMonthsAgo)}
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
        var essentialSum = 0.0

        val categoriesWithDetailed = db.expenseSettingsDao()
            .getAllSettings()
            .first()
            .map { it.category }
            .distinct()
            .toSet()


        val baseComfort = realRemaining + nonEssentialSum
        val baseBalanced = realRemaining + nonEssentialSum + neutralSum

        if (avgMonthlyIncome <= 130000) {
            val (comfortPercent, balancedPercent, hardPercent) =

                if (goalAmount <= avgMonthlyIncome) {
                    Triple(0.15, 0.40, 0.65)
                }
                else if (goalAmount <= 300000) {
                    Triple(0.35, 0.55, 0.80)
                }else if (goalAmount <= 700000) {
                    Triple(0.40, 0.60, 0.85)
                }
                else  Triple(0.45, 0.65, 0.90)
            val monthlyComfort = baseComfort * comfortPercent
            val monthlyBalanced = baseBalanced * balancedPercent
            val monthlyHard = baseBalanced * hardPercent

            return listOf(
                createStrategy("Комфортное", monthlyComfort, goalAmount, R.drawable.moneysave, "Можно почти не менять привычки"),
                createStrategy("Сбалансированное", monthlyBalanced, goalAmount, R.drawable.balancingmoney, "Разумный баланс"),
                createStrategy("Жёсткое", monthlyHard, goalAmount, R.drawable.monyy, "Максимально быстро")
            )
        }

        else if (avgMonthlyIncome <= 250000){

            val (comfortPercent, balancedPercent, hardPercent) =

                if (goalAmount <= 120000) {
                Triple(0.20, 0.40, 0.65)
                }
                else if (goalAmount <= 300000) {
                    Triple(0.25, 0.50, 0.75)
                }else if (goalAmount <= 600000) {
                    Triple(0.30, 0.60, 0.80)
                }
                else  Triple(0.35, 0.65, 0.85)

            val monthlyComfort = baseComfort * comfortPercent
            val monthlyBalanced = baseComfort * balancedPercent
            val monthlyHard = baseBalanced * hardPercent
            return listOf(
                createStrategy("Комфортное", monthlyComfort, goalAmount, R.drawable.moneysave, "Можно почти не менять привычки"),
                createStrategy("Сбалансированное", monthlyBalanced, goalAmount, R.drawable.balancingmoney, "Разумный баланс"),
                createStrategy("Жёсткое", monthlyHard, goalAmount, R.drawable.monyy, "Максимально быстро")
            )
        }
        else if (avgMonthlyIncome <= 350000){
            val (comfortPercent, balancedPercent, hardPercent) =

                if (goalAmount <= 150000) {
                    Triple(0.20, 0.40, 0.60)
                }
                else if (goalAmount <= 300000) {
                    Triple(0.25, 0.50, 0.65)
                }else if (goalAmount <= 700000) {
                    Triple(0.35, 0.65, 0.75)
                }
                else  Triple(0.40, 0.65, 0.85)


            val monthlyComfort = baseComfort * comfortPercent
            val monthlyBalanced = baseComfort * balancedPercent
            val monthlyHard = baseComfort * hardPercent

            Log.d("GOAL_CALC", "Доход до 350 000 ₽, ")

            return listOf(
                createStrategy("Комфортное", monthlyComfort, goalAmount, R.drawable.moneysave, "Можно почти не менять привычки"),
                createStrategy("Сбалансированное", monthlyBalanced, goalAmount, R.drawable.balancingmoney, "Разумный баланс"),
                createStrategy("Жёсткое", monthlyHard, goalAmount, R.drawable.monyy, "Максимально быстро")
            )
        }
        else if (avgMonthlyIncome <= 500000){
            val (comfortPercent, balancedPercent, hardPercent) =

                if (goalAmount <= 150000) {
                    Triple(0.15, 0.35, 0.55)
                }
                else if (goalAmount <= 300000) {
                    Triple(0.25, 0.50, 0.65)
                }else if (goalAmount <= 700000) {
                    Triple(0.27, 0.60, 0.70)
                }
                else if (goalAmount <= 1000000) {
                    Triple(0.30, 0.63, 0.75)
                }
                else if (goalAmount <= 2000000) {
                    Triple(0.33, 0.65, 0.80)
                }
                else  Triple(0.40, 0.70, 0.85)
            val monthlyComfort = baseComfort * comfortPercent
            val monthlyBalanced = baseComfort * balancedPercent
            val monthlyHard = baseComfort * hardPercent

            return listOf(
                createStrategy("Комфортное", monthlyComfort, goalAmount, R.drawable.moneysave, "Можно почти не менять привычки"),
                createStrategy("Сбалансированное", monthlyBalanced, goalAmount, R.drawable.balancingmoney, "Разумный баланс"),
                createStrategy("Жёсткое", monthlyHard, goalAmount, R.drawable.monyy, "Максимально быстро")
            )
        }

        else {
            val (comfortPercent, balancedPercent, hardPercent) =

                if (goalAmount <= 150000) {
                    Triple(0.10, 0.30, 0.40)
                }
                else if (goalAmount <= 300000) {
                    Triple(0.18, 0.40, 0.55)
                }else if (goalAmount <= 700000) {
                    Triple(0.25, 0.50, 0.65)
                }
                else if (goalAmount <= 1000000) {
                    Triple(0.00, 0.60, 0.75)
                }
                else if (goalAmount <= 2000000) {
                    Triple(0.33, 0.65, 0.80)
                }
                else  Triple(0.35, 0.70, 0.85)


            val monthlyComfort = baseComfort * comfortPercent
            val monthlyBalanced = baseComfort * balancedPercent
            val monthlyHard = baseComfort * hardPercent

            Log.d("GOAL_CALC", "Доход до  не ограничен ₽, ")

            return listOf(
                createStrategy("Комфортное", monthlyComfort, goalAmount, R.drawable.moneysave, "Можно почти не менять привычки"),
                createStrategy("Сбалансированное", monthlyBalanced, goalAmount, R.drawable.balancingmoney, "Разумный баланс"),
                createStrategy("Жёсткое", monthlyHard, goalAmount, R.drawable.monyy, "Максимально быстро"),
                createStrategy("Своя сумма", monthlyHard, goalAmount,R.drawable.ves4 , "Своя сумма")

            )
        }


    }

    private fun createStrategy(
        name: String,
        monthlyPayment: Double,
        goalAmount: Double,
        id: Int,
        desc: String
    ): StrategyRecommendation {

        val maxPayment = goalAmount


        val minPayment = when {
            goalAmount <= 30000 -> 1000.0
            goalAmount <= 100000 -> 3000.0
            else -> 5000.0
        }


        var finalMonthly = monthlyPayment.coerceAtLeast(minPayment).coerceAtMost(maxPayment)


        if (finalMonthly > goalAmount) {
            finalMonthly = goalAmount
        }

        val months = when {
            finalMonthly >= goalAmount -> 1
            else -> max(1, ceil(goalAmount / finalMonthly).toInt())
        }

        return StrategyRecommendation(
            name = name,
            months = months,
            monthlyPayment = finalMonthly,
            id = id,
            description = desc
        )



    }
}


data class StrategyRecommendation(
    val name: String,
    val months: Int,
    val monthlyPayment: Double,
    val id: Int,
    val description: String,
    val goalAmount: Double = 0.0
)
