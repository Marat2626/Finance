package com.example.finances

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items  // ← ДОБАВИТЬ ИМПОРТ
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finances.utils.AppColors
import com.example.finances.utils.cardShadow


data class CategoryData(
    val name: String,      // Название: "Рестораны"
    val icon: String,      // Иконка: "🍔"
    val amount: Double,    // Сумма: 32100.0
    val color: Color       // Цвет: Color(0xFFE94F4F)
)


@Composable
fun TopCategoriesChart( transactions: List<DateExcel>,          // ← принимаем транзакции извне
) {


    val chartColors = listOf(
        Color(0xFF510F18),
        Color(0xFFC0933D),
        Color(0xFF12314D),
        Color(0xFF007B2D),
        Color(0xFF795548),
        Color(0xFFE91E63), // розовый
        Color(0xFFFF9800), // оранжевый
        Color(0xFF00BCD4), // голубой
        Color(0xFF4CAF50), // зелёный
        Color(0xFF9C27B0), // пурпурный
        Color(0xFFFF5722), // глубокий оранжевый
    )

    // Группируем по категориям и считаем сумму для каждой
    val categoriesWithAmount = remember(transactions) {
        transactions
            .groupBy { it.category }           // Группируем по category
            .map { (category, list) ->         // Для каждой группы
                CategoryData(
                    name = category,
                    icon = getCategoryIcon(category),
                    amount = list.sumOf { it.amount },  // Суммируем amount
                    color = getCategoryColor(category)
                )
            }
            .sortedByDescending { it.amount }  // Сортируем по убыванию

    }

    val dateNow = transactions.map { it.date.month }
    if (categoriesWithAmount.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Нет данных о расходах", color = Color.Gray)
        }
        return
    }

    val text = AppColors.current.text
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .cardShadow()

            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 6.dp)
            .border(BorderStroke(2.dp, AppColors.current.borderLiner), shape = RoundedCornerShape(25.dp))
            .background(AppColors.current.cardBox,  shape = RoundedCornerShape(25.dp))
        ,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)

        ) {


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    Icon(
                        painter = painterResource(id = R.drawable.graf),
                        modifier = Modifier
                            .size(20.dp),
                        contentDescription = "график",
                        tint = AppColors.current.icones

                    )
                    Spacer(modifier = Modifier.width(5.dp))


                    Text(" ТОП КАТЕГОРИЙ",
                        fontWeight = FontWeight.SemiBold,
                        color = text)
                }
                Text("${dateNow[0]} " , color = text)
            }

            Spacer(modifier = Modifier.height(12.dp))

            val maxAmount = categoriesWithAmount.maxOfOrNull { it.amount } ?: 1.0
            val maxBarHeight = 100.dp


            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {


                itemsIndexed(categoriesWithAmount) { index, category ->

                    val color = chartColors[index % chartColors.size]
                    val barHeight = (category.amount / maxAmount * maxBarHeight.value).dp

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        // ✅ Контейнер, который выравнивает всё по низу
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(maxBarHeight),  // ← Фиксированная высота для всех
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Внутри — столбец + текст над ним
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(60.dp)
                            ) {
                                // Текст с суммой
                                Text(
                                    text = "₽${(category.amount / 1000).toInt()}K",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = text
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Столбец
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(barHeight)
                                        .background(color, RoundedCornerShape(9.dp))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Подпись
                        // Подпись
                        Column (
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {

                            val displayText = category.name.replace(" ", "\n")
                            Text(
                                text = displayText,
                                fontSize = 10.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Visible,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                color = text,
                                softWrap = false,
                                style = LocalTextStyle.current.copy(
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            }
        }
    }

}

fun getCategoryIcon(category: String): String {
    return when (category) {
        "Рестораны", "Кафе", "Фастфуд" -> "🍔"
        "Продукты", "Супермаркеты" -> "🛒"
        "Транспорт", "Такси", "Метро" -> "🚗"
        "Развлечения", "Игры", "Кино" -> "🎮"
        "Здоровье", "Аптека", "Врачи" -> "💊"
        "Одежда", "Шоппинг" -> "👕"
        "Красота", "Салон" -> "💄"
        "Дом", "Ремонт" -> "🏠"
        "Связь", "Интернет", "Телефон" -> "📱"
        "Образование", "Курсы", "Книги" -> "📚"
        else -> "📦"
    }
}

fun getCategoryColor(category: String): Color {
    return when (category) {
        "Рестораны", "Кафе", "Фастфуд" -> Color(0xFF510F18)
        "Продукты", "Супермаркеты" -> Color(0xFFC0933D)
        "Транспорт", "Такси", "Метро" -> Color(0xFF12314D)
        "Развлечения", "Игры", "Кино" -> Color(0xFF007B2D)
        "Здоровье", "Аптека", "Врачи" -> Color(0xFF510F18)
        "Одежда", "Шоппинг" -> Color(0xFF618F7A)
        "Красота", "Салон" -> Color(0xFF510F18)
        "Дом", "Ремонт" -> Color(0xFF795548)
        "Связь", "Интернет", "Телефон" -> Color(0xFF2196F3)
        "Образование", "Курсы", "Книги" -> Color(0xFF4CAF50)
        else -> Color(0xFF95A5A6)
    }
}