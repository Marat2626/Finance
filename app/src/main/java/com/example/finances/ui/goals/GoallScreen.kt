package com.example.finances

import android.R.attr.strokeWidth
import android.app.Application

import android.graphics.Paint
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.finances.ui.goals.addGoal
import com.example.finances.utils.AppColors
import com.example.finances.utils.cardShadow

import android.util.Log

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
    onAI: () -> Unit,
    onGoToCategories: () -> Unit

) {
    val goals by viewModel.transactions.collectAsState()
    val context = LocalContext.current
    val app = context.applicationContext as Application


    val savingsViewModel: EISviewModel = viewModel(
        key = "savings",
        factory = EISViewModelFactory(app, { it.getAllSave() }, "Накопление")
    )
    val savings by savingsViewModel.getCategory().collectAsState(initial = emptyList())
    LaunchedEffect(Unit) {
        viewModel.loadGoalsFromDb()
    }
    val effectiveGoalId = if (goals.isNotEmpty()) {
        if (goalId == -1) {
            goals.firstOrNull()?.id ?: -1
        } else {
            // Проверяем, существует ли цель с таким ID
            if (goals.any { it.id == goalId }) {
                goalId
            } else {
                goals.firstOrNull()?.id ?: -1
            }
        }
    } else {
        -1  // пока данные не загружены
    }


    var selectedGoalId by remember(effectiveGoalId) { mutableStateOf(effectiveGoalId) }

    LaunchedEffect(effectiveGoalId) {
        if (effectiveGoalId != -1) {
            selectedGoalId = effectiveGoalId
        }
    }
    val currentGoal = goals.find { it.id == selectedGoalId }
    val currentSaved = remember(currentGoal, savings) {
        if (currentGoal != null) {
            savings.filter { saving ->
                saving.description.contains(currentGoal.name, ignoreCase = true) &&
                        saving.date.isAfter(currentGoal.createdDate)
            }.sumOf { it.amount }
        } else 0.0
    }
    val db = DataTransactions.getDatabase(context)

    val predictor1 = remember { goalProgressPredictor(db) }  // db из DataTransactions
    val predictor = remember { MlGoalPredictor(context) }

    var prediction1 by remember { mutableStateOf<goalProgressPredictor.PredictionResult?>(null) }

    var prediction by remember { mutableStateOf<MlGoalPredictor.MlPredictionResult?>(null) }

    LaunchedEffect(currentGoal) {
        if (currentGoal != null) {
            prediction = predictor.predict(db, currentGoal)
        } else {
            prediction = null
        }
    }
    val lastGoalViewModel: LastGoalViewModel = viewModel()
    var showGoalFilter by remember { mutableStateOf(false) }
    var showAddGoalSheet by remember { mutableStateOf(false) }


    if (selectedGoalId != -1) {
        lastGoalViewModel.saveLastGoalId(selectedGoalId)
    }


    val pendingGoalName = remember { mutableStateOf("") }
    val pendingGoalAmount = remember { mutableStateOf<Double?>(null) }
    val showStrategySheet = remember { mutableStateOf(false) }
    val viewModelG: GoalViewModel = viewModel()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.current.screenBG)
    ) {

        LaunchedEffect(showAddGoalSheet) {
            Log.d("GOAL_DEBUG", "showAddGoalSheet = $showAddGoalSheet")
        }

        if (currentGoal == null) {
            nullGoal(
                showAddGoalSheet = { showAddGoalSheet = true },
                modifier = Modifier.fillMaxSize()
            )

        } else {

            Column(modifier = Modifier
                .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            )

            {
                topBar(
                    showGoalFilter = showGoalFilter,
                    onToggleFilter = { showGoalFilter = !showGoalFilter },
                    onAddGoal = { showAddGoalSheet = true },
                    goals = goals,
                    selectedGoalId = selectedGoalId,
                    onGoalSelected = { goal ->
                        selectedGoalId = goal.id
                        showGoalFilter = false
                    }
                )

                LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {

                    item {


                        Spacer(modifier = Modifier.height(16.dp))

                        CompactGoalCard(
                            goal = currentGoal,
                            currentSaved = currentSaved,
                            { viewModel.deleteGoal(currentGoal) }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        if (prediction != null) {
                            PredictionCard(
                                prediction = prediction!!,
                                goals = goals,
                            )

                        } else {

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

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MonthlyPaymentCard(
                                goal = currentGoal,
                                modifier = Modifier.cardShadow().weight(1f)
                            )
                            ProgressChartCard(
                                goal = currentGoal,
                                currentSaved = currentSaved,
                                modifier = Modifier.cardShadow().weight(1f)
                            )
                        }


                        Spacer(modifier = Modifier.height(24.dp))


                        GoalGrowthChart(
                            goal = currentGoal,
                            savings = savings,
                            modifier = Modifier.cardShadow(),
                            currentGoal
                        )


                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "История накоплений",
                                style = TextStyle(
                                    color = AppColors.current.text,
                                    fontSize = 18.sp,

                                    )
                            )

                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        val goalSavings =
                            remember(savings, currentGoal.name, currentGoal.createdDate) {
                                savings.filter { saving ->
                                    saving.description.contains(
                                        currentGoal.name,
                                        ignoreCase = true
                                    ) &&
                                            saving.date.isAfter(currentGoal.createdDate)
                                }.sortedByDescending { it.date }
                            }

                        if (goalSavings.isEmpty()) {
                            EmptySavingsPlaceholder()
                        } else {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(goalSavings.take(5)) { saving ->
                                    EnhancedSavingCard(saving = saving)
                                }
                            }
                        }


                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }


            }





        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)  // вот это прижимает к низу
        ) {
            bottomNavigationBar(
                { onHomeClick() },
                { onClipAddOperation() },
                { onClipAddGoal() },
                { onSettingScreen() },
                { onAI() }
            )
        }


        val pendingGoalName = remember { mutableStateOf("") }
        val pendingGoalAmount = remember { mutableStateOf<Double?>(null) }
        val pendingCategory = remember { mutableStateOf("") }  // ← новое


        if (showAddGoalSheet) {
            Log.d("GOAL_DEBUG", "Показываем addGoal, showAddGoalSheet = $showAddGoalSheet")
            addGoal(
                showAddGoalSheet = { newState -> showAddGoalSheet = newState },
                onGoalCreated = { name, amount, category ->  // ← добавил category
                    pendingGoalName.value = name
                    pendingGoalAmount.value = amount
                    pendingCategory.value = category  // ← сохраняем категорию
                    showStrategySheet.value = true
                }
            )
        }
        if (showStrategySheet.value) {
            GoalStrategySelector(
                goalAmount = pendingGoalAmount.value!!,
                category = pendingCategory.value,
                onDismiss = { showStrategySheet.value = false },
                onStrategySelected = { rec ->
                    viewModelG.saveGoal(
                        name = pendingGoalName.value,
                        amount = rec.goalAmount,  // ← используем сумму из ML
                        savedAmount = 0.0,
                        monthsCount = rec.months,
                        monthlyPayment = rec.monthlyPayment,
                        createdAt = LocalDateTime.now(),
                        category = pendingCategory.value
                    )
                    showStrategySheet.value = false
                },
                onGoToCategories = {  // ← добавить
                    onGoToCategories()
                }
            )

        }
    }

}
@Composable
fun nullGoal(
    showAddGoalSheet: () -> Unit,  // ← функция, которая открывает bottom sheet
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier  // ← применяем modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.goal),
                modifier = Modifier
                    .size(100.dp),
                contentDescription = "Цели",
                tint = AppColors.current.icones
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Цель не найдена",
                style = TextStyle(
                    color = AppColors.current.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "У вас пока нет активных целей",
                fontSize = 14.sp,
                style = TextStyle(AppColors.current.text)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 🟢 КНОПКА
            Button(
                onClick = { showAddGoalSheet() },  // ← ВЫЗЫВАЕМ ФУНКЦИЮ
                modifier = Modifier
                    .width(200.dp)
                    .height(48.dp)
                    .background(AppColors.current.cardBox, shape = RoundedCornerShape(12.dp))
                    .border(
                        BorderStroke(1.dp, AppColors.current.borderLiner),
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "➕",
                        fontSize = 18.sp,
                        style = TextStyle(color = AppColors.current.text)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Создать цель",
                        style = TextStyle(
                            color = AppColors.current.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Начните копить на мечту уже сегодня!",
                style = TextStyle(AppColors.current.text),
                fontSize = 12.sp
            )
        }


    }
}
@Composable
fun topBar(showGoalFilter: Boolean,
           onToggleFilter: () -> Unit,
           onAddGoal: () -> Unit,
           goals: List<DataGoal>,
           selectedGoalId: Int,
           onGoalSelected: (DataGoal) -> Unit
){

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()

                .padding(start = 15.dp, end = 10.dp),
        ) {
            Text(
                text = "ЦЕЛИ",
                style = TextStyle(
                    color = AppColors.current.text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(start = 5.dp)
            )

            Row {
                Button(
                    onClick = { onToggleFilter() },  // ← просто вызываем функцию
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.filter),
                            modifier = Modifier.size(24.dp),
                            contentDescription = "Выбрать цель",
                            tint = Color(0xDA3D3D3D),
                        )
                        Text(
                            text = "Список целей",
                            style = TextStyle(
                                color =  AppColors.current.text,
                                fontSize = 18.sp
                            ),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                IconButton(onClick = onAddGoal) {  // ← просто передаём функцию
                    Text(
                        text = "+",
                        style = TextStyle(
                            fontSize = 40.sp,
                            color = Color(0xDA3D3D3D),
                        )
                    )
                }
            }
        }

        if (showGoalFilter) {  // ← теперь можно использовать как Boolean
            GoalFilterPanel(
                goals = goals,
                selectedGoalId = selectedGoalId,
                onGoalSelected = onGoalSelected,
                onClose = onToggleFilter
            )
        }
    }
}

