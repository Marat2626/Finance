package com.example.finances

import android.util.Log
import com.example.finances.DateExcel
import kotlin.math.*
import java.time.DayOfWeek

class TransactionAnalyzer {

    companion object {
        const val TAG = "BlackHole_ML"

        // Категории, которые НЕ анализируем
        val EXCLUDED_CATEGORIES = listOf(
            "ЖКХ", "Аптека", "Здоровье", "Налоги", "Страхование"
        )

        val DAYS_RU = mapOf(
            1 to "пн", 2 to "вт", 3 to "ср", 4 to "чт",
            5 to "пт", 6 to "сб", 7 to "вс"
        )
    }

    data class BlackHole(
        val category: String,
        val dayOfWeek: String,
        val avgAmount: Double,
        val normalMedian: Double,
        val ratio: Double,
        val monthlyLoss: Double,
        val yearlyLoss: Double,
        val anomalyCount: Int
    )

    /**
     * ГЛАВНЫЙ МЕТОД: принимает расходы → возвращает чёрные дыры
     */
    fun findBlackHoles(expenses: List<DateExcel>): List<BlackHole> {
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, " ЗАПУСК АНАЛИЗА «ЧЁРНЫХ ДЫР»")
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, " Всего транзакций: ${expenses.size}")

        if (expenses.isEmpty()) {
            Log.d(TAG, " Нет данных для анализа")
            return emptyList()
        }

        // Статистика по категориям
        val categoryStats = expenses.groupBy { it.category }
            .mapValues { (_, list) ->
                Triple(list.size, list.sumOf { it.amount }, list.map { it.amount }.average())
            }

        Log.d(TAG, "")
        Log.d(TAG, " КАТЕГОРИИ РАСХОДОВ:")
        Log.d(TAG, "┌─────────────────────────────────────────────────────────────┐")
        Log.d(TAG, "│ Категория                 │ Кол-во │   Сумма    │ Средний  │")
        Log.d(TAG, "├─────────────────────────────────────────────────────────────┤")
        categoryStats.forEach { (cat, stats) ->
            Log.d(TAG, "│ ${cat.padEnd(25)} │ ${stats.first.toString().padStart(6)} │ ${String.format("%9.0f", stats.second).padStart(9)}₽ │ ${String.format("%7.0f", stats.third).padStart(7)}₽ │")
        }
        Log.d(TAG, "└─────────────────────────────────────────────────────────────┘")

        // 1. Подготавливаем признаки для ML
        val featuresMap = prepareFeatures(expenses)
        Log.d(TAG, "Признаки подготовлены для ${featuresMap.size} категорий")

        // 2. Для каждой категории обучаем Isolation Forest
        val anomalies = mutableListOf<DateExcel>()

        featuresMap.forEach { (category, dataList) ->
            if (dataList.size >= 8) {
                Log.d(TAG, "")
                Log.d(TAG, "АНАЛИЗ КАТЕГОРИИ: $category (${dataList.size} транзакций)")

                val features = dataList.map { it.toDoubleArray() }.toTypedArray()

                // Isolation Forest: обучаем на данных категории
                val iso = IsolationForest(contamination = 0.15)
                iso.fit(features)
                Log.d(TAG, "   ├─ Isolation Forest обучен (${iso.getTreeCount()} деревьев)")

                // Находим аномалии
                var anomalyCount = 0
                dataList.forEachIndexed { index, data ->
                    if (iso.isAnomaly(features[index])) {
                        anomalies.add(data.original)
                        anomalyCount++
                        Log.d(TAG, "   ├─ АНОМАЛИЯ: ${data.original.date.toLocalDate()} | ${String.format("%.0f", data.amount)}₽ | ${data.original.description.take(30)}")
                    }
                }

                val anomalyPercent = (anomalyCount * 100.0 / dataList.size)
                Log.d(TAG, "   └─ Найдено аномалий: $anomalyCount (${String.format("%.1f", anomalyPercent)}%)")
            } else {
                Log.d(TAG, "Категория '$category' - недостаточно данных (${dataList.size} < 8)")
            }
        }

        Log.d(TAG, "")
        Log.d(TAG, "ВСЕГО АНОМАЛИЙ: ${anomalies.size}")

        // 3. Группируем аномалии в "чёрные дыры"
        val blackHoles = groupIntoBlackHoles(anomalies, expenses)

        Log.d(TAG, "")
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "НАЙДЕНО ЧЁРНЫХ ДЫР: ${blackHoles.size}")
        Log.d(TAG, "═══════════════════════════════════════")

        blackHoles.forEachIndexed { index, hole ->
            Log.d(TAG, "")
            Log.d(TAG, "ДЫРА #${index + 1}: ${hole.category}")
            Log.d(TAG, "   ├─ День недели: ${hole.dayOfWeek}")
            Log.d(TAG, "   ├─ Средний чек: ${String.format("%.0f", hole.avgAmount)}₽")
            Log.d(TAG, "   ├─ Норма: ${String.format("%.0f", hole.normalMedian)}₽")
            Log.d(TAG, "   ├─ Превышение: в ${String.format("%.1f", hole.ratio)} раза")
            Log.d(TAG, "   ├─ Аномальных транзакций: ${hole.anomalyCount}")
            Log.d(TAG, "   ├─ Потери в месяц: ${String.format("%.0f", hole.monthlyLoss)}₽")
            Log.d(TAG, "   └─ Потери в год: ${String.format("%.0f", hole.yearlyLoss)}₽")
        }

        if (blackHoles.isEmpty()) {
            Log.d(TAG, "«Чёрных дыр» не обнаружено")
        }

        Log.d(TAG, "")
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "🏁 АНАЛИЗ ЗАВЕРШЁН")
        Log.d(TAG, "═══════════════════════════════════════")

        return blackHoles
    }

    private fun prepareFeatures(expenses: List<DateExcel>): Map<String, List<FeatureData>> {
        val byCategory = expenses
            .filter { it.category !in EXCLUDED_CATEGORIES }
            .groupBy { it.category }

        val result = mutableMapOf<String, List<FeatureData>>()

        byCategory.forEach { (category, transactions) ->
            result[category] = transactions.map { tx ->
                FeatureData(
                    original = tx,
                    amount = tx.amount,
                    dayOfWeek = tx.date.dayOfWeek.value.toDouble(),  // 1-7
                    weekOfMonth = (tx.date.dayOfMonth / 7.0 + 1).toDouble(),  // 1-5
                    isWeekend = if (tx.date.dayOfWeek == DayOfWeek.SATURDAY ||
                        tx.date.dayOfWeek == DayOfWeek.SUNDAY) 1.0 else 0.0
                )
            }
        }

        return result
    }


    private fun groupIntoBlackHoles(
        anomalies: List<DateExcel>,
        allExpenses: List<DateExcel>
    ): List<BlackHole> {
        val blackHoles = mutableListOf<BlackHole>()

        val grouped = anomalies.groupBy {
            Pair(it.category, it.date.dayOfWeek.value)
        }

        grouped.forEach { (key, group) ->
            val (category, dayOfWeek) = key
            if (group.size >= 2) {  // Минимум 2 повторения
                val avgAmount = group.map { it.amount }.average()

                val normalTransactions = allExpenses.filter {
                    it.category == category && it.date.dayOfWeek.value == dayOfWeek
                }
                val normalMedian = median(normalTransactions.map { it.amount })

                if (normalMedian > 0) {
                    val ratio = avgAmount / normalMedian

                    if (ratio > 1.5) {  // Превышение более чем в 1.5 раза
                        val monthsOfData = maxOf(1.0,
                            allExpenses.size.toDouble() / 30.0)  // Примерно месяцев данных
                        val monthlyCount = group.size / monthsOfData
                        val monthlyLoss = (avgAmount - normalMedian) * monthlyCount

                        blackHoles.add(
                            BlackHole(
                                category = category,
                                dayOfWeek = DAYS_RU[dayOfWeek] ?: "?",
                                avgAmount = avgAmount,
                                normalMedian = normalMedian,
                                ratio = ratio,
                                monthlyLoss = monthlyLoss,
                                yearlyLoss = monthlyLoss * 12,
                                anomalyCount = group.size
                            )
                        )
                    }
                }
            }
        }

        return blackHoles.sortedByDescending { it.monthlyLoss }.take(3)
    }

    private fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }

    private data class FeatureData(
        val original: DateExcel,
        val amount: Double,
        val dayOfWeek: Double,
        val weekOfMonth: Double,
        val isWeekend: Double
    ) {
        fun toDoubleArray() = doubleArrayOf(amount, dayOfWeek, weekOfMonth, isWeekend)
    }
}

