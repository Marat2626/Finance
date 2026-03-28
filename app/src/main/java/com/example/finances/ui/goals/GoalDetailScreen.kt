package com.example.finances

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import com.example.finances.utils.AppColors

import java.time.LocalDateTime
import kotlin.Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: Int,
    onBack: () -> Unit,
    viewModel: GoalViewModel = viewModel(),
    onClipAddOperation: () -> Unit,
    onHomeClick: () -> Unit,
    onClipAddGoal: () -> Unit,
    onSettingScreen: () -> Unit,
    onAI: () -> Unit
) {
    val goals by viewModel.transactions.collectAsState()
    val context = LocalContext.current
    val app = context.applicationContext as Application

    val savingsViewModel: EISviewModel = viewModel(
        key = "savings",
        factory = EISViewModelFactory(app, { it.getAllSave() }, "Накопление")
    )
    val savings by savingsViewModel.getCategory().collectAsState(initial = emptyList())

    LaunchedEffect(goalId) {
        viewModel.loadGoalsFromDb()
    }

    val goal = remember(goals, goalId) { goals.find { it.id == goalId } }

    val currentSaved = remember(goal, savings) {
        if (goal != null) {
            savings.filter { saving ->
                saving.description.contains(goal.name, ignoreCase = true) &&
                        saving.date.isAfter(goal.createdDate)  // ← ВОТ ЭТО!
            }.sumOf { it.amount }
        } else 0.0
    }
    val db = DataTransactions.getDatabase(context)

    val predictor = remember { goalProgressPredictor(db) }  // db из DataTransactions


    var prediction by remember { mutableStateOf<goalProgressPredictor.PredictionResult?>(null) }


    LaunchedEffect(goal) {
        if (goal != null) {
            prediction = predictor.predict(goal)
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.screenBlue)
    ) {
        // Верхняя панель с кнопкой назад
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color(0xFFD4AF37)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }


        if (goal == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Цель не найдена",
                    style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 18.sp)
                )
            }


        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(remember { ScrollState(0) }),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CompactGoalCard(goal = goal, currentSaved = currentSaved)



                // === ПРОГНОЗ УСПЕХА — САМОЕ ВИДИМОЕ МЕСТО ===
                if (prediction != null) {
                    PredictionCard(prediction = prediction!!)
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    // Заглушка пока прогноз грузится
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2F4A))
                    ) {
                        Text(
                            text = "Расчёт прогноза...",
                            modifier = Modifier.padding(16.dp),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MonthlyPaymentCard(
                        goal = goal,
                        currentSaved = currentSaved,
                        monthlyAmount = viewModel.calculateMonthlySavings(goal),
                        modifier = Modifier.weight(1f)
                    )
                    ProgressChartCard(
                        goal = goal,
                        currentSaved = currentSaved,
                        modifier = Modifier.weight(1f)
                    )
                }


                Spacer(modifier = Modifier.height(24.dp))


                GoalGrowthChart(
                    goal = goal,
                    savings = savings,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )


                Spacer(modifier = Modifier.height(16.dp))


                // История накоплений (оставляем как было)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "История накоплений",
                        style = TextStyle(
                            brush = AppColors.royalGoldGradientV,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Все →",
                        style = TextStyle(
                            color = Color(0xFFD4AF37).copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    )
                }


                Spacer(modifier = Modifier.height(12.dp))


                val goalSavings = remember(savings, goal.name, goal.createdDate) {
                    savings.filter { saving ->
                        saving.description.contains(goal.name, ignoreCase = true) &&
                                saving.date.isAfter(goal.createdDate)
                    }.sortedByDescending { it.date }
                }


                if (goalSavings.isEmpty()) {
                    EmptySavingsPlaceholder()
                } else {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(goalSavings.take(5)) { saving ->
                            EnhancedSavingCard(saving = saving)
                        }
                    }
                }


                Spacer(modifier = Modifier.height(100.dp)) // место для нижней навигации
            }
        }

        // Нижняя навигация
        bottomNavigationBar(
            { onHomeClick()},
            {  onClipAddOperation() },
            { onClipAddGoal()  },
            { onSettingScreen()},
            {onAI()}
        )
    }
}
@Composable
fun CompactGoalCard(
    goal: DataGoal,
    currentSaved: Double,
) {
    val savedAmount = goal.savedAmount
    val targetAmount = goal.amount ?: 0.0
    val progress = if (targetAmount > 0) {
        (currentSaved / targetAmount).toFloat().coerceIn(0f, 1f)  // ← используем currentSaved
    } else 0f
    val percentComplete = (progress * 100).toInt()

//        var manualMonth by remember { mutableStateOf(5) }
//    val monthsPassed = remember(goal.createdDate) {
//        manualMonth // ← вместо getMonthsPassed используем manualMonth
//    }

    val monthsPassed = remember(goal.createdDate) {
        getMonthsPassed(goal.createdDate)
    }
    val remainingMonths = remember(goal.createdDate, goal.monthsCount) {
        (goal.monthsCount - monthsPassed).coerceAtLeast(0)
    }
    val timePassed = when {
        monthsPassed == 0 -> "Только начали"
        monthsPassed == 1 -> "1 месяц"
        monthsPassed < 5 -> "$monthsPassed месяца"
        else -> "$monthsPassed месяцев"
    }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .background(AppColors.radialBlueGradient1),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent  // прозрачный, чтобы видеть градиент
        ),
        border = BorderStroke(1.dp, brush = AppColors.royalGoldGradientL),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Верхний ряд: эмодзи и название
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Эмодзи цели
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            Color(0x33D4AF37),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFFD4AF37).copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            goal.name.contains("машина", ignoreCase = true) -> "🚗"
                            goal.name.contains("квартира", ignoreCase = true) -> "🏠"
                            goal.name.contains("путешествие", ignoreCase = true) -> "✈️"
                            goal.name.contains("телефон", ignoreCase = true) -> "📱"
                            goal.name.contains("образование", ignoreCase = true) -> "📚"
                            else -> "🎯"
                        },
                        fontSize = 22.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Название
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = goal.name.toUpperCase(),
                        style = TextStyle(
                            brush = AppColors.royalGoldGradientL,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${String.format("%,.0f", targetAmount)} ₽",
                        style = TextStyle(
                            brush = AppColors.royalGoldGradientV,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Процент
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color(0x336BCB6B),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$percentComplete%",
                        style = TextStyle(
                            color = Color(0xFF6BCB6B),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Прогресс-бар
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF6BCB6B),
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Информация о времени
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(
                    icon = "📅",
                    text = "Прошло: $timePassed",
                    color = Color(0xFFD4AF37)
                )

                InfoChip(
                    icon = "⏳",
                    text = "Осталось: $remainingMonths мес",
                    color = if (remainingMonths > 0) Color(0xFFFF6B6B) else Color(0xFF6BCB6B)
                )
            }
        }
    }
}