@Composable
fun CompactGoalCard(
    goal: DataGoal,
    currentSaved: Double,
    onDelete: () -> Unit,
) {

    var showDeleteDialog by remember { mutableStateOf(false) }


    val targetAmount = goal.amount ?: 0.0
    val progress = if (targetAmount > 0) {
        (currentSaved / targetAmount).toFloat().coerceIn(0f, 1f)  // ← используем currentSaved
    } else 0f
    val percentComplete = (progress * 100).toInt()

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
    val customFont = FontFamily(
        Font(R.font.three, FontWeight.Normal)
    )


    Card(
        modifier = Modifier
            .width(330.dp)
            .height(130.dp)
            .cardShadow()
            .background(AppColors.current.cardBox, shape = RoundedCornerShape(20.dp)),

        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent  // прозрачный, чтобы видеть градиент
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
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
                            AppColors.current.cardBox,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            Color(0xFFECE5D8),
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
                        fontSize = 25.sp
                    )
                }

                Spacer(modifier = Modifier.width(5.dp))
                // Название
                Column(
                    modifier = Modifier.weight(1f)
                ) {


                    Text(
                        text = goal.name.toUpperCase(),
                        style = TextStyle(
                            color =  AppColors.current.text,
                            fontSize = 20.sp,
                            fontFamily = customFont,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${targetAmount.formatWithSpace()} ₽",
                        style = TextStyle(
                            color = AppColors.current.text,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                    IconButton(
                        onClick = { showDeleteDialog = true },  // ← показываем диалог
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.editgoals),
                            modifier = Modifier.size(28.dp),
                            contentDescription = "Редактировать цель",
                            tint = Color(0xDA3D3D3D)
                        )
                    }

                    IconButton(
                        onClick = { showDeleteDialog = true },  // ← показываем диалог
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.basket),
                            modifier = Modifier.size(24.dp),
                            contentDescription = "Удалить цель",
                            tint = Color(0xDA3D3D3D)
                        )
                    }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                modifier = Modifier
                    .background(AppColors.current.cardBox, shape = RoundedCornerShape(20.dp)),
                onDismissRequest = { showDeleteDialog = false },
                title = {
                    Text(
                        text = "⚠️ Очистка данных",
                        style = TextStyle(
                            color = AppColors.current.icones,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                text = {
                    Text(
                        text = "Вы уверены, что хотите удалить ВСЕ данные?\n\n" +
                                "Будут удалены:\n" +
                                "• Все транзакции\n" +
                                "• Все цели\n" +
                                "• Настройки категорий\n\n" +
                                "Это действие нельзя отменить!",
                        style = TextStyle(
                            color = AppColors.current.icones,
                            fontSize = 16.sp
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDelete() // ← выполняем очистку
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F)  // красный
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Удалить всё", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false }
                    ) {
                        Text(
                            "Отмена",
                            style = TextStyle(color = AppColors.current.icones)
                        )
                    }
                },
                containerColor = Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp,),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "$percentComplete%",
                style = TextStyle(
                    color = Color(0xFF033B2E),
                    fontSize = 18.sp,
                    fontFamily = customFont,
                    fontWeight = FontWeight.Bold
                ),

            )

            Spacer(modifier = Modifier.padding(horizontal = 3.dp))
            // Прогресс-бар
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF033B2E),
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }

            Spacer(modifier = Modifier.height(5.dp))

            // Информация о времени
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(
                    icon = "📅",
                    text = "Прошло: $timePassed",
                    color = AppColors.current.text,
                )

                InfoChip(
                    icon = "⏳",
                    text = "Осталось: $remainingMonths мес",
                    color = AppColors.current.text,
                )
            }
        }
}

