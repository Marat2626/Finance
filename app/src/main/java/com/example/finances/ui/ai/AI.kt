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
        private const val TAG = "Alisa"
        private const val SERVER_URL = "http://10.0.2.2:5000"
    }


    /**
     * Отправка сообщения в чат с полным финансовым контекстом
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


                val requestBody = JSONObject().apply {
                    put("message", message)


                    // История сообщений
                    put("history", JSONArray().apply {
                        history.forEach { msg ->
                            put(JSONObject().apply {
                                put("role", msg.role)
                                put("content", msg.content)
                            })
                        }
                    })


                    // Финансовый контекст
                    if (context != null) {
                        put("context", JSONObject().apply {
                            put("totalBalance", context.totalBalance)
                            put("monthlyIncome", context.monthlyIncome)
                            put("monthlyExpenses", context.monthlyExpenses)
                            put("categories", JSONObject(context.categories))


                            // Все транзакции
                            val txArray = JSONArray()
                            context.allTransactions.forEach { tx ->
                                txArray.put(JSONObject().apply {
                                    put("date", tx.date)
                                    put("typeOfOperation", tx.typeOfOperation)
                                    put("category", tx.category)
                                    put("amount", tx.amount)
                                    put("description", tx.description)
                                })
                            }
                            put("allTransactions", txArray)


                            // Последние 15 транзакций
                            val recentArray = JSONArray()
                            context.recentTransactions.take(15).forEach { tx ->
                                recentArray.put(JSONObject().apply {
                                    put("date", tx.date)
                                    put("typeOfOperation", tx.typeOfOperation)
                                    put("category", tx.category)
                                    put("amount", tx.amount)
                                    put("description", tx.description)
                                })
                            }
                            put("recentTransactions", recentArray)
                        })
                    }
                }


                // Отправляем запрос
                connection.outputStream.use { os ->
                    os.write(requestBody.toString().toByteArray(Charsets.UTF_8))
                }


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
                    AIResponse.Error(jsonResponse.optString("error", "Ошибка сервера"))
                }


            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                AIResponse.Error(e.message ?: "Сетевая ошибка")
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


// ====================== МОДЕЛИ ДАННЫХ ======================


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
    val allTransactions: List<Transaction>,
    val categories: Map<String, Double> = emptyMap(),
    val recentTransactions: List<Transaction> = emptyList()
)


data class Transaction(
    val date: String,
    val typeOfOperation: String,   // "Зачисления", "Расходы", "Накопление"
    val category: String,
    val amount: Double,
    val description: String
)
