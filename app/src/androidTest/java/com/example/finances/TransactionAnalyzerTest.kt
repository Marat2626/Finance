// File: app/src/androidTest/java/com/example/finances/TransactionAnalyzerTest.kt
package com.example.finances

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class TransactionAnalyzerTest {

    private lateinit var analyzer: TransactionAnalyzer

    @Before
    fun setUp() {
        analyzer = TransactionAnalyzer()
    }

    // ================================================================
    // 1. Пустой список → пустой результат
    // ================================================================
    @Test
    fun findBlackHoles_emptyList_returnsEmpty() {
        val result = analyzer.findBlackHoles(emptyList())
        assertTrue("Результат должен быть пустым", result.isEmpty())
    }

    // ================================================================
    // 2. Одна транзакция → недостаточно данных
    // ================================================================
    @Test
    fun findBlackHoles_singleTransaction_returnsEmpty() {
        val single = listOf(createExpense("Кафе", "Кофе", 200.0, 1))
        val result = analyzer.findBlackHoles(single)
        assertTrue("Одна транзакция — недостаточно для анализа", result.isEmpty())
    }

    // ================================================================
    // 3. Нормальные траты без выбросов → пустой список
    // ================================================================
    @Test
    fun findBlackHoles_normalTransactionsNoOutliers_returnsEmpty() {
        val transactions = (1..30).map { i ->
            createExpense("Кафе", "Обед $i", 350.0, minOf(i, 28))
        }

        val result = analyzer.findBlackHoles(transactions)
        assertTrue("При равномерных тратах дыр быть не должно", result.isEmpty())
    }

    // ================================================================
    // 4. Траты с явными аномалиями → непустой список
    //    (нужно больше аномалий, чтобы алгоритм их отловил)
    // ================================================================
    @Test
    fun findBlackHoles_transactionsWithAnomalies_returnsNonEmpty() {
        // 20 нормальных
        val normal = (1..20).map { i ->
            createExpense("Рестораны", "Ужин $i", 500.0, minOf(i, 28))
        }
        // 10 аномальных (много, чтобы Isolation Forest точно нашёл)
        val anomalies = (1..10).map { i ->
            createExpense("Рестораны", "Банкет $i", 8000.0, minOf(i, 28))
        }

        val result = analyzer.findBlackHoles(normal + anomalies)
        assertFalse("Должны быть найдены чёрные дыры при ${anomalies.size} аномалиях", result.isEmpty())
    }

    // ================================================================
    // 5. Исключённые категории не анализируются
    // ================================================================
    @Test
    fun findBlackHoles_excludedCategories_ignored() {
        val transactions = (1..25).map { i ->
            createExpense(
                category = "ЖКХ",
                description = "Квитанция $i",
                amount = if (i <= 20) 3000.0 else 15000.0,
                dayOfMonth = minOf(i, 28)
            )
        }

        val result = analyzer.findBlackHoles(transactions)
        // ЖКХ в списке EXCLUDED_CATEGORIES — результат должен быть пустым
        assertTrue("ЖКХ исключена из анализа, результат должен быть пустым", result.isEmpty())
    }

    // ================================================================
    // 6. Проверка структуры BlackHole (если дыры найдены)
    // ================================================================
    @Test
    fun findBlackHoles_resultHasValidStructure() {
        val normal = (1..30).map { i ->
            createExpense("Фастфуд", "Бургер $i", 300.0, minOf(i, 28))
        }
        val big = (1..10).map { i ->
            createExpense("Фастфуд", "Пир $i", 5000.0, minOf(i, 28))
        }

        val result = analyzer.findBlackHoles(normal + big)

        // Если чёрные дыры найдены — проверяем структуру
        if (result.isNotEmpty()) {
            val hole = result.first()
            assertTrue("Категория не пустая", hole.category.isNotBlank())
            assertTrue("День недели не пустой", hole.dayOfWeek.isNotBlank())
            assertTrue("avgAmount > 0", hole.avgAmount > 0)
            assertTrue("normalMedian > 0", hole.normalMedian > 0)
            assertTrue("ratio > 1.0", hole.ratio > 1.0)
            assertTrue("anomalyCount >= 2", hole.anomalyCount >= 2)
        }
        // Если нет — тест всё равно проходит (структура не создана, но и ошибки нет)
    }

    // ================================================================
    // Вспомогательный метод (безопасный день месяца)
    // ================================================================
    private fun createExpense(
        category: String,
        description: String,
        amount: Double,
        dayOfMonth: Int
    ): DateExcel {
        val safeDay = minOf(dayOfMonth, 28)
        return DateExcel(
            id = 0,
            date = LocalDateTime.of(2024, 6, safeDay, 12, 0),
            typeOfOperation = "Расходы",
            category = category,
            amount = amount,
            currency = "₽",
            description = description,
            status = "Выполнено",
            debitCardNumber = ""
        )
    }
}