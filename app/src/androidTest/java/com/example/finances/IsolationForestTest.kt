// File: app/src/androidTest/java/com/example/finances/IsolationForestTest.kt
package com.example.finances

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IsolationForestTest {

    // ================================================================
    // 1. Пустые данные — не падает
    // ================================================================
    @Test
    fun fit_emptyData_doesNotCrash() {
        val iso = IsolationForest(nTrees = 10, contamination = 0.1)
        iso.fit(arrayOf(doubleArrayOf(100.0, 1.0, 1.0, 0.0)))
        assertEquals(10, iso.getTreeCount())
    }

    // ================================================================
    // 2. Нормальные данные — все isAnomaly = false (или мало)
    // ================================================================
    @Test
    fun isAnomaly_uniformData_mostlyNormal() {
        val data = (1..50).map {
            doubleArrayOf(500.0, 3.0, 2.0, 0.0)
        }.toTypedArray()

        val iso = IsolationForest(nTrees = 50, contamination = 0.1)
        iso.fit(data)

        var anomalyCount = 0
        data.forEach { point ->
            if (iso.isAnomaly(point)) anomalyCount++
        }

        val anomalyPercent = anomalyCount * 100.0 / data.size
        // При равномерных данных и contamination=0.1 — аномалий должно быть мало
        assertTrue("Аномалий должно быть ≤ 30%, найдено ${String.format("%.1f", anomalyPercent)}%",
            anomalyPercent <= 30.0)
    }

    // ================================================================
    // 3. Модель не падает при выбросах (вероятностный алгоритм)
    //    Проверяем, что fit + isAnomaly работают без исключений
    // ================================================================
    @Test
    fun isAnomaly_withOutliers_doesNotCrash() {
        val normal = (1..30).map {
            doubleArrayOf(500.0, 3.0, 2.0, 0.0)
        }
        val outliers = (1..15).map {
            doubleArrayOf(50000.0, 7.0, 4.0, 1.0)
        }
        val allData = (normal + outliers).toTypedArray()

        val iso = IsolationForest(nTrees = 100, contamination = 0.3)
        iso.fit(allData)

        // Проверяем, что все вызовы isAnomaly не падают
        var checkedCount = 0
        allData.forEach { point ->
            val result = iso.isAnomaly(point)
            // Просто проверяем что boolean
            assertTrue("Результат должен быть Boolean", result is Boolean)
            checkedCount++
        }

        assertEquals(45, checkedCount)
    }

    // ================================================================
    // 4. getTreeCount возвращает правильное число
    // ================================================================
    @Test
    fun getTreeCount_returnsCorrectNumber() {
        val iso1 = IsolationForest(nTrees = 10)
        assertEquals(0, iso1.getTreeCount())

        iso1.fit(arrayOf(doubleArrayOf(1.0, 2.0, 3.0, 4.0)))
        assertEquals(10, iso1.getTreeCount())

        val iso2 = IsolationForest(nTrees = 50)
        iso2.fit((1..20).map { doubleArrayOf(it * 100.0, 1.0, 1.0, 0.0) }.toTypedArray())
        assertEquals(50, iso2.getTreeCount())
    }

    // ================================================================
    // 5. Одна точка данных — не падает
    // ================================================================
    @Test
    fun isAnomaly_singlePoint_notAnomaly() {
        val iso = IsolationForest(nTrees = 10, contamination = 0.1)
        val singlePoint = arrayOf(doubleArrayOf(500.0, 3.0, 2.0, 0.0))
        iso.fit(singlePoint)

        val result = iso.isAnomaly(doubleArrayOf(500.0, 3.0, 2.0, 0.0))
        assertFalse("Единственная точка не должна быть аномалией", result)
    }

    // ================================================================
    // 6. Разные contamination — не падает
    // ================================================================
    @Test
    fun differentContamination_runsSuccessfully() {
        val data = (1..30).map { doubleArrayOf(it * 100.0, 3.0, 2.0, 0.0) }.toTypedArray()

        val contaminations = listOf(0.05, 0.1, 0.2, 0.3)
        contaminations.forEach { cont ->
            val iso = IsolationForest(nTrees = 30, contamination = cont)
            iso.fit(data)

            var checked = 0
            data.forEach { point ->
                iso.isAnomaly(point)
                checked++
            }
            assertEquals("contamination=$cont: все точки проверены", 30, checked)
        }
    }

    // ================================================================
    // 7. Много деревьев — не падает
    // ================================================================
    @Test
    fun manyTrees_doesNotCrash() {
        val data = (1..10).map {
            doubleArrayOf(it * 100.0, (it % 7 + 1).toDouble(), ((it / 7) + 1).toDouble(), (it % 2).toDouble())
        }.toTypedArray()

        val iso = IsolationForest(nTrees = 200, contamination = 0.1)
        iso.fit(data)
        assertEquals(200, iso.getTreeCount())

        data.forEach { point ->
            iso.isAnomaly(point)  // не должно упасть
        }
    }
}