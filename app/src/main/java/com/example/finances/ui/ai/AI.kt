package com.example.finances

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AI(private val context: Context) {

    companion object {
        private const val TAG = "DeepSeekAI"
        private const val SERVER_URL = "http://172.20.10.9:5001"
    }

    /**
     * Отправка сообщения в чат с контекстом
     */
    suspend fun sendMessage(
        message: String,
        history: List<ChatMessage> = emptyList(),
        context: FinancialContext? = null
    ): AIResponse {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("$SERVER_URL/api/chat")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                // Формируем JSON запрос
                val requestBody = JSONObject().apply {
                    put("message", message)

                    // История сообщений
                    val historyArray = JSONArray()
                    history.forEach { msg ->
                        historyArray.put(JSONObject().apply {
                            put("role", msg.role)
                            put("content", msg.content)
                        })
                    }
                    put("history", historyArray)

                    // Финансовый контекст (если есть)
                    if (context != null) {
                        put("context", JSONObject().apply {
                            put("totalBalance", context.totalBalance)
                            put("monthlyIncome", context.monthlyIncome)
                            put("monthlyExpenses", context.monthlyExpenses)

                            // Цели
                            val goalsArray = JSONArray()
                            context.goals.forEach { goal ->
                                goalsArray.put(JSONObject().apply {
                                    put("name", goal.name)
                                    put("targetAmount", goal.targetAmount)
                                    put("currentAmount", goal.currentAmount)
                                    put("deadline", goal.deadline ?: "")
                                })
                            }
                            put("goals", goalsArray)

                            // Последние транзакции
                            val transactionsArray = JSONArray()
                            context.recentTransactions.forEach { tx ->
                                transactionsArray.put(JSONObject().apply {
                                    put("date", tx.date)
                                    put("category", tx.category)
                                    put("amount", tx.amount)
                                    put("description", tx.description)
                                })
                            }
                            put("recentTransactions", transactionsArray)

                            // Категории расходов
                            put("categories", JSONObject(context.categories))
                        })
                    }
                }

                // Отправляем запрос
                connection.outputStream.use { os ->
                    os.write(requestBody.toString().toByteArray(Charsets.UTF_8))
                }

                // Получаем ответ
                val responseCode = connection.responseCode
                val responseText = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                Log.d(TAG, "Response code: $responseCode")
                Log.d(TAG, "Response: $responseText")

                val jsonResponse = JSONObject(responseText)

                if (jsonResponse.getBoolean("success")) {
                    AIResponse.Success(jsonResponse.getString("response"))
                } else {
                    AIResponse.Error(jsonResponse.optString("error", "Unknown error"))
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                AIResponse.Error(e.message ?: "Network error")
            } finally {
                connection?.disconnect()
            }
        }
    }

    /**
     * Анализ расходов
     */
    suspend fun analyzeExpenses(expenses: List<DateExcel>, categories: Map<String, Int>): AIResponse {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("$SERVER_URL/api/analyze-expenses")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                val expensesData = expenses.take(20).map { transaction ->
                    mapOf(
                        "date" to transaction.date.toString(),
                        "category" to transaction.category,
                        "amount" to transaction.amount,
                        "description" to transaction.description
                    )
                }

                val requestBody = JSONObject().apply {
                    put("expenses", JSONArray().apply {
                        expensesData.forEach { exp ->
                            put(JSONObject(exp))
                        }
                    })
                    put("categories", JSONObject(categories))
                }

                connection.outputStream.use { os ->
                    os.write(requestBody.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                val responseText = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                Log.d(TAG, "Analyze response: $responseText")

                val jsonResponse = JSONObject(responseText)

                if (jsonResponse.getBoolean("success")) {
                    AIResponse.Success(jsonResponse.getString("analysis"))
                } else {
                    AIResponse.Error(jsonResponse.optString("error", "Unknown error"))
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing expenses", e)
                AIResponse.Error(e.message ?: "Network error")
            } finally {
                connection?.disconnect()
            }
        }
    }

    /**
     * Проверка доступности сервера
     */
    suspend fun checkServer(): Boolean {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("$SERVER_URL/api/health")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 3000

                val responseCode = connection.responseCode
                responseCode in 200..299
            } catch (e: Exception) {
                Log.e(TAG, "Server check failed", e)
                false
            } finally {
                connection?.disconnect()
            }
        }
    }
}

// Модели данных
data class ChatMessage(
    val role: String,
    val content: String
)

sealed class AIResponse {
    data class Success(val message: String) : AIResponse()
    data class Error(val error: String) : AIResponse()
}

data class FinancialContext(
    val totalBalance: Double,
    val monthlyIncome: Double,
    val monthlyExpenses: Double,
    val goals: List<Goal>,
    val recentTransactions: List<Transaction>,
    val categories: Map<String, Int>
)

data class Goal(
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val deadline: String? = null
)

data class Transaction(
    val date: String,
    val category: String,
    val amount: Double,
    val description: String
)