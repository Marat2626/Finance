// File: app/src/androidTest/java/com/example/finances/RandomForestPortTest.kt
package com.example.finances

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RandomForestPortTest {

    private lateinit var forest: RandomForestPort

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        forest = RandomForestPort(context)
    }

    // ================================================================
    // 1. Модель загрузилась и возвращает число
    // ================================================================
    @Test
    fun predictProbability_returnsNumber() {
        val features = floatArrayOf(35f, 100f, 20f, 80f, 2f, 1f, 0f, 750f)
        val probability = forest.predictProbability(features)

        assertTrue("Вероятность должна быть числом", !probability.isNaN())
        assertTrue("Вероятность должна быть конечной", !probability.isInfinite())
    }

    // ================================================================
    // 2. Вероятность всегда в диапазоне [0, 1]
    // ================================================================
    @Test
    fun predictProbability_alwaysInRange() {
        val testCases = listOf(
            floatArrayOf(35f, 100f, 20f, 80f, 2f, 1f, 0f, 750f),   // высокий доход
            floatArrayOf(35f, 5f, 15f, 1f, 2f, 1f, 1f, 400f),       // низкий доход
            floatArrayOf(25f, 40f, 30f, 10f, 0f, 1f, 0f, 550f),     // средний
            floatArrayOf(50f, 200f, 50f, 200f, 3f, 2f, 0f, 800f)    // высокий
        )

        testCases.forEachIndexed { index, features ->
            val prob = forest.predictProbability(features)
            assertTrue(
                "Тест $index: вероятность $prob вне диапазона [0, 1]",
                prob in 0f..1f
            )
        }
    }

    // ================================================================
    // 3. Крайние значения не ломают модель
    // ================================================================
    @Test
    fun predictProbability_extremeValues_doesNotCrash() {
        val zeroFeatures = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        val maxFeatures = floatArrayOf(100f, 1000f, 1000f, 1000f, 100f, 100f, 1f, 1000f)

        val probZero = forest.predictProbability(zeroFeatures)
        val probMax = forest.predictProbability(maxFeatures)

        assertTrue("probZero в диапазоне: $probZero", probZero in 0f..1f)
        assertTrue("probMax в диапазоне: $probMax", probMax in 0f..1f)
    }

    // ================================================================
    // 4. Отрицательные значения не ломают
    // ================================================================
    @Test
    fun predictProbability_negativeValues_doesNotCrash() {
        val negativeFeatures = floatArrayOf(-10f, -50f, -20f, -5f, -1f, -1f, 0f, -100f)

        val prob = forest.predictProbability(negativeFeatures)
        assertTrue("Вероятность при отрицательных признаках в [0,1]: $prob", prob in 0f..1f)
    }

    // ================================================================
    // 5. Одинаковые признаки → консистентный результат
    // ================================================================
    @Test
    fun predictProbability_sameFeatures_sameResult() {
        val features = floatArrayOf(30f, 60f, 25f, 30f, 1f, 1f, 0f, 600f)

        val prob1 = forest.predictProbability(features)
        val prob2 = forest.predictProbability(features)

        assertEquals("Одинаковые признаки → одинаковый результат", prob1, prob2)
    }

    // ================================================================
    // 6. Высокий доход > низкого дохода (тенденция, не жёстко)
    //    Проверяем что оба вычисляются без ошибок
    // ================================================================
    @Test
    fun predictProbability_highAndLowIncome_bothCompute() {
        val highIncome = floatArrayOf(35f, 150f, 30f, 100f, 3f, 2f, 0f, 800f)
        val lowIncome = floatArrayOf(35f, 5f, 10f, 1f, 0f, 1f, 1f, 300f)

        val probHigh = forest.predictProbability(highIncome)
        val probLow = forest.predictProbability(lowIncome)

        assertTrue("probHigh в [0,1]", probHigh in 0f..1f)
        assertTrue("probLow в [0,1]", probLow in 0f..1f)

        // Логируем для информации (не assert)
        println("High income probability: ${String.format("%.2f", probHigh)}")
        println("Low income probability: ${String.format("%.2f", probLow)}")
    }
}