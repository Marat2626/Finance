package com.example.finances


import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import java.nio.ByteBuffer
import java.nio.ByteOrder


class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isBalanceSet = prefs.getBoolean("balance_set", false)
        setContent {
            val startDestination = if (isBalanceSet) "mainScreen" else "mainScreen"
            val navController = rememberNavController()
            val showBottomSheet = remember { mutableStateOf(true) }
            NavHost(
                navController = navController,
                startDestination = startDestination,

            ) {


                composable("settingBalance") {
                    settingBalance(
                        onClik = {
                            navController.navigate("mainScreen")
                        }
                    )
                }
                composable("mainScreen") {
                    mainScreen(
                        onClipAddGoal = {
                            navController.navigate("addGoals")
                        },
                        onClipAddOperation = {
                            navController.navigate("loadingCash")
                        },
                        onHomeClick = {
                            navController.navigate("mainScreen")
                        },
                        onSettingScreen = {
                            navController.navigate("categorySettingsScreen")
                        },
                        onAI = {
                            navController.navigate("aiChat")
                        }

                    )
                }

                composable("categorySettingsScreen") {
                    settingsScreen(
                        onSave = {
                            // После сохранения настроек возвращаемся на главный экран
                            navController.popBackStack()
                        },
                        onClipAddGoal = { navController.navigate("addGoals") },
                        onHomeClick = { navController.navigate("mainScreen") },
                        onClipAddOperation = { navController.navigate("loadingCash") },
                        onSettingScreen = {
                            navController.navigate("categorySettingsScreen")
                        },
                        onAI = {
                            navController.navigate("aiChat")
                        }
                    )
                }
                composable("loadingCash") {
                    loadingCash(

                        onClipAddGoal = { navController.navigate("addGoals") },
                        onHomeClick = { navController.navigate("mainScreen") },
                        onClipAddOperation = {navController.navigate("loadingCash")},
                        onSettingScreen = {
                            navController.navigate("categorySettingsScreen")
                        },
                        onOpenCamera = { navController.navigate("cameraScreen") },
                        onAI = {
                            navController.navigate("aiChat")
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
                            navController.navigate("addGoals")
                        },
                        onClipAddOperation = {
                            navController.navigate("loadingCash")
                        },
                        onHomeClick = {
                            navController.navigate("mainScreen")
                                      },
                        onSettingScreen = {
                            navController.navigate("categorySettingsScreen")
                        },
                        onAI = {
                            navController.navigate("aiChat")
                        }
                    )
                }

                composable("addGoals") {
                    addGoals(
                        onClipAddGoal = { navController.navigate("addGoals") },
                        onHomeClick = { navController.navigate("mainScreen") },
                        onClipAddOperation = { navController.navigate("loadingCash") },
//                        myGoalsClip = { navController.navigate("myGoalsScreen") },
                        onGoalClick = { goalId ->
                            Log.d("Navigation", "=== NAVIGATION CALLED ===")
                            Log.d("Navigation", "Navigating to goalDetail/$goalId")
                            navController.navigate("goalDetail/$goalId")
                        },
                        onSettingScreen = {
                            navController.navigate("categorySettingsScreen")
                        },
                        onAI = {
                            navController.navigate("aiChat")
                        }
                    )
                }

                composable("aiChat") {
                    // Получаем данные для анализа
                    val expensesViewModel: EISviewModel = viewModel(
                        factory = EISViewModelFactory(application, { it.getAllExpenses() }, "Расходы")
                    )
                    val expenses by expensesViewModel.transactions.collectAsState()

                    // Получаем настройки категорий
                    val settingsViewModel: CategorySettingsViewModel = viewModel()
                    val savedSettings by settingsViewModel.categories.collectAsState()

                    val categoriesImportance = savedSettings.associate {
                        it.category to (it.importance?.ordinal ?: 0)
                    }

                    val aiViewModel: AIViewModel = viewModel()

                    AIChatScreen(
                        onBack = { navController.popBackStack() },
                        expenses = aiViewModel.expenses.collectAsState().value,
                        categories = aiViewModel.categories.collectAsState().value,
                        goals = aiViewModel.goals.collectAsState().value,
                        totalBalance = aiViewModel.totalBalance.collectAsState().value,
                        monthlyIncome = aiViewModel.monthlyIncome.collectAsState().value,
                        monthlyExpenses = aiViewModel.monthlyExpenses.collectAsState().value,
                        onClipAddGoal = {
                            navController.navigate("addGoals")
                        },
                        onClipAddOperation = {
                            navController.navigate("loadingCash")
                        },
                        onHomeClick = {
                            navController.navigate("mainScreen")
                        },
                        onSettingScreen = {
                            navController.navigate("categorySettingsScreen")
                        },
                        onAI = {
                            navController.navigate("aiChat")
                        }
                    )
                }

            }
        }
    }
}
