package com.example.finances


import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder


class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        testModel()
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isBalanceSet = prefs.getBoolean("balance_set", false)
        setContent {
            val startDestination = if (isBalanceSet) "start" else "settingBalance"
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable("settingBalance") {
                    settingBalance(
                        onClik = {
                            navController.navigate("start")
                        }
                    )
                }
                composable("start") {

                    StartScreen(
                        onClipMainScreen = {
                            navController.navigate("mainScreen")
                        },
                        onClipMyDate = {
                            navController.navigate("loadingCash")
                        },
                        onClipSber = {
                                navController.navigate("mainScreen")
                        },
                        onClipSettingBalance = {
                            navController.navigate("settingBalance")
                        }
                    )
                }
                composable("mainScreen") {
                    mainScreen(
                        onClipExpenses = {
                            navController.navigate("ExpensesScreen")
                        },
                        onClipIncomes = {
                            navController.navigate("IncomesScreen")
                        },
                        onClipSave = {
                            navController.navigate("SaveScreen")
                        },
                        onClipAddGoal = {
                            navController.navigate("addGoals")
                        }
                    )
                }
                composable("loadingCash") {
                    loadingCash(
                        onClip = {
                            navController.navigate("mainScreen")
                        }
                    )
                }
                composable("ExpensesScreen"){
                    expensesScren()
                }
                composable("IncomesScreen") {
                    incomesScren()
                }
                composable("SaveScreen") {
                    saveScren()
                }
                composable("addGoals") {
                    addGoals()
                }

            }
        }
    }
    private fun testModel() {
        try {
            // Создаем модель
            val model = createSimpleInterpreter(this)

            // Тестируем
            Log.d("TEST", "🧪 Тестируем модель:")
            Log.d("TEST", "50000 -> ${testIncome(model, 50000.0)}")
            Log.d("TEST", "100000 -> ${testIncome(model, 110000.0)}")
            Log.d("TEST", "150000 -> ${testIncome(model, 150000.0)}")

        } catch (e: Exception) {
            Log.e("TEST", "❌ Ошибка: ${e.message}")
        }
    }

    private fun createSimpleInterpreter(context: Context): Interpreter {
        // 1. Читаем файл модели ПРОСТО
        val inputStream = context.assets.open("python_logic.tflite")
        val bytes = inputStream.readBytes()
        inputStream.close()

        // 2. Создаем ByteBuffer ПРОСТО
        val buffer = ByteBuffer.allocateDirect(bytes.size)
        buffer.order(ByteOrder.nativeOrder())
        buffer.put(bytes)
        buffer.rewind()

        // 3. Возвращаем интерпретатор
        return Interpreter(buffer)
    }
    private fun testIncome(interpreter: Interpreter, income: Double) {
        // ПРОСТОЙ вызов модели
        val inputArray = Array(1) { FloatArray(1) }
        val outputArray = Array(1) { FloatArray(1) }

        inputArray[0][0] = income.toFloat()
        interpreter.run(inputArray, outputArray)

        val score = outputArray[0][0]
        val result = if (score >= 0.5) "ХОРОШО" else "ПЛОХО"

        Log.d("TEST", "Доход $income → $result ($score)")
    }
}