class IsolationForest(
    private val nTrees: Int = 100,
    private val contamination: Double = 0.1
) {
    companion object {
        private const val TAG = "IsolationForest"
    }

    private data class Node(
        val splitFeature: Int? = null,
        val splitValue: Double? = null,
        val left: Node? = null,
        val right: Node? = null,
        val size: Int = 0
    )

    private val trees = mutableListOf<Node>()
    private var threshold: Double = 0.0

    fun getTreeCount() = trees.size

    fun fit(data: Array<DoubleArray>) {
        trees.clear()
        val sampleSize = minOf(256, data.size)

        Log.d(TAG, "   ├─ Строим лес из $nTrees деревьев...")
        Log.d(TAG, "   ├─ Размер выборки: $sampleSize")
        Log.d(TAG, "   ├─ Размерность данных: ${data[0].size} признаков")

        for (i in 0 until nTrees) {
            val sample = if (data.size <= sampleSize) {
                data
            } else {
                data.toList().shuffled().take(sampleSize).toTypedArray()
            }
            trees.add(buildTree(sample, 0, ceil(log2(sampleSize.toDouble())).toInt()))
        }

        Log.d(TAG, "   ├─ Построено ${trees.size} деревьев")

        val scores = data.map { anomalyScore(it) }.sortedDescending()
        val cutoffIndex = (data.size * contamination).toInt()
        threshold = if (cutoffIndex < scores.size) scores[cutoffIndex] else scores.last()

        Log.d(TAG, "   └─ Порог аномальности: ${String.format("%.3f", threshold)}")
    }

    fun isAnomaly(features: DoubleArray): Boolean {
        val score = anomalyScore(features)
        return score > threshold
    }

    private fun buildTree(data: Array<DoubleArray>, height: Int, heightLimit: Int): Node {
        if (height >= heightLimit || data.size <= 1) {
            return Node(size = data.size)
        }

        val nFeatures = data[0].size
        val splitFeature = (0 until nFeatures).random()
        val values = data.map { it[splitFeature] }
        val minVal = values.minOrNull()!!
        val maxVal = values.maxOrNull()!!

        if (minVal == maxVal) return Node(size = data.size)

        val splitValue = minVal + Math.random() * (maxVal - minVal)

        val leftData = data.filter { it[splitFeature] < splitValue }.toTypedArray()
        val rightData = data.filter { it[splitFeature] >= splitValue }.toTypedArray()

        return Node(
            splitFeature = splitFeature,
            splitValue = splitValue,
            left = buildTree(leftData, height + 1, heightLimit),
            right = buildTree(rightData, height + 1, heightLimit),
            size = data.size
        )
    }

    private fun anomalyScore(point: DoubleArray): Double {
        if (trees.isEmpty()) return 0.0
        val avgPathLength = trees.map { pathLength(point, it, 0) }.average()
        return 2.0.pow(-avgPathLength / c(256.0))
    }

    private fun pathLength(point: DoubleArray, node: Node?, currentHeight: Int): Double {
        if (node?.splitFeature == null) {
            return currentHeight + c(node?.size?.toDouble() ?: 0.0)
        }

        return if (point[node.splitFeature] < node.splitValue!!) {
            pathLength(point, node.left, currentHeight + 1)
        } else {
            pathLength(point, node.right, currentHeight + 1)
        }
    }

    private fun c(n: Double): Double {
        if (n <= 1) return 0.0
        return 2.0 * (ln(n - 1.0) + 0.5772156649) - (2.0 * (n - 1.0) / n)
    }
}