@Composable
fun InfoChip(icon: String, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 5.dp, vertical = 3.dp)



    ) {
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = TextStyle(
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun MonthlyPaymentCard(
    goal: DataGoal,
    modifier: Modifier = Modifier
) {
    val customFont = FontFamily(
        Font(R.font.three, FontWeight.Normal)
    )

    Card(
        modifier = modifier
            .height(80.dp)
            .background(
                color = AppColors.current.cardBox,
                shape = RoundedCornerShape(25.dp)
            )
            ,

        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),



    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ЕЖЕМЕС. ВЗНОС",
                style = TextStyle(
                    AppColors.current.text,
                    fontSize = 12.sp
                )
            )

            Text(
                text = "${goal.monthlyPayment.formatWithSpace()} ₽",
                style = TextStyle(
                    color = AppColors.current.text,
                    fontFamily = customFont ,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = "рекомендовано",
                style = TextStyle(
                    AppColors.current.text,
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
                else -> Pair("Долг ${neededForPastMonths.formatWithSpace()} ₽", Color(0xFFFF6B6B))
            }
        }

        // Есть переплата на следующий месяц
        overpayment > 0 && paidThisMonth >= actualNeededThisMonth ->
            Pair("ПЕРЕПЛАТА",  Color(0xFF033B2E))

        // Проверяем проценты ТЕКУЩЕГО месяца
        monthProgress == 0 -> Pair("ВНЕСИТЕ ПЛАТЕЖ", Color(0xFF510F18))
        monthProgress < 100 -> Pair("ОСТАЛОСЬ ВНЕСТИ",  Color(0xFF033B2E))

        // Месяц оплачен полностью
        paidThisMonth >= actualNeededThisMonth -> Pair("МЕСЯЦ ЗАКРЫТ",  Color(0xFF033B2E))

        else -> Pair("Месяц закрыт",  Color(0xFF033B2E))
    }




    val customFont = FontFamily(
        Font(R.font.three, FontWeight.Normal)
    )

    Card(
        modifier = modifier
            .height(80.dp)
            .background(AppColors.current.cardBox, shape = RoundedCornerShape(25.dp)),

        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, top = 10.dp),
            horizontalAlignment = Alignment.Start  // ← текст по левому краю
        ) {
            Text(
                text = statusText,
                style = TextStyle(
                    color = AppColors.current.text,
                    fontSize = 13.sp,


                )
            )

            Text(
                text =  "${paidThisMonth.formatWithSpace()} ₽",
                style = TextStyle(
                    color =  AppColors.current.text,
                    fontFamily = customFont,
                    fontSize = 24.sp
                )
            )

            LinearProgressIndicator(
                progress = (monthProgress / 100f),
                modifier = Modifier
                    .padding(end = 10.dp)
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = Color(0xFFF5F0E8)
            )

        }
    }
}
@Composable
fun EmptySavingsPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)

            .background(
                AppColors.current.borderLiner,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                brush = AppColors.current.borderLiner,
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.income),
                modifier = Modifier
                    .size(26.dp),
                contentDescription = "Домой",
                tint = AppColors.current.iconsWhite

            )
            Text(
                text = "Пока нет накоплений",
                style = TextStyle(
                    color = AppColors.current.cardBox,
                    fontSize = 14.sp
                )
            )
            Text(
                text = "Начните копить прямо сейчас",
                style = TextStyle(
                    color = AppColors.current.cardBox,
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
            .height(60.dp)
            .cardShadow()
            .background(AppColors.current.cardBox, shape = RoundedCornerShape(20.dp)),

        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),

    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = saving.description.take(10) + if (saving.description.length > 10) "..." else "",
                    style = TextStyle(
                        AppColors.current.text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "+${ saving.amount.formatWithSpace()}₽",
                    style = TextStyle(
                        color = Color(0xFF033B2E),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )


            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = saving.date.format(dateFormatter),
                    style = TextStyle(
                        AppColors.current.textAlfa,
                        fontSize = 11.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = saving.date.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = TextStyle(
                        AppColors.current.textAlfa,
                        fontSize = 10.sp
                    )
                )
            }

//            // Сумма с плюсом
//            Box(
//                modifier = Modifier
//                    .background(
//                        Color(0xFFE7F0EC),
//                        shape = RoundedCornerShape(15.dp)
//                    )
//                    .padding(horizontal = 8.dp, vertical = 4.dp)
//            ) {
//                Text(
//                    text = "+${ saving.amount.formatWithSpace()}",
//                    style = TextStyle(
//                        color = Color(0xFF033B2E),
//                        fontSize = 13.sp,
//                        fontWeight = FontWeight.Bold
//                    )
//                )
//            }
        }
    }
}
@Composable
fun GoalGrowthChart(
    goal: DataGoal,
    savings: List<DateExcel>,
    modifier: Modifier = Modifier,
    currentGoal: DataGoal
) {
    val goalSavings = remember(savings, currentGoal.name, currentGoal.createdDate) {
        savings.filter { saving ->
            saving.description.contains(currentGoal.name, ignoreCase = true) &&
                    saving.date.isAfter(currentGoal.createdDate)
        }.sortedByDescending { it.date }
    }

    // Подготавливаем данные для графика
    val chartData = remember(goal, goalSavings) {
        val result = mutableListOf<Pair<LocalDateTime, Double>>()
        result.add(Pair(goal.createdDate, 0.0))

        val sortedSavings = goalSavings
            .filter { it.description.contains(goal.name, ignoreCase = true) }
            .sortedBy { it.date }

        var cumulative = 0.0
        sortedSavings.forEach { transaction ->
            cumulative += transaction.amount
            result.add(Pair(transaction.date, cumulative))
        }
        result
    }

    // Генерируем список месяцев от даты создания до текущей
    val allMonths = remember(goal.createdDate) {
        val months = mutableListOf<LocalDateTime>()
        var current = goal.createdDate.withDayOfMonth(1)
        val now = LocalDateTime.now()
        while (current.isBefore(now) || current.month == now.month && current.year == now.year) {
            months.add(current)
            current = current.plusMonths(1)
        }
        months
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)  // увеличил высоту для подписей
            .background(AppColors.current.cardBox, shape = RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Заголовок
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Рост накоплений",
                    style = TextStyle(
                        color = AppColors.current.text,
                        fontSize = 14.sp,
                    )
                )
                val total = chartData.lastOrNull()?.second ?: 0.0
                Text(
                    text = "${total.formatWithSpace()} ₽",
                    style = TextStyle(
                        color =  AppColors.current.textAlfa,
                        fontSize = 16.sp,
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // График
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(AppColors.current.borderLiner, RoundedCornerShape(6.dp))
                    .padding(4.dp)
            ) {
                if (chartData.size <= 1) {
                    drawContext.canvas.nativeCanvas.drawText(
                        "Начните копить",
                        size.width / 2 - 50,
                        size.height / 2,
                        Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 30f
                            alpha = 150
                        }
                    )
                    return@Canvas
                }

                val width = size.width
                val height = size.height
                val paddingLeft = 40f
                val paddingRight = 20f
                val paddingTop = 20f
                val paddingBottom = 40f  // увеличен для подписей

                val startX = paddingLeft
                val endX = width - paddingRight
                val bottomY = height - paddingBottom
                val topY = paddingTop

                val maxValue = maxOf(
                    (goal.amount ?: 0.0),
                    chartData.maxOfOrNull { it.second } ?: 0.0
                )
                val scaleY = (bottomY - topY) / maxValue.toFloat()

                // Точки графика
                val points = mutableListOf<Offset>()
                val totalPoints = chartData.size

                chartData.forEachIndexed { index, (_, value) ->
                    val x = startX + (index.toFloat() / (totalPoints - 1)) * (endX - startX)
                    val y = bottomY - (value.toFloat() * scaleY)
                    points.add(Offset(x, y))
                }

                // Линия графика
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = Color(0xFF12314D),
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 2.5f
                    )
                }

                // Точки
                points.forEachIndexed { index, point ->
                    val isLastPoint = index == points.size - 1
                    val pointColor = if (isLastPoint) Color(0xFF12314D) else Color(0xFFC0933D)
                    drawCircle(
                        color = pointColor,
                        radius = if (isLastPoint) 5f else 3f,
                        center = point
                    )
                }

                // Подписи месяцев снизу (только первый и последний)
                val paint = Paint().apply {
                    color = android.graphics.Color.parseColor("#723D3D3D")
                    textSize = 18f
                    alpha = 180
                    textAlign = android.graphics.Paint.Align.LEFT
                }

