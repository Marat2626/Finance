package com.example.finances

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.flow.Flow
import kotlin.collections.component1
import kotlin.collections.component2


class PieCharts {

    @Composable
    fun Chart(db: DataTransactions, text: String, modifier: Modifier, transactionsFlow: Flow<List<DateExcel>>) {


        val transactions by transactionsFlow.collectAsState(initial = emptyList())

        val entriesData = remember(transactions){

            transactions.groupBy { it.typeOfOperation }.map { (type, amount) ->
                val sum = amount.sumOf { it.amount }.toFloat()
                type to sum

            }
        }

        val entries = remember(entriesData) {
            entriesData.map { (_, sum) ->
                PieEntry(sum, "")
            }
        }

        AndroidView(
            factory = { context ->
                PieChart(context).apply {
                    description.isEnabled = false
                    isRotationEnabled = true
                    setUsePercentValues(true)

                    legend.isEnabled = true
                    legend.textColor = android.graphics.Color.WHITE
                    legend.textSize = 18f
                    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                    legend.verticalAlignment = Legend.LegendVerticalAlignment.CENTER
                    legend.orientation = Legend.LegendOrientation.VERTICAL
                    legend.xOffset = -19f
                    legend.yOffset = -40f
                    legend.form = Legend.LegendForm.CIRCLE

                    animateY(1500)
                    centerText = text
                    setCenterTextSize(26f)
                    setCenterTextColor(android.graphics.Color.WHITE)

                    setHoleRadius(60f)
                    setHoleColor(android.graphics.Color.parseColor("#2D447E"))


                }
            },

            update = { pieChart ->

                val pieEntries = entriesData.map { (type, sum) ->
                    PieEntry(sum, type)  // Пустая строка, чтобы не влиять на метки
                }
                val dataSet = PieDataSet(pieEntries,"").apply {


                    val colorList = entriesData.map { (type, _) ->

                        when (type) {
                            "Расходы" -> android.graphics.Color.parseColor("#FF6B6B")    // яркий кораллово-красный
                            "Зачисления" -> android.graphics.Color.parseColor("#6B9CFF")  // яркий васильковый
                            "Накопление" -> android.graphics.Color.parseColor("#6BCB6B")  // яркий травянисто-зеленый
                            else -> android.graphics.Color.parseColor("#969696")
                        }
                    }
                    setDrawValues(true)

                    this.colors = colorList

                    sliceSpace = 2f
                }


                val pieData = PieData(dataSet)


                pieData.setValueFormatter(object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "${value.toInt()} %"
                    }
                })

                pieChart.data = pieData

                pieChart.legend.isEnabled = true
                pieChart.invalidate()



            },
            modifier = modifier

        )

    }
    val colorList = listOf(
        android.graphics.Color.rgb(255, 0, 0),      // Красный
        android.graphics.Color.rgb(255, 255, 0),    // Желтый
        android.graphics.Color.rgb(0, 255, 0),      // Зеленый
        android.graphics.Color.rgb(0, 255, 128),    // Зелено-голубой
        android.graphics.Color.rgb(0, 255, 255),    // Голубой
        android.graphics.Color.rgb(0, 128, 255),    // Синий
        android.graphics.Color.rgb(0, 0, 255),      // Синий чистый
        android.graphics.Color.rgb(128, 0, 255),    // Фиолетовый
        android.graphics.Color.rgb(255, 0, 255),    // Пурпурный
        android.graphics.Color.rgb(255, 0, 128),    // Розово-красный
        android.graphics.Color.rgb(255, 128, 128),  // Светло-красный
        android.graphics.Color.rgb(255, 255, 128),  // Светло-желтый
        android.graphics.Color.rgb(128, 255, 255)   // Светло-голубой
    )
}

