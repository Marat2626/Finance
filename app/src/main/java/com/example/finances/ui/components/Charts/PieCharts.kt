package com.example.finances


import android.graphics.Color
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.finances.utils.AppColors
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter


class PieCharts {


    @Composable
    fun Chart(
        transactions: List<DateExcel>,   // ← теперь принимаем готовый список
        centerText: String = "ИТОГ",
        modifier: Modifier = Modifier
    ) {
        // Группируем по типу операции
        val entriesData = remember(transactions) {
            transactions
                .groupBy { it.typeOfOperation }
                .mapValues { (_, list) -> list.sumOf { it.amount }.toFloat() }
                .toList()
        }

        val typeDisplayNames = mapOf(
            "Расходы" to "Потрачено",
            "Зачисления" to "Заработано",
            "Накопление" to "Накоплено"
        )


        val pieEntries = remember(entriesData) {
            val total = entriesData.sumOf { it.second.toDouble() }.toFloat()
            entriesData.map { (type, sum) ->
                val displayName = typeDisplayNames[type] ?: type
                val percent = if (total > 0) (sum / total * 100).toInt() else 0
                PieEntry(sum, "$displayName $percent%") // «Потрачено 45%»
            }
        }
        val diagramText = AppColors.current.text
        val cardBox = AppColors.current.cardBox
        AndroidView(
            factory = { context ->
                PieChart(context).apply {
                    description.isEnabled = false
                    isRotationEnabled = true
                    setUsePercentValues(true)
                    setEntryLabelColor(Color.TRANSPARENT)


                    legend.apply {
                        isEnabled = true

                        // Вертикальное расположение (сверху вниз)
                        verticalAlignment = Legend.LegendVerticalAlignment.TOP
                        horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                        orientation = Legend.LegendOrientation.VERTICAL

                        // Отступы от краёв
                        xOffset = 9f
                        yOffset = 120f

                        // Размер текста
                        textSize = 12f
                        textColor = diagramText.toArgb()


                        // ✅ КРУГЛЫЕ ТОЧКИ ВМЕСТО КВАДРАТОВ
                        form = Legend.LegendForm.CIRCLE
                        formSize = 10f  // Размер кружка

                        // Отступ между элементами легенды
                        yEntrySpace = 1f

                    }

                    setHoleRadius(60f)
                    setHoleColor(cardBox.toArgb())


//                    this.centerText = centerText
//                    setCenterTextSize(26f)
//                    setCenterTextColor(diagramText.toArgb())


                    animateY(200)
                }
            },
            update = { pieChart ->
                if (pieEntries.isEmpty()) {
                    pieChart.data = null
                    pieChart.invalidate()
                    return@AndroidView
                }


                val dataSet = PieDataSet(pieEntries, "").apply {
                    colors = entriesData.map { (type, _) ->
                        when (type) {
                            "Расходы" -> Color.parseColor("#510F18")
                            "Зачисления" -> Color.parseColor("#12314D")
                            "Накопление" -> Color.parseColor("#C0933D")
                            else -> Color.GRAY
                        }
                    }
                    sliceSpace = 3f
                    setDrawValues(false)
                }

                val pieData = PieData(dataSet).apply {
                    setValueFormatter(object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return "${value.toInt()}%"
                        }
                    })
                }


                pieChart.data = pieData
                pieChart.invalidate()
            },
            modifier = modifier
        )

        Spacer(modifier = Modifier.height(60.dp))
    }
}