// Первая подпись (дата создания)
                val firstMonthLabel = when (goal.createdDate.monthValue) {
                    1 -> "Янв ${goal.createdDate.year}"
                    2 -> "Фев ${goal.createdDate.year}"
                    3 -> "Мар ${goal.createdDate.year}"
                    4 -> "Апр ${goal.createdDate.year}"
                    5 -> "Май ${goal.createdDate.year}"
                    6 -> "Июн ${goal.createdDate.year}"
                    7 -> "Июл ${goal.createdDate.year}"
                    8 -> "Авг ${goal.createdDate.year}"
                    9 -> "Сен ${goal.createdDate.year}"
                    10 -> "Окт ${goal.createdDate.year}"
                    11 -> "Ноя ${goal.createdDate.year}"
                    12 -> "Дек ${goal.createdDate.year}"
                    else -> ""
                }

                drawContext.canvas.nativeCanvas.drawText(
                    firstMonthLabel,
                    startX,
                    bottomY + 20f,
                    paint
                )

// Последняя подпись (текущий месяц)
                val now = LocalDateTime.now()
                val lastMonthLabel = when (now.monthValue) {
                    1 -> "Янв ${now.year}"
                    2 -> "Фев ${now.year}"
                    3 -> "Мар ${now.year}"
                    4 -> "Апр ${now.year}"
                    5 -> "Май ${now.year}"
                    6 -> "Июн ${now.year}"
                    7 -> "Июл ${now.year}"
                    8 -> "Авг ${now.year}"
                    9 -> "Сен ${now.year}"
                    10 -> "Окт ${now.year}"
                    11 -> "Ноя ${now.year}"
                    12 -> "Дек ${now.year}"
                    else -> ""
                }

                paint.textAlign = android.graphics.Paint.Align.RIGHT
                drawContext.canvas.nativeCanvas.drawText(
                    lastMonthLabel,
                    endX,
                    bottomY + 20f,
                    paint
                )
            }
        }
    }
}