@Composable
fun InfoChip(icon: String, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = icon, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = TextStyle(
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun MonthlyPaymentCard(
    goal: DataGoal,
    currentSaved: Double,
    monthlyAmount: Double,
    modifier: Modifier = Modifier
) {
    val targetAmount = goal.amount ?: 0.0
    val remaining = targetAmount - currentSaved
    val monthsLeft = goal.monthsCount
    val recommendedMonthly = if (monthsLeft > 0) remaining / monthsLeft else 0.0

    Card(
        modifier = modifier
            .height(80.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.2f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E2F4A)
        ),
        border = BorderStroke(1.dp, brush = AppColors.royalGoldGradientL),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Ежемесячный взнос",
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            )

            Text(
                text = "${String.format("%,.0f", goal.monthlyPayment)} ₽",
                style = TextStyle(
                    brush = AppColors.royalGoldGradientV,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = "рекомендовано",
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp
                )
            )
        }
    }
}


@Composable
fun ProgressChartCard(
    goal: DataGoal,
    currentSaved: Double,
    modifier: Modifier = Modifier
) {
    val targetAmount = goal.amount ?: 0.0

//
//    var manualMonth by remember { mutableStateOf(5) }
//    val monthsPassed = remember(goal.createdDate) {
//        manualMonth // ← вместо getMonthsPassed используем manualMonth
//    }
    // Получаем прошедшие месяцы
    val monthsPassed = remember(goal.createdDate) {
        getMonthsPassed(goal.createdDate)
    }

    // Сколько нужно платить в месяц
    val monthlyPayment = if (goal.monthsCount > 0) {
        targetAmount / goal.monthsCount
    } else 0.0

    val currentMonthNeeded = monthlyPayment

    // Сколько уже должно было быть накоплено к началу этого месяца
    val shouldHaveByStartOfMonth = monthlyPayment * (monthsPassed - 1).coerceAtLeast(0)

    // Сколько внесено сверх плана (переплата)
    val overpayment = (currentSaved - shouldHaveByStartOfMonth - currentMonthNeeded).coerceAtLeast(0.0)

    // Сколько реально нужно внести в этом месяце (с учетом переплаты)
    val actualNeededThisMonth = (currentMonthNeeded - overpayment).coerceAtLeast(0.0)

    // Сколько уже внесено в этом месяце (без учета переплаты)
    val paidThisMonth = if (monthsPassed > 0) {
        (currentSaved - shouldHaveByStartOfMonth).coerceIn(0.0, currentMonthNeeded)
    } else {
        currentSaved.coerceIn(0.0, currentMonthNeeded)
    }

    // Процент оплаты текущего месяца
    // Процент оплаты текущего месяца
    val monthProgress = if (actualNeededThisMonth > 0) {
        ((paidThisMonth / actualNeededThisMonth) * 100).toInt().coerceIn(0, 100)
    } else 100

// Определяем статус (максимум 2 слова)
    val (statusText, statusColor) = when {
        // Цель достигнута
        currentSaved >= targetAmount -> Pair("Цель достигнута", Color(0xFF6BCB6B))

        // Если месяц уже должен быть закрыт (прошло месяцев и сумма меньше плана)
        monthsPassed > 0 && currentSaved < monthlyPayment * monthsPassed -> {
            val neededForPastMonths = monthlyPayment * monthsPassed - currentSaved
            when {
                neededForPastMonths > monthlyPayment -> Pair("Долг за ${monthsPassed} мес", Color(0xFFFF6B6B))
                else -> Pair("Долг ${String.format("%,.0f", neededForPastMonths)} ₽", Color(0xFFFF6B6B))
            }
        }

        // Есть переплата на следующий месяц
        overpayment > 0 && paidThisMonth >= actualNeededThisMonth ->
            Pair("Есть переплата", Color(0xFF6BCB6B))

        // Проверяем проценты ТЕКУЩЕГО месяца
        monthProgress == 0 -> Pair("Требуется оплата", Color(0xFFFF6B6B))
        monthProgress < 30 -> Pair("Критично мало", Color(0xFFFF6B6B))
        monthProgress < 70 -> Pair("Не хватает", Color(0xFFFFD700))
        monthProgress < 100 -> Pair("Почти готово", Color(0xFF6BCB6B))

        // Месяц оплачен полностью
        paidThisMonth >= actualNeededThisMonth -> Pair("Месяц закрыт", Color(0xFF6BCB6B))

        else -> Pair("Месяц закрыт", Color(0xFF6BCB6B))
    }

// Текст для отображения
    val displayText = when {
        // Если есть долг за прошлые месяцы
        monthsPassed > 0 && currentSaved < monthlyPayment * monthsPassed -> {
            val needed = monthlyPayment * monthsPassed - currentSaved
            "Долг: ${String.format("%,.0f", needed)} ₽"
        }
        // Если есть переплата
        overpayment > 0 && paidThisMonth >= actualNeededThisMonth -> {
            "Переплата: ${String.format("%,.0f", overpayment)} ₽"
        }
        // Обычный месяц
        else -> {
            "${String.format("%,.0f", paidThisMonth)}/${String.format("%,.0f", actualNeededThisMonth)} ₽"
        }
    }


    Card(
        modifier = modifier
            .height(80.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.2f)
            )
            .background(AppColors.radialBlueGradient1),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(1.dp, brush = AppColors.royalGoldGradientL),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Круговая диаграмма
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(
                                statusColor,
                                statusColor.copy(alpha = 0.2f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(3.dp)
                    .background(Color(0xFF1E2F4A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$monthProgress%",
                    style = TextStyle(
                        color = statusColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Статус
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = statusText,
                    style = TextStyle(
                        color = statusColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = displayText,
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}
@Composable
fun EmptySavingsPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(80.dp)

            .background(
                AppColors.radialBlueGradient1,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                brush = AppColors.royalGoldGradientL,
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "💰",
                fontSize = 24.sp
            )
            Text(
                text = "Пока нет накоплений",
                style = TextStyle(
                    brush = AppColors.royalGoldGradientV,
                    fontSize = 14.sp
                )
            )
            Text(
                text = "Начните копить прямо сейчас",
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            )
        }
    }
}

@Composable
fun EnhancedSavingCard(saving: DateExcel) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    Card(
        modifier = Modifier
            .width(150.dp)
            .height(80.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.2f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E2F4A)
        ),
        border = BorderStroke(1.dp, brush = AppColors.royalGoldGradientL),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                // Дата
                Text(
                    text = saving.date.format(dateFormatter),
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Описание (обрезанное)
                Text(
                    text = saving.description.take(10) + if (saving.description.length > 10) "..." else "",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            }

            // Сумма с плюсом
            Box(
                modifier = Modifier
                    .background(
                        Color(0x336BCB6B),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "+${String.format("%,.0f", saving.amount)}",
                    style = TextStyle(
                        color = Color(0xFF6BCB6B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
@Composable
fun GoalGrowthChart(
    goal: DataGoal,
    savings: List<DateExcel>,
    modifier: Modifier = Modifier
) {
    // Подготавливаем данные для графика
    val chartData = remember(goal, savings) {
        val result = mutableListOf<Pair<LocalDateTime, Double>>()

        // Стартовая точка - дата создания цели, сумма 0
        result.add(Pair(goal.createdDate, 0.0))

        // Сортируем накопления по дате
        val sortedSavings = savings
            .filter { it.description.contains(goal.name, ignoreCase = true) }
            .sortedBy { it.date }

        // Добавляем каждое накопление
        var cumulative = 0.0
        sortedSavings.forEach { transaction ->
            cumulative += transaction.amount
            result.add(Pair(transaction.date, cumulative))
        }

        result
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E2F4A)
        ),
        border = BorderStroke(1.dp, brush = AppColors.royalGoldGradientL),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Заголовок с итоговой суммой
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Рост накоплений",
                    style = TextStyle(
                        brush = AppColors.royalGoldGradientV,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                val total = chartData.lastOrNull()?.second ?: 0.0
                Text(
                    text = "${String.format("%,.0f", total)} ₽",
                    style = TextStyle(
                        color = Color(0xFF6BCB6B),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // График
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color(0xFF0A1A2F), RoundedCornerShape(6.dp))
                    .padding(4.dp)
            ) {
                if (chartData.size <= 1) {
                    // Если нет данных, показываем заглушку
                    drawContext.canvas.nativeCanvas.drawText(
                        "Начните копить",
                        size.width / 2 - 50,
                        size.height / 2,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 30f
                            alpha = 150
                        }
                    )
                    return@Canvas
                }

                val width = size.width
                val height = size.height
                val padding = 20f

                val startX = padding
                val endX = width - padding
                val bottomY = height - padding
                val topY = padding

                // Максимальное значение (цель или текущая сумма)
                val maxValue = maxOf(
                    (goal.amount ?: 0.0),
                    chartData.maxOfOrNull { it.second } ?: 0.0
                )
                val scaleY = (bottomY - topY) / maxValue.toFloat()

                // Создаем точки для графика
                val points = mutableListOf<Offset>()
                val totalPoints = chartData.size

                chartData.forEachIndexed { index, (_, value) ->
                    val x = startX + (index.toFloat() / (totalPoints - 1)) * (endX - startX)
                    val y = bottomY - (value.toFloat() * scaleY)
                    points.add(Offset(x, y))
                }

                // Рисуем линию графика
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = Color(0xFF6BCB6B),
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 2.5f
                    )
                }

                // Рисуем точки
                points.forEachIndexed { index, point ->
                    val isLastPoint = index == points.size - 1
                    val pointColor = if (isLastPoint) Color(0xFFD4AF37) else Color(0xFF6BCB6B)

                    drawCircle(
                        color = pointColor,
                        radius = if (isLastPoint) 5f else 3f,
                        center = point
                    )
                }

                // Подписи дат
                val dateFormat = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yy")

                // Дата создания
                drawContext.canvas.nativeCanvas.drawText(
                    goal.createdDate.format(dateFormat),
                    startX - 15,
                    bottomY + 15,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 20f
                        alpha = 180
                    }
                )

                // Текущая дата
                drawContext.canvas.nativeCanvas.drawText(
                    LocalDateTime.now().format(dateFormat),
                    endX - 25,
                    bottomY + 15,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 20f
                        alpha = 180
                    }
                )
            }
        }
    }
}


@Composable
fun PredictionCard(prediction: goalProgressPredictor.PredictionResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(140.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2F4A)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Круговая диаграмма
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.Transparent)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val sweepAngle = (prediction.probability / 100f) * 360f
                    drawArc(
                        color = prediction.color,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                    drawArc(
                        color = Color.White.copy(alpha = 0.1f),
                        startAngle = sweepAngle - 90f,
                        sweepAngle = 360f - sweepAngle,
                        useCenter = true
                    )
                }


                Column(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${prediction.probability}%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = prediction.color
                    )
                    Text(
                        text = "шанс успеха",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }


            Spacer(modifier = Modifier.width(16.dp))


            Column {
                Text(
                    text = prediction.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = prediction.color
                )
                Text(
                    text = prediction.advice,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}




