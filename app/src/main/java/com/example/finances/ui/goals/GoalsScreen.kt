package com.example.finances

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.example.finances.utils.AppColors

import java.time.LocalDateTime
import kotlin.Unit



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun addGoals(
    onClipAddGoal: () -> Unit,
    onClipAddOperation: () -> Unit,
    onHomeClick: () -> Unit,
    onGoalClick: (Int) -> Unit,
    onSettingScreen: () -> Unit,
    onAI: () -> Unit
) {
    val viewModelG: GoalViewModel = viewModel()
    val goals by viewModelG.transactions.collectAsState()


    val context = LocalContext.current
    val app = context.applicationContext as Application


    // Получаем накопления (это было главной причиной ошибки)
    val savingsViewModel: EISviewModel = viewModel(
        key = "savings",
        factory = EISViewModelFactory(app, { it.getAllSave() }, "Накопление")
    )
    val categoryList by savingsViewModel.getCategory().collectAsState(initial = emptyList())


    // Состояния для ввода цели
    val showAddGoalSheet = remember { mutableStateOf(false) }
    val goalName = remember { mutableStateOf("") }
    val goalAmount = remember { mutableStateOf("") }
    val goalMonths = remember { mutableStateOf("") }


    // Состояния для стратегии (после нажатия "Далее")
    val showStrategySheet = remember { mutableStateOf(false) }
    val pendingGoalName = remember { mutableStateOf("") }
    val pendingGoalAmount = remember { mutableStateOf<Double?>(null) }
    val pendingGoalMonths = remember { mutableStateOf<Int?>(null) }


    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }


    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.screenBlue),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Мои цели",
                style = TextStyle(
                    brush = AppColors.royalGoldGradientV,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            )


            IconButton(onClick = { showAddGoalSheet.value = true }) {
                Text(
                    text = "+",
                    style = TextStyle(fontSize = 60.sp, brush = AppColors.royalGoldGradientL)
                )
            }
        }


        Spacer(modifier = Modifier.height(8.dp))


        // ==================== СПИСОК ЦЕЛЕЙ ====================
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(goals) { item ->
                val currentSaved = remember(categoryList, item.name, item.createdDate) {
                    categoryList
                        .filter { saving ->
                            val descriptionMatch = saving.description.startsWith("Цель: ${item.name}") ||
                                    saving.description.equals(item.name, ignoreCase = true)


                            val dateMatch = saving.date.isAfter(item.createdDate)


                            descriptionMatch && dateMatch
                        }
                        .sumOf { it.amount }
                }


                GoalCard(
                    goal = item,
                    currentSaved = currentSaved,
                    onDelete = { viewModelG.deleteGoal(item) },
                    onClick = { onGoalClick(item.id) }
                )
            }
        }


        bottomNavigationBar(
            { onHomeClick() },
            { onClipAddOperation() },
            { /* onClipAddGoal */ },
            { onSettingScreen() },
            {onAI()}
        )
    }


    // ==================== 1. BottomSheet ввода цели ====================
    if (showAddGoalSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showAddGoalSheet.value = false },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = Color(0xFF203565),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(5.dp)
                        .background(Color(0xFFD4AF37), RoundedCornerShape(3.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Новая цель",
                    style = TextStyle(brush = AppColors.royalGoldGradientV, fontSize = 32.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 24.dp)
                )


                OutlinedTextField(
                    value = goalName.value,
                    onValueChange = { goalName.value = it },
                    placeholder = { Text("Название цели") },
                    textStyle = TextStyle(brush = AppColors.royalGoldGradientV, fontSize = 20.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD4AF37),
                        focusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(65.dp)
                )


                Spacer(modifier = Modifier.height(16.dp))


                OutlinedTextField(
                    value = goalAmount.value,
                    onValueChange = { goalAmount.value = it },
                    placeholder = { Text("Сумма цели") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(brush = AppColors.royalGoldGradientV, fontSize = 20.sp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD4AF37), focusedTextColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(65.dp)
                )



                Spacer(modifier = Modifier.height(32.dp))


                Button(
                    onClick = {
                        val amount = goalAmount.value.toDoubleOrNull()
                        if (goalName.value.isBlank() || amount == null || amount <= 0) {
                            // snackbar
                        } else {
                            pendingGoalName.value = goalName.value
                            pendingGoalAmount.value = amount
                            showAddGoalSheet.value = false
                            showStrategySheet.value = true
                        }


                    },
                    modifier = Modifier
                        .height(60.dp)
                        .width(220.dp)
                        .background(AppColors.radialBlueGradient, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(Color.Transparent),
                    border = BorderStroke(2.dp, brush = AppColors.royalGoldGradientL)
                ) {
                    Text("Далее →", style = TextStyle(brush = AppColors.royalGoldGradientV, fontSize = 20.sp, fontWeight = FontWeight.Bold))
                }


                Spacer(modifier = Modifier.height(8.dp))
                SnackbarHost(hostState = snackbarHostState)
            }
        }
    }


    // ==================== 2. Выбор стратегии ====================
    if (showStrategySheet.value) {
        GoalStrategySelector(
            goalAmount = pendingGoalAmount.value!!,
            onDismiss = { showStrategySheet.value = false },
            onStrategySelected = { rec ->
                viewModelG.saveGoal(
                    name = pendingGoalName.value,
                    amount = pendingGoalAmount.value,
                    savedAmount = 0.0,
                    monthsCount = rec.months,
                    monthlyPayment = rec.monthlyPayment,
                    createdAt = LocalDateTime.now()
                )
                showStrategySheet.value = false
            }
        )
    }







}


