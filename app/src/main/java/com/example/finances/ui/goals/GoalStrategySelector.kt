package com.example.finances

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finances.utils.AppColors
import kotlinx.coroutines.flow.first
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalStrategySelector(
    goalAmount: Double,
    category: String,
    onDismiss: () -> Unit,
    onStrategySelected: (StrategyRecommendation) -> Unit,
    onGoToCategories: () -> Unit
) {
    val context = LocalContext.current
    val predictor = remember { MlGoalPredictor(context) }
    var mlPrediction by remember { mutableStateOf<MlGoalPredictor.MlInflationResult?>(null) }
    val calculator = GoalSavingsCalculator(DataTransactions.getDatabase(LocalContext.current))
    var recs by remember { mutableStateOf<List<StrategyRecommendation>>(emptyList()) }

    var showBudgetWarning by remember { mutableStateOf(false) }
    var pendingStrategy by remember { mutableStateOf<StrategyRecommendation?>(null) }

    val db = DataTransactions.getDatabase(LocalContext.current)
    var averageIncome by remember { mutableStateOf(0.0) }
    var totalPlan by remember { mutableStateOf(0.0) }

    LaunchedEffect(Unit) {
        val incomes = db.dataExcelDao().getAllIncome().first()
        val threeMonthsAgo = java.time.LocalDate.now().minusMonths(3)
        val recentIncomes = incomes.filter { it.date.toLocalDate().isAfter(threeMonthsAgo) }
        val monthsCount = recentIncomes.map { it.date.toLocalDate().withDayOfMonth(1) }.distinct().size.coerceAtLeast(1)
        averageIncome = recentIncomes.sumOf { it.amount } / monthsCount

        val planSettings = db.planSettingsDao().getAll().first()
        totalPlan = planSettings.sumOf { it.plannedAmount }
    }

    LaunchedEffect(goalAmount) {
        recs = calculator.calculateStrategies(goalAmount)
    }

    LaunchedEffect(category) {
        mlPrediction = predictor.predictInflation(
            category = category,
            currentAmount = goalAmount,
            months = 12,  // или goalMonths если есть
            onAcceptInflation = { rec -> onStrategySelected(rec) }
        )
    }

    if (showBudgetWarning && pendingStrategy != null) {
        showBudgetWarningDialog(
            averageIncome = averageIncome,
            totalPlan = totalPlan,
            overAmount = totalPlan + pendingStrategy!!.monthlyPayment - averageIncome,
            onIgnore = {
                showBudgetWarning = false
                pendingStrategy?.let { onStrategySelected(it) }
            },
            onGoToCategories = {
                showBudgetWarning = false
                onDismiss()
                onGoToCategories()
            },
            onDismiss = {
                showBudgetWarning = false
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        modifier = Modifier.background(AppColors.current.screenBG).height(800.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    AppColors.current.screenBG,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp)
            ) {
                Text(
                    "Как накопить на цель?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(color = AppColors.current.text, fontWeight = FontWeight.Bold)
                )
                Text(
                    "Сумма: ${String.format("%,.0f", goalAmount)} ₽",
                    style = TextStyle(color = AppColors.current.text)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (category.isNotEmpty() && mlPrediction != null) {
                    InflationPredictionCard(mlPrediction!!)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))

                recs.forEach { rec ->
                    StrategyCard(rec, onClick = {
                        if (totalPlan + rec.monthlyPayment > averageIncome && averageIncome > 0) {
                            pendingStrategy = rec
                            showBudgetWarning = true
                        } else {
                            onStrategySelected(rec)
                        }
                    })
                    Spacer(modifier = Modifier.height(12.dp))
                }

                var customAmount by remember { mutableStateOf("") }
                var isExpanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.current.cardBox, shape = RoundedCornerShape(20.dp))
                        .clickable { isExpanded = !isExpanded },
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                ) {
                    Column(modifier = Modifier) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(
                                modifier = Modifier.padding(start = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ves4),
                                    modifier = Modifier.size(50.dp),
                                    contentDescription = "Цели",
                                    tint = AppColors.current.icones
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Свой взнос", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(if (isExpanded) "▲" else "▼")
                            }
                        }

                        if (isExpanded) {
                            OutlinedTextField(
                                value = customAmount,
                                onValueChange = { customAmount = it },
                                placeholder = {
                                    Text("Сумма в месяц", color = AppColors.current.icones.copy(alpha = 0.5f))
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AppColors.current.iconsWhite,
                                    unfocusedBorderColor = AppColors.current.icones.copy(alpha = 0.3f),
                                    focusedTextColor = AppColors.current.icones,
                                    unfocusedTextColor = AppColors.current.icones,
                                    cursorColor = AppColors.current.icones
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(5.dp))

                            Button(
                                onClick = {
                                    val amount = customAmount.toDoubleOrNull()
                                    if (amount != null && amount > 0) {
                                        val months = ceil(goalAmount / amount).toInt()
                                        val strategy = StrategyRecommendation(
                                            id = R.drawable.ves4,
                                            name = "Свой взнос",
                                            description = "Вы сами выбираете сумму",
                                            months = months,
                                            monthlyPayment = amount
                                        )
                                        if (totalPlan + amount > averageIncome && averageIncome > 0) {
                                            pendingStrategy = strategy
                                            showBudgetWarning = true
                                        } else {
                                            onStrategySelected(strategy)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.current.icones),
                                shape = RoundedCornerShape(14.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 4.dp,
                                    pressedElevation = 8.dp
                                ),
                                enabled = customAmount.toDoubleOrNull() != null && (customAmount.toDoubleOrNull() ?: 0.0) > 0
                            ) {
                                Text("Выбрать", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InflationPredictionCard(
    prediction: MlGoalPredictor.MlInflationResult
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.current.cardBox, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Прогноз инфляции",
                fontSize = 14.sp,
                style = TextStyle(AppColors.current.textAlfa)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Через ${prediction.months} мес. цель будет стоить ~${String.format("%,.0f", prediction.futureAmount)} ₽",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(AppColors.current.text)
            )
            Text(
                "Рост: ${prediction.growthPercent}%",
                fontSize = 14.sp,
                style = TextStyle(AppColors.current.textAlfa)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Рекомендуемый платёж: ${String.format("%,.0f", prediction.recommendedMonthly)} ₽/мес",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                style = TextStyle(AppColors.current.text)
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = prediction.onAccept,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.current.icones),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "Скорректировать сумму с учётом инфляции",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun StrategyCard(
    recommendation: StrategyRecommendation,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.current.cardBox, shape = RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = recommendation.id),
                modifier = Modifier.size(70.dp),
                contentDescription = "Цели",
                tint = AppColors.current.icones
            )

            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(recommendation.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, style = TextStyle(AppColors.current.text))
                Text(recommendation.description, style = TextStyle(AppColors.current.text), fontSize = 14.sp)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("${recommendation.months} мес", fontSize = 18.sp, fontWeight = FontWeight.Bold, style = TextStyle(AppColors.current.text))
                Text("${String.format("%,.0f", recommendation.monthlyPayment)} ₽/мес", fontSize = 15.sp, style = TextStyle(AppColors.current.text))
            }
        }
    }
}