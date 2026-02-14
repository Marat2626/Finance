package com.example.finances


import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder


class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isBalanceSet = prefs.getBoolean("start", false)
        setContent {
            val startDestination = if (isBalanceSet) "loadingCash" else "settingBalance"
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
}