@Composable
fun GoalCard(
    goal: DataGoal,
    currentSaved: Double,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val progress = if ((goal.amount ?: 0.0) > 0) {
        (currentSaved / (goal.amount ?: 1.0)).toFloat().coerceIn(0f, 1f)
    } else 0f

    val remaining = (goal.amount ?: 0.0) - currentSaved
    val percentComplete = (progress * 100).toInt()


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable { onClick() }
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.5f),
                ambientColor = Color.Black.copy(alpha = 0.5f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(2.dp, brush = AppColors.radialBlueGradient),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.radialBlueGradient, shape = RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Верхний ряд с названием и кнопкой удаления
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Название цели
                    Text(
                        text = goal.name.uppercase(),
                        fontSize = 22.sp,
                        style = TextStyle(
                            brush = AppColors.royalGoldGradientV,
                            fontWeight = FontWeight.Bold,

                            ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )


                    // Кнопка удаления
                    Button(
                        onClick = onDelete,
                        border = BorderStroke(1.dp, brush = AppColors.royalGoldGradientL),
                        colors = ButtonDefaults.buttonColors(Color(0xD73B3B3B)),
                        modifier = Modifier
                            .height(36.dp)
                            .width(36.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить цель",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Место для фото (заглушка)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                Color(0x4D4C67A7),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFF4C67A7),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Фото цели",
                            tint = Color(0xFF4C67A7),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Информация о прогрессе
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Прогресс: $percentComplete%",
                            style = TextStyle(
                                brush = AppColors.royalGoldGradientL,
                                fontSize = 20.sp
                            )
                        )

                        Text(
                            text = "${String.format("%,.0f", currentSaved)} ₽ из ${String.format("%,.0f", goal.amount ?: 0.0)} ₽",
                            style = TextStyle(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFD4AF37).copy(alpha = 0.7f),
                                        Color(0xFFE7CF8C).copy(alpha = 0.7f)
                                    )
                                ),
                                fontSize = 17.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))


            }

            // Бейдж "Цель достигнута" если выполнено
            if (remaining <= 0) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color(0x99000000), shape = RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✅ Цель достигнута!",
                        style = TextStyle(
                            color = Color(0xFF6BCB6B),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

