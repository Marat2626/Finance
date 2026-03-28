package com.example.finances


import android.icu.util.LocaleData
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

// Доходы за последние 6 месяцев
        val incomesLast6Months = incomes.filter { it.date.toLocalDate().isAfter(sixMonthsAgo)}
// Расходы за последние 6 месяцев
        val expensesLast6Months = expenses.filter { it.date.toLocalDate().isAfter(sixMonthsAgo) }

// Количество уникальных месяцев, за которые есть данные
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


        Log.d("GOAL_CALC", "${String.format("%,.0f",totalIncome6Months  )} сумма ща последнии 6 месяцев")
        Log.d("GOAL_CALC", "${String.format("%,.0f",totalExpenses6Months  )} траты ща последнии 6 месяцев")

        Log.d("GOAL_CALC", "${String.format("%,.0f",avgMonthlyIncome  )} средняя сумма ща последнии 6 месяцев")
        Log.d("GOAL_CALC", "${String.format("%,.0f",avgMonthlyExpense  )} средние траты ща последнии 6 месяцев")

        Log.d("GOAL_CALC", "${String.format("%,.0f",realRemaining  )} Остаток")

        var nonEssentialSum = 0.0
        var neutralSum = 0.0
        var essentialSum = 0.0

// Загружаем все категории, у которых есть детальные настройки
        val categoriesWithDetailed = db.expenseSettingsDao()
            .getAllSettings()
            .first()
            .map { it.category }
            .distinct()
            .toSet()






        expenses.forEach { exp ->
            val transactionKey = getTransactionKey(exp.description)

            val expenseSetting = db.expenseSettingsDao()
                .getByCategoryAndDescription(exp.category, transactionKey)

            val hasDetailedSettings = categoriesWithDetailed.contains(exp.category)

            val imp = if (hasDetailedSettings) {
                // Если есть детальные настройки — используем только их
                expenseSetting?.importance
            } else {
                // Если нет детальных — используем настройку категории
                expenseSetting?.importance ?: settings.find { it.category == exp.category }?.importance
            }

            when (imp) {
                ImportanceType.NON_ESSENTIAL -> nonEssentialSum += exp.amount
                ImportanceType.NEUTRAL -> neutralSum += exp.amount
                ImportanceType.ESSENTIAL -> essentialSum += exp.amount
                else -> essentialSum += exp.amount
            }
        }


        // ──────────────── Базы для каждой стратегии (как ты хотел) ────────────────
        val baseComfort = realRemaining + nonEssentialSum
        val baseBalanced = realRemaining + nonEssentialSum + neutralSum
        val baseHard = realRemaining + nonEssentialSum + neutralSum


        Log.d("GOAL_CALC", "${String.format("%,.0f",baseComfort  )} Комвфортный остаток")
        Log.d("GOAL_CALC", "${String.format("%,.0f",baseBalanced  )} Сбалансированный  остаток")
        Log.d("GOAL_CALC", "${String.format("%,.0f",baseHard  )} жесткий остаток")



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

            Log.d("GOAL_CALC", "Доход до 130 000 ₽, ")

            return listOf(
                createStrategy("Комфортное", monthlyComfort, goalAmount, "😌", "Можно почти не менять привычки"),
                createStrategy("Сбалансированное", monthlyBalanced, goalAmount, "⚖️", "Разумный баланс"),
                createStrategy("Жёсткое", monthlyHard, goalAmount, "🔥", "Максимально быстро")
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

            Log.d("GOAL_CALC", "Доход до 250 000 ₽, цель ${if (goalAmount <= 120000) "≤120к" else ">120к"} → $comfortPercent, $balancedPercent, $hardPercent")

            return listOf(
                createStrategy("Комфортное", monthlyComfort, goalAmount, "😌", "Можно почти не менять привычки"),
                createStrategy("Сбалансированное", monthlyBalanced, goalAmount, "⚖️", "Разумный баланс"),
                createStrategy("Жёсткое", monthlyHard, goalAmount, "🔥", "Максимально быстро")
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
                createStrategy("Комфортное", monthlyComfort, goalAmount, "😌", "Можно почти не менять привычки"),
                createStrategy("Сбалансированное", monthlyBalanced, goalAmount, "⚖️", "Разумный баланс"),
                createStrategy("Жёсткое", monthlyHard, goalAmount, "🔥", "Максимально быстро")
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

            Log.d("GOAL_CALC", "Доход до 500 000 ₽, ")

            return listOf(
                createStrategy("Комфортное", monthlyComfort, goalAmount, "😌", "Можно почти не менять привычки"),
                createStrategy("Сбалансированное", monthlyBalanced, goalAmount, "⚖️", "Разумный баланс"),
                createStrategy("Жёсткое", monthlyHard, goalAmount, "🔥", "Максимально быстро")
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
                createStrategy("Комфортное", monthlyComfort, goalAmount, "😌", "Можно почти не менять привычки"),
                createStrategy("Сбалансированное", monthlyBalanced, goalAmount, "⚖️", "Разумный баланс"),
                createStrategy("Жёсткое", monthlyHard, goalAmount, "🔥", "Максимально быстро")
            )
        }


    }


    // Изменена сигнатура — теперь принимает уже готовую monthlyPayment
    private fun createStrategy(
        name: String,
        monthlyPayment: Double,
        goalAmount: Double,
        emoji: String,
        desc: String
    ): StrategyRecommendation {
        // Максимальный платеж: не больше цели, и чтобы срок был минимум 1 месяц
        val maxPayment = goalAmount  // максимум — вся сумма за 1 месяц

        // Минимальный платеж зависит от суммы цели
        val minPayment = when {
            goalAmount <= 30000 -> 1000.0
            goalAmount <= 100000 -> 3000.0
            else -> 5000.0
        }

        // Корректируем платеж
        var finalMonthly = monthlyPayment.coerceAtLeast(minPayment).coerceAtMost(maxPayment)

        // Если платеж получился больше цели, корректируем
        if (finalMonthly > goalAmount) {
            finalMonthly = goalAmount
        }

        // Рассчитываем месяцы
        val months = when {
            finalMonthly >= goalAmount -> 1
            else -> max(1, ceil(goalAmount / finalMonthly).toInt())
        }

        return StrategyRecommendation(
            name = name,
            months = months,
            monthlyPayment = finalMonthly,
            emoji = emoji,
            description = desc
        )



    }
}


data class StrategyRecommendation(
    val name: String,
    val months: Int,
    val monthlyPayment: Double,
    val emoji: String,
    val description: String
)
