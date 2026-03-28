package com.example.finances

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finances.utils.AppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    onBack: () -> Unit,
    expenses: List<DateExcel> = emptyList(),
    categories: Map<String, Int> = emptyMap(),
    goals: List<Goal> = emptyList(),
    totalBalance: Double = 0.0,
    monthlyIncome: Double = 0.0,
    monthlyExpenses: Double = 0.0,
    onClipAddGoal: () -> Unit,
    onClipAddOperation: () -> Unit,
    onHomeClick: () -> Unit,
    onSettingScreen: () -> Unit,
    onAI: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val deepSeekAI = remember { AI(context) }

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var serverAvailable by remember { mutableStateOf<Boolean?>(null) }

    // Формируем финансовый контекст
    val financialContext = remember(expenses, categories, goals, totalBalance, monthlyIncome, monthlyExpenses) {
        FinancialContext(
            totalBalance = totalBalance,
            monthlyIncome = monthlyIncome,
            monthlyExpenses = monthlyExpenses,
            goals = goals,
            recentTransactions = expenses.take(20).map { tx ->
                Transaction(
                    date = tx.date.toString(),
                    category = tx.category,
                    amount = tx.amount,
                    description = tx.description
                )
            },
            categories = categories
        )
    }

    // Проверка сервера при запуске
    LaunchedEffect(Unit) {
        serverAvailable = deepSeekAI.checkServer()
        if (serverAvailable == true && messages.isEmpty()) {
            messages = listOf(
                ChatMessage(
                    role = "assistant",
                    content = "Привет! Я твой финансовый помощник. " +
                            "Могу помочь проанализировать расходы, дать советы по экономии " +
                            "или просто ответить на вопросы о финансах. Чем могу помочь?"
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.screenBlue)
    ) {
        // Верхняя панель
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("AI Помощник", color = Color.White, fontWeight = FontWeight.Bold)
                    if (serverAvailable == true) {
                        Text(
                            text = " ● Онлайн",
                            color = Color(0xFF4CAF50),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    } else if (serverAvailable == false) {
                        Text(
                            text = " ● Офлайн",
                            color = Color(0xFFF44336),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF2A3A6A)
            )
        )

        // Кнопка анализа расходов (если есть данные)
        if (expenses.isNotEmpty()) {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        val analysis = deepSeekAI.analyzeExpenses(expenses, categories)
                        when (analysis) {
                            is AIResponse.Success -> {
                                messages = messages + ChatMessage(
                                    role = "assistant",
                                    content = "📊 **Анализ расходов:**\n\n${analysis.message}"
                                )
                            }
                            is AIResponse.Error -> {
                                messages = messages + ChatMessage(
                                    role = "assistant",
                                    content = "❌ Ошибка анализа: ${analysis.error}"
                                )
                            }
                        }
                        isLoading = false
                        listState.animateScrollToItem(messages.size)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
            ) {
                Text("📈 Проанализировать мои расходы", color = Color(0xFF1E2F4A))
            }
        }

        // Список сообщений
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            state = listState,
            reverseLayout = false
        ) {
            items(messages) { message ->
                ChatBubble(message)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFFD4AF37)
                    )
                }
            }
        }

        // Поле ввода
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A3A6A)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Спроси AI...", color = Color.White.copy(alpha = 0.5f)) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color(0xFFD4AF37),
                        unfocusedIndicatorColor = Color(0xFF4C67A7)
                    )
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isLoading) {
                            val userMessage = inputText
                            messages = messages + ChatMessage(role = "user", content = userMessage)
                            inputText = ""
                            isLoading = true

                            scope.launch {
                                val response = deepSeekAI.sendMessage(
                                    message = userMessage,
                                    history = messages,
                                    context = financialContext
                                )
                                when (response) {
                                    is AIResponse.Success -> {
                                        messages = messages + ChatMessage(
                                            role = "assistant",
                                            content = response.message
                                        )
                                    }
                                    is AIResponse.Error -> {
                                        messages = messages + ChatMessage(
                                            role = "assistant",
                                            content = "❌ ${response.error}"
                                        )
                                    }
                                }
                                isLoading = false
                                listState.animateScrollToItem(messages.size)
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Отправить",
                        tint = if (inputText.isNotBlank()) Color(0xFFD4AF37) else Color.Gray
                    )
                }
            }
        }

        bottomNavigationBar(
            { onHomeClick() },
            { onClipAddOperation() },
            { onClipAddGoal() },
            { onSettingScreen() },
            { onAI() },
        )
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Color(0xFFD4AF37) else Color(0xFF2A3A6A)
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                color = if (isUser) Color(0xFF1E2F4A) else Color.White,
                modifier = Modifier.padding(12.dp),
                fontSize = 14.sp
            )
        }
    }
}