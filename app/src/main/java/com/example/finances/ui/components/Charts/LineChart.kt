package com.example.finances

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.time.format.DateTimeFormatter

@Composable
fun TransactionsLineChart(
    transactions: List<DateExcel>,
    modifier: Modifier = Modifier
) {
    if (transactions.isEmpty()) {
        return
    }

    val chartData = remember(transactions) {
        val dateFormatter = DateTimeFormatter.ofPattern("dd.MM")

        transactions
            .groupBy { it.date.format(dateFormatter) }
            .map { (date, list) ->
                val total = list.sumOf { it.amount }.toFloat()
                date to total
            }
            .sortedBy { it.first }
    }

    val entries = chartData.mapIndexed { index, (_, amount) ->
        Entry(index.toFloat(), amount)
    }

    val labels = chartData.map { it.first }
    Card(
        modifier = modifier
            .height(200.dp)
            .width(340.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false

                // Настройка осей
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = Color.WHITE
                    textSize = 12f
                    granularity = 1f
                    setDrawGridLines(false)  // убираем сетку по X
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val index = value.toInt()
                            return if (index >= 0 && index < labels.size) {
                                labels[index]
                            } else {
                                ""
                            }
                        }
                    }
                }

                axisLeft.apply {
                    textColor = Color.WHITE
                    textSize = 12f
                    gridColor = android.graphics.Color.parseColor("#2D447E")  // синяя сетка
                    gridLineWidth = 0.5f
                    axisLineColor = android.graphics.Color.parseColor("#2D447E")
                }

                axisRight.isEnabled = false

                // Настройка легенды
                legend.isEnabled = true
                legend.textColor = Color.WHITE
                legend.formSize = 12f
                legend.form = Legend.LegendForm.CIRCLE

                // Анимация
                animateX(1500)

                // Интерактивность
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)

                // Фон
                setBackgroundColor(android.graphics.Color.parseColor("#1E2A47"))
            }
        },
        update = { lineChart ->
            if (entries.isNotEmpty()) {
                val dataSet = LineDataSet(entries, "Динамика транзакций").apply {
                    // Основная линия - яркий синий
                    color = android.graphics.Color.parseColor("#6B9CFF")

                    // Текст значений
                    valueTextColor = Color.WHITE
                    valueTextSize = 8f

                    // Толщина линии
                    lineWidth = 2f

                    // Кружки на точках
                    circleRadius = 5f
                    circleColors = listOf(android.graphics.Color.parseColor("#FFD700"))  // золотые точки
                    circleHoleColor = android.graphics.Color.parseColor("#2D447E")
                    setDrawCircleHole(true)

                    // Градиентная заливка под линией
                    setDrawFilled(true)
                    fillColor = android.graphics.Color.parseColor("#2D447E")
                    fillAlpha = 180  // прозрачность

                    // Сглаживание
                    mode = LineDataSet.Mode.CUBIC_BEZIER

                    // Толщина обводки кружков
                    circleHoleRadius = 2f
                }

                val lineData = LineData(dataSet)
                lineChart.data = lineData
            }
            lineChart.invalidate()
        },
        modifier = modifier
            .height(200.dp)
            .width(340.dp)
    )
        }
}