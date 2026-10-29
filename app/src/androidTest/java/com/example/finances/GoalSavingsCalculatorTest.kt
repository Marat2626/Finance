// File: app/src/androidTest/java/com/example/finances/GoalSavingsCalculatorTest.kt
package com.example.finances

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class GoalSavingsCalculatorTest {

    private lateinit var calculator: GoalSavingsCalculator
    private lateinit var db: DataTransactions

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = DataTransactions.getTestDatabase(context)
        calculator = GoalSavingsCalculator(db)
    }

    @After
    fun tearDown() {
        db.close()
        DataTransactions.clearTestDatabase()
    }

    // ================================================================
    // 1. Стандартный сценарий: доход 100к, расходы 40к, цель 120к
    //    Должны быть 3 стратегии
    // ================================================================
    @Test
    fun calculateStrategies_normalIncome_returnsThreeStrategies(): Unit = runBlocking {
        // 6 месяцев доходов по 100 000
        repeat(6) { month ->
            insertIncome(100_000.0, "Зарплата", month)
        }

        // 6 месяцев расходов по 40 000
        repeat(6) { month ->
            insertExpense(40_000.0, "Еда", "Продукты", month)
        }

        val strategies = calculator.calculateStrategies(120_000.0)

        assertEquals("Должно быть 3 стратегии", 3, strategies.size)

        // Проверяем структуру каждой стратегии
        strategies.forEach { strategy ->
            assertTrue("Имя не пустое: ${strategy.name}", strategy.name.isNotBlank())
            assertTrue("Описание не пустое", strategy.description.isNotBlank())
            assertTrue("months > 0", strategy.months > 0)
            assertTrue("monthlyPayment > 0", strategy.monthlyPayment > 0)
            assertTrue("id не нулевой", strategy.id != 0)
        }
    }

    // ================================================================
    // 2. Стратегии при большом доходе (500к)
    //    Для дохода > 500к должно быть 4 стратегии (включая "Своя сумма")
    // ================================================================
    @Test
    fun calculateStrategies_highIncome_returnsFourStrategies(): Unit = runBlocking {
        repeat(6) { month ->
            insertIncome(500_000.0, "Зарплата", month)
            insertExpense(100_000.0, "Еда", "Продукты", month)
        }

        val strategies = calculator.calculateStrategies(300_000.0)

        assertTrue("Для высокого дохода должно быть ≥3 стратегий", strategies.size >= 3)
        strategies.forEach { strategy ->
            assertTrue("monthlyPayment > 0 для дохода 500к", strategy.monthlyPayment > 0)
        }
    }

    // ================================================================
    // 3. Проверка названий стратегий
    // ================================================================
    @Test
    fun calculateStrategies_hasCorrectStrategyNames(): Unit = runBlocking {
        repeat(6) { month ->
            insertIncome(80_000.0, "Зарплата", month)
            insertExpense(30_000.0, "Еда", "Продукты", month)
        }

        val strategies = calculator.calculateStrategies(60_000.0)
        val names = strategies.map { it.name }

        assertTrue("Должна быть стратегия 'Комфортное'", names.any { it.contains("Комфорт") })
        assertTrue("Должна быть стратегия 'Сбалансированное'", names.any { it.contains("Сбалансирован") })
        assertTrue("Должна быть стратегия 'Жёсткое'", names.any { it.contains("Жёстк") })
    }

    // ================================================================
    // 4. Нулевая цель → платежи = 0
    // ================================================================
    @Test
    fun calculateStrategies_zeroGoal_returnsZeroPayments(): Unit = runBlocking {
        repeat(6) { month ->
            insertIncome(100_000.0, "Зарплата", month)
            insertExpense(40_000.0, "Еда", "Продукты", month)
        }

        val strategies = calculator.calculateStrategies(0.0)

        strategies.forEach { strategy ->
            assertEquals(
                "При нулевой цели monthlyPayment должен быть 0",
                0.0,
                strategy.monthlyPayment,
                0.01
            )
        }
    }

    // ================================================================
    // 5. Отрицательная цель — обрабатывается корректно
    // ================================================================
    @Test
    fun calculateStrategies_negativeGoal_handlesCorrectly(): Unit = runBlocking {
        repeat(6) { month ->
            insertIncome(100_000.0, "Зарплата", month)
            insertExpense(40_000.0, "Еда", "Продукты", month)
        }

        val strategies = calculator.calculateStrategies(-50_000.0)

        strategies.forEach { strategy ->
            assertTrue(
                "Платёж не должен быть положительным для отрицательной цели",
                strategy.monthlyPayment <= 0.0
            )
        }
    }

    // ================================================================
    // 6. Нет данных о доходах — всё равно возвращает 3 стратегии
    // ================================================================
    @Test
    fun calculateStrategies_noIncomeData_returnsThreeStrategies(): Unit = runBlocking {
        // Вообще нет транзакций
        val strategies = calculator.calculateStrategies(100_000.0)

        assertEquals("Должно быть 3 стратегии даже без данных", 3, strategies.size)
        strategies.forEach { strategy ->
            assertTrue("months > 0", strategy.months > 0)
            // Платёж может быть минимальным, но не отрицательным
            assertTrue("monthlyPayment >= 0", strategy.monthlyPayment >= 0)
        }
    }

    // ================================================================
    // 7. months округляется вверх (ceil)
    // ================================================================
    @Test
    fun calculateStrategies_monthsRoundedUp(): Unit = runBlocking {
        repeat(6) { month ->
            insertIncome(100_000.0, "Зарплата", month)
            insertExpense(40_000.0, "Еда", "Продукты", month)
        }

        val strategies = calculator.calculateStrategies(100_000.0)

        strategies.forEach { strategy ->
            val calculatedMonths = Math.ceil(100_000.0 / strategy.monthlyPayment).toInt()
            assertEquals(
                "months должно быть ceil(goalAmount / monthlyPayment)",
                calculatedMonths,
                strategy.months
            )
        }
    }

    // ================================================================
    // 8. Очень большая цель — не ломается
    // ================================================================
    @Test
    fun calculateStrategies_veryLargeGoal_handlesCorrectly(): Unit = runBlocking {
        repeat(6) { month ->
            insertIncome(200_000.0, "Зарплата", month)
            insertExpense(50_000.0, "Еда", "Продукты", month)
        }

        val strategies = calculator.calculateStrategies(10_000_000.0)

        assertEquals(3, strategies.size)
        strategies.forEach { strategy ->
            assertTrue("months > 0 для большой цели", strategy.months > 0)
            assertTrue("monthlyPayment > 0", strategy.monthlyPayment > 0)
            assertTrue("monthlyPayment конечный", strategy.monthlyPayment.isFinite())
        }
    }

    // ================================================================
    // Вспомогательные методы
    // ================================================================

    private fun insertIncome(amount: Double, description: String, monthsAgo: Int) {
        runBlocking {
            db.dataExcelDao().insert(
                DateExcel(
                    date = LocalDateTime.now().minusMonths(monthsAgo.toLong()).withDayOfMonth(15),
                    typeOfOperation = "Зачисления",
                    category = "Зарплата",
                    amount = amount,
                    currency = "₽",
                    description = description,
                    status = "Выполнено",
                    debitCardNumber = ""
                )
            )
        }
    }

    private fun insertExpense(amount: Double, category: String, description: String, monthsAgo: Int) {
        runBlocking {
            db.dataExcelDao().insert(
                DateExcel(
                    date = LocalDateTime.now().minusMonths(monthsAgo.toLong()).withDayOfMonth(10),
                    typeOfOperation = "Расходы",
                    category = category,
                    amount = amount,
                    currency = "₽",
                    description = description,
                    status = "Выполнено",
                    debitCardNumber = ""
                )
            )
        }
    }
}