@Composable
fun PredictionCard(
    prediction: MlGoalPredictor.MlPredictionResult,
//    prediction: goalProgressPredictor.PredictionResult,
    goals: List<DataGoal>
) {

    val totalSavedAmount = goals.sumOf { it.savedAmount }
    val totalGoalAmount = goals.sumOf { it.amount ?: 0.0 }

    val progress = if (totalGoalAmount > 0) {
        (totalSavedAmount / totalGoalAmount).toFloat().coerceIn(0f, 1f)
    } else 0f
    val customFont = FontFamily(
        Font(R.font.three, FontWeight.Normal)
    )

    Row( modifier = Modifier
        .height(160.dp)
    ) {

        Card(
            modifier = Modifier
                .weight(1f)
                .cardShadow()

                .background(
                    color = AppColors.current.cardBox,
                    shape = RoundedCornerShape(25.dp)
                )

                .height(160.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),

            ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.current.cardBox, shape = RoundedCornerShape(25.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "Шанс успеха",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(color = AppColors.current.textAlfa)
                )
                Spacer(Modifier.padding(vertical = 3.dp))


                // Круговая диаграмма
                Box(
                    modifier = Modifier
                        .height(120.dp)
                        .width(120.dp),
//                        .padding(start = 15.dp),
                    Alignment.Center

                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val sweepAngle = (prediction.probability / 100f) * 360f
                        val strokeWidth = 15.dp.toPx()  // толщина кольца
                        val radius = (size.minDimension - strokeWidth) / 2
                        val topLeft = Offset(
                            (size.width - radius * 2) / 2,
                            (size.height - radius * 2) / 2
                        )
                        val arcSize = Size(radius * 2, radius * 2)

                        drawArc(
                            color = Color(0xFFF5F0E8),  // светло-бежевый, всегда видно кольцо
                            startAngle = sweepAngle - 90f,
                            sweepAngle = 360f - sweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = prediction.color,
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,  // false = не заполнять центр
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }


                    Text(
                        text = "${prediction.probability}%",
                        fontSize = 30.sp,
                        fontFamily = customFont,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(color =  AppColors.current.text)
                    )

                }

            }
        }

        Spacer(Modifier.padding(horizontal = 3.dp))

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 7.dp)
                .background(
                    color = AppColors.current.cardBox,
                    shape = RoundedCornerShape(25.dp)
                )

                .height(160.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),

            ) {


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.current.cardBox, shape = RoundedCornerShape(25.dp)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Прогресс",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(color = AppColors.current.textAlfa)
                )

                Spacer(Modifier.padding(vertical = 3.dp))

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(color = AppColors.current.cardBox, shape = CircleShape)
                        .border(
                            width = 1.dp,
                            brush = AppColors.current.borderLiner,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val strokeWidth = 15.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val topLeft = Offset(
                            (size.width - radius * 2) / 2,
                            (size.height - radius * 2) / 2
                        )
                        val arcSize = Size(radius * 2, radius * 2)

                        // Фон (светло-бежевое кольцо)
                        drawArc(
                            color = Color(0xFFF5F0E8),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Прогресс (зелёное кольцо)
                        drawArc(
                            color = Color(0xFF12314D),
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Внутренний круг с текстом
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(AppColors.current.cardBox, CircleShape)
                            .border(
                                width = 1.dp,
                                color = Color.LightGray.copy(alpha = 0.5f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${totalSavedAmount.formatWithSpace()}",
                                fontSize = 20.sp,
                                fontFamily = customFont,
                                style = TextStyle(color =  AppColors.current.text),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "из ${totalGoalAmount.formatWithSpace()}",
                                fontSize = 9.sp,
                                style = TextStyle(color =  AppColors.current.textAlfa)
                            )
                        }


                    }
                }
//
            }
        }

    }
}


