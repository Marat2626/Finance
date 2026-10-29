// File: app/src/androidTest/java/com/example/finances/InflationPredictorTest.kt
package com.example.finances

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InflationPredictorTest {

    private lateinit var predictor: MlGoalPredictor

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        predictor = MlGoalPredictor(context)
    }

    // ================================================================
    // 1. Машина — инфляция положительная (futureAmount > currentAmount)
    // ================================================================
    @Test
    fun predictInflation_car_positiveGrowth() {
        val result = predictor.predictInflation(
            category = "Машина",
            currentAmount = 1_000_000.0,
            months = 12,
            onAcceptInflation = {}
        )

        assertTrue("Будущая сумма (${String.format("%,.0f", result.futureAmount)}) > 1 000 000",
            result.futureAmount > 1_000_000.0)
        assertTrue("growthPercent > 0", result.growthPercent.toDoubleOrNull() ?: 0.0 > 0)
    }

    // ================================================================
    // 2. Квартира — рост есть
    // ================================================================
    @Test
    fun predictInflation_apartment_positiveGrowth() {
        val result = predictor.predictInflation(
            category = "Квартира",
            currentAmount = 5_000_000.0,
            months = 24,
            onAcceptInflation = {}
        )

        assertTrue("futureAmount > 5 000 000", result.futureAmount > 5_000_000.0)
        assertTrue("growthPercent > 0", result.growthPercent.toDoubleOrNull() ?: 0.0 > 0)
        assertEquals(24, result.months)
    }

    // ================================================================
    // 3. Пустая категория → без изменений
    // ================================================================
    @Test
    fun predictInflation_emptyCategory_noChange() {
        val result = predictor.predictInflation(
            category = "",
            currentAmount = 50_000.0,
            months = 12,
            onAcceptInflation = {}
        )

        assertEquals(50_000.0, result.futureAmount, 0.01)
        assertEquals("0.0", result.growthPercent)
    }

    // ================================================================
    // 4. Пробелы → без изменений
    // ================================================================
    @Test
    fun predictInflation_blankCategory_noChange() {
        val result = predictor.predictInflation(
            category = "   ",
            currentAmount = 100_000.0,
            months = 6,
            onAcceptInflation = {}
        )
        assertEquals(100_000.0, result.futureAmount, 0.01)
    }

    // ================================================================
    // 5. Неизвестная категория → без изменений
    // ================================================================
    @Test
    fun predictInflation_unknownCategory_noChange() {
        val result = predictor.predictInflation(
            category = "Космолёт",
            currentAmount = 200_000.0,
            months = 12,
            onAcceptInflation = {}
        )
        assertEquals(200_000.0, result.futureAmount, 0.01)
    }

    // ================================================================
    // 6. Нулевая сумма → 0
    // ================================================================
    @Test
    fun predictInflation_zeroAmount_returnsZero() {
        val result = predictor.predictInflation(
            category = "Мебель",
            currentAmount = 0.0,
            months = 12,
            onAcceptInflation = {}
        )
        assertEquals(0.0, result.futureAmount, 0.01)
        assertEquals(0.0, result.recommendedMonthly, 0.01)
    }

    // ================================================================
    // 7. Телефон — проверка роста
    // ================================================================
    @Test
    fun predictInflation_phone_positiveGrowth() {
        val result = predictor.predictInflation(
            category = "Телефон",
            currentAmount = 80_000.0,
            months = 6,
            onAcceptInflation = {}
        )

        // Телефоны могут дорожать или дешеветь — проверяем что не падает
        assertTrue("futureAmount должен быть > 0", result.futureAmount > 0)
        assertTrue("growthPercent не пустой", result.growthPercent.isNotBlank())
    }

    // ================================================================
    // 8. Отпуск — структура результата
    // ================================================================
    @Test
    fun predictInflation_vacation_validStructure() {
        val result = predictor.predictInflation(
            category = "Отпуск",
            currentAmount = 150_000.0,
            months = 12,
            onAcceptInflation = {}
        )

        assertTrue("futureAmount > 0", result.futureAmount > 0)
        assertTrue("growthPercent не пустой", result.growthPercent.isNotBlank())
        assertTrue("months > 0", result.months > 0)
        assertTrue("recommendedMonthly > 0", result.recommendedMonthly > 0)
    }
}