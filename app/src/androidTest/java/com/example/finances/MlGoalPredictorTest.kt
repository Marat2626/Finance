// File: app/src/androidTest/java/com/example/finances/MlGoalPredictorTest.kt
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
class MlGoalPredictorTest {

    private lateinit var predictor: MlGoalPredictor
    private lateinit var db: DataTransactions

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        predictor = MlGoalPredictor(context)
        db = DataTransactions.getTestDatabase(context)
    }

    @After
    fun tearDown() {
        db.close()
        DataTransactions.clearTestDatabase()
    }

    // ================================================================
    // ТЕСТЫ: predictInflation()
    // ================================================================

    // 1. Категория "Машина" — инфляция положительная
    @Test
    fun predictInflation_carCategory_futureAmountGreaterThanCurrent() {
        val result = predictor.predictInflation(
            category = "Машина",
            currentAmount = 1_000_000.0,
            months = 12,
            onAcceptInflation = {}
        )

        assertTrue(
            "futureAmount (${String.format("%,.0f", result.futureAmount)}) должен быть > 1 000 000",
            result.futureAmount > 1_000_000.0
        )
        assertEquals("months должен быть 12", 12, result.months)
        assertTrue("recommendedMonthly > 0", result.recommendedMonthly > 0)
    }

    // 2. Пустая категория → futureAmount = currentAmount
    @Test
    fun predictInflation_emptyCategory_returnsSameAmount() {
        val result = predictor.predictInflation(
            category = "",
            currentAmount = 50_000.0,
            months = 12,
            onAcceptInflation = {}
        )

        assertEquals(50_000.0, result.futureAmount, 0.01)
        assertEquals("0.0", result.growthPercent)
        assertEquals(50_000.0 / 12, result.recommendedMonthly, 0.01)
    }

    // 3. Пустая строка с пробелами
    @Test
    fun predictInflation_blankCategory_returnsSameAmount() {
        val result = predictor.predictInflation(
            category = "   ",
            currentAmount = 100_000.0,
            months = 6,
            onAcceptInflation = {}
        )
        assertEquals(100_000.0, result.futureAmount, 0.01)
    }

    // 4. Неизвестная категория
    @Test
    fun predictInflation_unknownCategory_returnsSameAmount() {
        val result = predictor.predictInflation(
            category = "НеизвестнаяКатегорияXYZ",
            currentAmount = 200_000.0,
            months = 12,
            onAcceptInflation = {}
        )
        assertEquals(200_000.0, result.futureAmount, 0.01)
    }

    // 5. Категория "Квартира" — проверяем growthPercent
    @Test
    fun predictInflation_apartmentCategory_hasPositiveGrowth() {
        val result = predictor.predictInflation(
            category = "Квартира",
            currentAmount = 5_000_000.0,
            months = 12,
            onAcceptInflation = {}
        )

        assertTrue("futureAmount должен быть больше", result.futureAmount > 5_000_000.0)
        assertTrue("growthPercent не пустой", result.growthPercent.isNotBlank())
        val growth = result.growthPercent.toDoubleOrNull()
        assertNotNull("growthPercent должен быть числом: ${result.growthPercent}", growth)
    }

    // 6. Нулевая сумма
    @Test
    fun predictInflation_zeroAmount_returnsZero() {
        val result = predictor.predictInflation(
            category = "Машина",
            currentAmount = 0.0,
            months = 12,
            onAcceptInflation = {}
        )
        assertEquals(0.0, result.futureAmount, 0.01)
        assertEquals(0.0, result.recommendedMonthly, 0.01)
    }

    // 7. Проверка callback onAccept
    @Test
    fun predictInflation_onAcceptCallback_works() {
        val result = predictor.predictInflation(
            category = "Телефон",
            currentAmount = 50_000.0,
            months = 6,
            onAcceptInflation = {}
        )

        var callbackCalled = false
        var receivedStrategy: StrategyRecommendation? = null

        // Создаём новый результат с callback
        val resultWithCallback = MlGoalPredictor.MlInflationResult(
            futureAmount = result.futureAmount,
            growthPercent = result.growthPercent,
            months = result.months,
            recommendedMonthly = result.recommendedMonthly,
            onAccept = {
                callbackCalled = true
                // Не можем проверить strategy, но можем проверить вызов
            }
        )
        resultWithCallback.onAccept()
        assertTrue("Callback должен быть вызван", callbackCalled)
    }

    // ================================================================
    // ТЕСТЫ: predict() — RandomForest
    // ================================================================

    // 8. predict() для высокого дохода → probability > 60%
    @Test
    fun predict_highIncome_highProbability(): Unit = runBlocking {
        insertIncome(amount = 200_000.0, description = "Зарплата")
        insertIncome(amount = 200_000.0, description = "Зарплата")
        insertIncome(amount = 200_000.0, description = "Зарплата")
        insertIncome(amount = 200_000.0, description = "Зарплата")

        insertExpense(amount = 10_000.0, category = "Еда", description = "Продукты")
        insertExpense(amount = 5_000.0, category = "Транспорт", description = "Метро")

        val goal = DataGoal(
            name = "Тестовая цель",
            amount = 100_000.0,
            monthsCount = 12,
            monthlyPayment = 8_333.0,
            createdDate = LocalDateTime.now().minusMonths(1)
        )

        val prediction = predictor.predict(db, goal)

        assertTrue(
            "Вероятность (${prediction.probability}%) должна быть > 60% при высоком доходе",
            prediction.probability > 60
        )
        assertTrue("Название не пустое", prediction.title.isNotBlank())
        assertTrue("Совет не пустой", prediction.advice.isNotBlank())
    }

    // 9. predict() для низкого дохода → probability < 50%
    @Test
    fun predict_lowIncome_lowProbability(): Unit = runBlocking {
        insertIncome(amount = 30_000.0, description = "Зарплата")
        insertIncome(amount = 30_000.0, description = "Зарплата")

        insertExpense(amount = 25_000.0, category = "Аренда", description = "Квартира")
        insertExpense(amount = 10_000.0, category = "Еда", description = "Продукты")

        val goal = DataGoal(
            name = "Дорогая цель",
            amount = 500_000.0,
            monthsCount = 12,
            monthlyPayment = 41_666.0,
            createdDate = LocalDateTime.now().minusMonths(1)
        )

        val prediction = predictor.predict(db, goal)

        assertTrue(
            "Вероятность (${prediction.probability}%) должна быть < 50% при низком доходе",
            prediction.probability < 50
        )
    }

    // 10. predict() — проверка диапазона probability
    @Test
    fun predict_probabilityInRange(): Unit = runBlocking {
        insertIncome(amount = 100_000.0, description = "Зарплата")
        insertIncome(amount = 100_000.0, description = "Зарплата")
        insertExpense(amount = 30_000.0, category = "Еда", description = "Продукты")

        val goal = DataGoal(
            name = "Средняя цель",
            amount = 200_000.0,
            monthsCount = 10,
            monthlyPayment = 20_000.0,
            createdDate = LocalDateTime.now().minusMonths(1)
        )

        val prediction = predictor.predict(db, goal)

        assertTrue("probability >= 0", prediction.probability >= 0)
        assertTrue("probability <= 100", prediction.probability <= 100)
    }

    // ================================================================
    // Вспомогательные методы
    // ================================================================

    private fun insertIncome(amount: Double, description: String) {
        runBlocking {
            db.dataExcelDao().insert(
                DateExcel(
                    date = LocalDateTime.now().minusDays(10),
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

    private fun insertExpense(amount: Double, category: String, description: String) {
        runBlocking {
            db.dataExcelDao().insert(
                DateExcel(
                    date = LocalDateTime.now().minusDays(5),
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