@Composable
fun GoalFilterPanel(
    goals: List<DataGoal>,
    selectedGoalId: Int,
    onGoalSelected: (DataGoal) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 15.dp, top = 15.dp, end = 15.dp)
            .background(
                AppColors.current.cardBox,
                RoundedCornerShape(16.dp)
            ),

        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
            // Заголовок
            Row(
                modifier = Modifier

                    .fillMaxWidth()
                    .padding(start = 15.dp, end = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Мои цели",
                    style = TextStyle(AppColors.current.text),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Text("✕", style = TextStyle(color =  AppColors.current.text), fontSize = 16.sp)
                }
            }

        LazyRow ( modifier = Modifier.fillMaxWidth(),  ){
            items(goals) {goal ->
                GoalFilterItem(
                    goal = goal,
                    isSelected = goal.id == selectedGoalId,
                    onClick = { onGoalSelected(goal) }
                )
            }
        }
    }
}

@Composable
fun GoalFilterItem(
    goal: DataGoal,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .wrapContentWidth()

            .padding(start = 17.dp, bottom = 5.dp)
            .background( if (isSelected) AppColors.current.icones.copy(alpha = 0.2f) else AppColors.current.icones, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),

        border = if (isSelected) BorderStroke(1.dp, AppColors.current.borderLiner) else null,
    ) {
        Row(
            modifier = Modifier

                .background(AppColors.current.icones)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val progress = if ((goal.amount ?: 0.0) > 0) {
                ((goal.savedAmount / (goal.amount ?: 1.0)) * 100).toInt()
            } else 0

            Text(
                text = "${goal.name} $progress%",
                style =  TextStyle(AppColors.current.cardBox) ,
                fontSize = if (isSelected) 17.sp else 15.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Bold,

            )

        }
    }
}
