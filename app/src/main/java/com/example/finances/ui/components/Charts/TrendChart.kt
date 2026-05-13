package com.example.finances

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finances.utils.AppColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun YearlyIncomeTrendChart(
    transactions: List<DateExcel>,
    modifier: Modifier = Modifier
) {
    // Получаем данные по доходам за последние 12 месяцев
    val monthlyData = remember(transactions) {
        val now = LocalDate.now()
        val last12Months = (0..11).map { offset ->
            val date = now.minusMonths(offset.toLong())
            val income = transactions
                .filter {
                    it.typeOfOperation == "Зачисления" &&
                            it.date.year == date.year &&
                            it.date.monthValue == date.monthValue
                }
                .sumOf { it.amount }
            MonthData(
                month = date.monthValue,
                year = date.year,
                monthName = date.format(DateTimeFormatter.ofPattern("MMM")),
                income = income,
                date = date
            )
        }.reversed()
        last12Months
    }

    if (monthlyData.isEmpty()) return

    // Статистика
    val incomes = monthlyData.map { it.income }
    val maxIncome = incomes.maxOrNull() ?: 1.0
    val minIncome = incomes.minOrNull() ?: 0.0
    val avgIncome = incomes.average()
    val lastIncome = incomes.lastOrNull() ?: 0.0
    val firstIncome = incomes.firstOrNull() ?: 0.0
    val yearChangePercent = if (firstIncome > 0) {
        ((lastIncome - firstIncome) / firstIncome * 100).toInt()
    } else if (lastIncome > 0) 100 else 0

    val trendDirection = when {
        yearChangePercent > 10 -> "▲"
        yearChangePercent < -10 -> "▼"
        else -> "●"
    }

    val trendColor = when {
        yearChangePercent > 0 -> Color(0xFF6BCB6B)
        yearChangePercent < 0 -> Color(0xFFFF6B6B)
        else -> Color(0xFFFFD700)
    }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Заголовок
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Динамика\nдоходов",
                        style = TextStyle(AppColors.current.text),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Изменение за год
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp, top = 4.dp)
                ) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${if (yearChangePercent > 0) "+" else ""}$yearChangePercent%",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(AppColors.current.text),
                        color = trendColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Мини-график
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                MiniLineChart(
                    data = incomes,
                    height = 80f,
                    lineColor = trendColor,
                    gradientStart = trendColor.copy(alpha = 0.3f),
                    gradientEnd = trendColor.copy(alpha = 0.05f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Подписи месяцев (только последние 6)
            val last6Months = if (monthlyData.size >= 6) monthlyData.takeLast(6) else monthlyData
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                last6Months.forEach { data ->
                    Text(
                        text = data.monthName,
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Итоговая строка
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ср. доход: ${avgIncome.toInt().formatWithSpace()} ₽",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = "Текущий: ${lastIncome.toInt().formatWithSpace()} ₽",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = trendColor
                )
            }
        }
}

@Composable
fun MiniLineChart(
    data: List<Double>,
    height: Float,
    lineColor: Color,
    gradientStart: Color,
    gradientEnd: Color
) {
    if (data.isEmpty()) return

    val maxValue = data.maxOrNull() ?: 1.0
    val minValue = data.minOrNull() ?: 0.0
    val range = if (maxValue == minValue) 1.0 else maxValue - minValue

    val points = remember(data, maxValue, minValue, range) {
        data.mapIndexed { index, value ->
            val x = if (data.size > 1) {
                (index.toFloat() / (data.size - 1)) * 100f
            } else {
                50f
            }
            val y = if (range > 0) {
                ((maxValue - value) / range).toFloat() * height
            } else {
                height / 2
            }
            Offset(x, y)
        }
    }

    androidx.compose.foundation.Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        if (points.size < 2) return@Canvas

        // Рисуем заливку под линией
        val fillPath = Path().apply {
            moveTo(0f, size.height)
            points.forEach { point ->
                lineTo(point.x * size.width / 100f, point.y)
            }
            lineTo(size.width, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(gradientStart, gradientEnd),
                startY = 0f,
                endY = size.height
            )
        )

        // Рисуем линию
        val linePath = Path().apply {
            moveTo(points.first().x * size.width / 100f, points.first().y)
            points.forEach { point ->
                lineTo(point.x * size.width / 100f, point.y)
            }
        }
        drawPath(
            path = linePath,
            color = lineColor,
//            strokeWidth = 2f,
        )

        // Рисуем точки на графике
        points.forEachIndexed { index, point ->
            val isLast = index == points.size - 1
            drawCircle(
                color = if (isLast) Color(0xFFFFD700) else lineColor,
                radius = if (isLast) 4f else 2.5f,
                center = Offset(point.x * size.width / 100f, point.y)
            )
        }
    }
}

data class MonthData(
    val month: Int,
    val year: Int,
    val monthName: String,
    val income: Double,
    val date: LocalDate
)