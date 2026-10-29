package com.example.finances


import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.finances.utils.AppColors
import kotlinx.coroutines.launch

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.format.TextStyle


class MainActivity : ComponentActivity() {
    object PendingRepositoryHolder {
        val repository = PendingRepository()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isBalanceSet = prefs.getBoolean("balance_set", false)
        setContent {
            val startDestination = if (isBalanceSet) "mainScreen" else "mainScreen"
            val navController = rememberNavController()
            val showBottomSheet = remember { mutableStateOf(true) }

            val goalViewModel: GoalViewModel = viewModel()
            val goals by goalViewModel.transactions.collectAsState()

            val lastGoalViewModel: LastGoalViewModel = viewModel()
            val lastGoalId by lastGoalViewModel.lastGoalId.collectAsState()

            // Загружаем цели при старте
            LaunchedEffect(Unit) {
                goalViewModel.loadGoalsFromDb()
            }

            // Определяем, какую цель показывать
            val targetGoalId = when {
                // Если есть цели
                goals.isNotEmpty() -> {
                    // Если есть сохранённая цель И она существует в списке → показываем её
                    if (lastGoalId != -1 && goals.any { it.id == lastGoalId }) {
                        lastGoalId
                    } else {
                        // Иначе показываем первую цель
                        goals.first().id
                    }
                }
                // Если нет целей → создаём новую
                else -> -1
            }

            val pendingRepo = PendingRepositoryHolder.repository
            val pendingTransactions by pendingRepo.pendingTransactions.collectAsState()

            var showDialog by remember { mutableStateOf(false) }
            var currentPending by remember { mutableStateOf<PendingTransaction?>(null) }

// Если есть неподтверждённые транзакции — показываем первую
            LaunchedEffect(pendingTransactions) {
                if (pendingTransactions.isNotEmpty() && currentPending == null && !showDialog) {
                    currentPending = pendingTransactions.first()
                    showDialog = true
                }
            }

// ============================================================
// 2. ДИАЛОГ ПОДТВЕРЖДЕНИЯ (поверх всего)
// ============================================================
            if (showDialog && currentPending != null) {
                val scope = rememberCoroutineScope()
                TransactionConfirmDialog(
                    transaction = currentPending!!,
                    onConfirm = { type, amount, description ->
                        scope.launch {
                            val db = DataTransactions.getDatabase(application)
                            val newTransaction = DateExcel(
                                id = 0,
                                date = java.time.LocalDateTime.now(),
                                typeOfOperation = type,
                                category = "Без категории",
                                amount = amount,
                                currency = "RUB",
                                description = description,
                                status = "Подтверждено",
                                debitCardNumber = ""
                            )
                            db.dataExcelDao().insert(newTransaction)
                            Log.d("MainActivity", "✅ Транзакция сохранена: $amount₽, $type, $description")
                        }
                        pendingRepo.remove(currentPending!!)
                        showDialog = false
                        currentPending = null
                    },
                    onDismiss = {
                        pendingRepo.remove(currentPending!!)
                        showDialog = false
                        currentPending = null
                    }
                )
            }
            NavHost(
                navController = navController,
                startDestination = startDestination,

            ) {



                composable("mainScreen") {


                    mainScreen(
                        onClipAddGoal = {
                            navController.navigate("goalDetail/$targetGoalId")
                        },
                        onClipAddOperation = {
                            navController.navigate("loadingCash")
                        },
                        onHomeClick = {
                            navController.navigate("mainScreen")
                        },
                        onSettingScreen = {
                            navController.navigate("settingsScreen")
                        },
                        onAI = {
                            navController.navigate("aiChat")
                        }

                    )
                }
                composable("settingsScreen") {
                    settingsScreen(
                        onHomeClick = { navController.navigate("mainScreen") },
                        onClipAddOperation = { navController.navigate("loadingCash") },
                        onClipAddGoal = {
                            navController.navigate("goalDetail/$targetGoalId")
                        },
                        onAI = { navController.navigate("aiChat") },
                        onSettingScreen = { navController.navigate("settingsScreen") },

                        )
                }


                // Экран детальной настройки трат внутри категории
                composable("expenseSettingsScreen/{category}") { backStackEntry ->
                    val category = backStackEntry.arguments?.getString("category") ?: ""
                    ExpenseSettingsScreen(
                        selectedCategory = category,
                        onBack = { navController.popBackStack() }
                    )
                }




                // Экран детальной настройки трат внутри категории
                composable("expenseSettingsScreen/{category}") { backStackEntry ->
                    val category = backStackEntry.arguments?.getString("category") ?: ""
                    ExpenseSettingsScreen(
                        selectedCategory = category,
                        onBack = { navController.popBackStack() }
                    )
                }


                composable("ExpenseSettingsScreen/{category}") { backStackEntry ->
                    val category = backStackEntry.arguments?.getString("category") ?: ""

                    ExpenseSettingsScreen(
                        selectedCategory = category,  // ← передаём категорию
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable("loadingCash") {

                    loadingCash(
                        onClipAddGoal = {
                            navController.navigate("goalDetail/$targetGoalId")
                        },
                        onHomeClick = { navController.navigate("mainScreen") },
                        onClipAddOperation = { navController.navigate("loadingCash") },
                        onSettingScreen = { navController.navigate("settingsScreen") },
                        onOpenCamera = { navController.navigate("cameraScreen") },
                        onAI = { navController.navigate("aiChat") },
                        onNavigateToExpenseSettings = { category ->
                            navController.navigate("expenseSettingsScreen/$category")
                        }
                    )
                }




                // В блоке NavHost добавьте:
                composable("cameraScreen") {
                    val scope = rememberCoroutineScope()
                    CameraScreen(
                        onBack = { navController.popBackStack() },
                        onTransactionDetected = { amount, category, description ->
                            // Сохраняем транзакцию в базу
                            val transaction = DateExcel(
                                id = 0,
                                date = java.time.LocalDateTime.now(),
                                typeOfOperation = "Расходы",
                                category = category,
                                amount = amount,
                                currency = "Руб",
                                description = description,
                                status = "Выполнено",
                                debitCardNumber = ""
                            )

                            scope.launch {
                                val db = DataTransactions.getDatabase(application)
                                db.dataExcelDao().insert(transaction)
                            }
                            navController.popBackStack()
                        }
                    )
                }

                composable("goalDetail/{goalId}") { backStackEntry ->
                    val goalId = backStackEntry.arguments?.getString("goalId")?.toInt() ?: 0
                    GoalDetailScreen(
                        goalId = goalId,
                        onBack = { navController.popBackStack() },
                        onClipAddGoal = {
                            navController.navigate("goalDetail/$targetGoalId")
                        },
                        onClipAddOperation = {
                            navController.navigate("loadingCash")
                        },
                        onHomeClick = {
                            navController.navigate("mainScreen")
                                      },
                        onSettingScreen = {
                            navController.navigate("settingsScreen")
                        },
                        onAI = {
                            navController.navigate("aiChat")
                        },
                        onGoToCategories = {
                            navController.navigate("loadingCash")
                        }
                    )
                }


                composable("aiChat") {
                    // Получаем данные для анализа
                    val expensesViewModel: EISviewModel = viewModel(
                        factory = EISViewModelFactory(application, { it.getAllExpenses() },
                            "Расходы"),

                    )


                    AIChatScreen(
                        onBack = { navController.popBackStack() },
                        onClipAddGoal = {
                            navController.navigate("goalDetail/$targetGoalId")
                        },
                        onClipAddOperation = {
                            navController.navigate("loadingCash")
                        },
                        onHomeClick = {
                            navController.navigate("mainScreen")
                        },
                        onSettingScreen = {
                            navController.navigate("settingsScreen")
                        },
                        onAI = {
                            navController.navigate("aiChat")
                        }
                    )
                }

            }
        }
    }

    @Composable
    fun TransactionConfirmDialog(
        transaction: PendingTransaction,
        onConfirm: (type: String, amount: Double, description: String) -> Unit,
        onDismiss: () -> Unit
    ) {
        var editableType by remember { mutableStateOf(transaction.type) }
        var editableAmount by remember { mutableStateOf(transaction.amount.toString()) }
        var editableDescription by remember { mutableStateOf(transaction.description) }

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.current.cardBox
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.current.cardBox, shape = RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Заголовок
                    Text(
                        text = "Новая транзакция",
                        color = AppColors.current.text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    // Тип операции
                    OutlinedTextField(
                        value = editableType,
                        onValueChange = { editableType = it },
                        label = {
                            Text(
                                "Тип операции",
                                style = androidx.compose.ui.text.TextStyle(color = AppColors.current.icones)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.current.icones,
                            unfocusedBorderColor = AppColors.current.icones,
                            focusedTextColor = AppColors.current.text,
                            unfocusedTextColor = AppColors.current.text,
                            cursorColor = AppColors.current.icones
                        ),
                        shape = RoundedCornerShape(13.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Сумма
                    OutlinedTextField(
                        value = editableAmount,
                        onValueChange = { editableAmount = it },
                        label = {
                            Text(
                                text = "Сумма",
                                color = AppColors.current.icones
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.current.icones,
                            unfocusedBorderColor = AppColors.current.icones,
                            focusedTextColor = AppColors.current.text,
                            unfocusedTextColor = AppColors.current.text,
                            cursorColor = AppColors.current.icones
                        ),
                        shape = RoundedCornerShape(13.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Описание
                    OutlinedTextField(
                        value = editableDescription,
                        onValueChange = { editableDescription = it },
                        label = {
                            Text(
                                "Описание",
                               color = AppColors.current.icones
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.current.icones,
                            unfocusedBorderColor = AppColors.current.icones,
                            focusedTextColor = AppColors.current.text,
                            unfocusedTextColor = AppColors.current.text,
                            cursorColor = AppColors.current.icones
                        ),
                        shape = RoundedCornerShape(13.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Кнопки
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Кнопка "Добавить"
                        Button(
                            onClick = {
                                val amount = editableAmount.toDoubleOrNull() ?: transaction.amount
                                onConfirm(editableType, amount, editableDescription)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.current.icones
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Добавить",
                                style = androidx.compose.ui.text.TextStyle(
                                    color = AppColors.current.cardBox,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        // Кнопка "Отклонить"
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AppColors.current.icones
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                AppColors.current.icones
                            )
                        ) {
                            Text(
                                text = "Отклонить",
                                color = AppColors.current.icones,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold

                            )
                        }
                    }
                }
            }
        }
    }

}
