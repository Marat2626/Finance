package com.example.finances


import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finances.utils.AppColors
import kotlinx.coroutines.launch
import java.time.LocalDate


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    onBack: () -> Unit,
    onClipAddGoal: () -> Unit,
    onClipAddOperation: () -> Unit,
    onHomeClick: () -> Unit,
    onSettingScreen: () -> Unit,
    onAI: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val ai = remember { AI(context) }
    val aiViewModel: AIViewModel = viewModel()  // ← Добавляем ViewModel

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var serverAvailable by remember { mutableStateOf<Boolean?>(null) }

    // Данные из ViewModel
    val totalBalance by aiViewModel.totalBalance.collectAsState()
    val monthlyIncome by aiViewModel.monthlyIncome.collectAsState()
    val monthlyExpenses by aiViewModel.monthlyExpenses.collectAsState()
    val categories by aiViewModel.categories.collectAsState()
    val allTransactions by aiViewModel.expenses.collectAsState()

    val expensesViewModel: EISviewModel = viewModel(
        key = "ai_expenses",
        factory = EISViewModelFactory(
            context.applicationContext as Application,
            { it.getAllExpenses() },
            "Расходы",
        )
    )


    val blackHoles by expensesViewModel.blackHoles.collectAsState()

    // Формируем финансовый контекст
    val financialContext = remember(totalBalance, monthlyIncome, monthlyExpenses, categories, allTransactions) {
        FinancialContext(
            totalBalance = totalBalance,
            monthlyIncome = monthlyIncome,
            monthlyExpenses = monthlyExpenses,
            allTransactions = allTransactions.map { tx ->
                Transaction(
                    date = tx.date.toString(),
                    typeOfOperation = tx.typeOfOperation,
                    category = tx.category,
                    amount = tx.amount,
                    description = tx.description
                )
            },
            categories = categories.mapValues { it.value.toDouble() },
            recentTransactions = allTransactions.takeLast(25).map { tx ->
                Transaction(
                    date = tx.date.toString(),
                    typeOfOperation = tx.typeOfOperation,
                    category = tx.category,
                    amount = tx.amount,
                    description = tx.description
                )
            }
        )
    }

    // Функция для формирования текста со всеми данными
    fun buildShareMessage(): String {
        val sb = StringBuilder()
        sb.appendLine("📊 **Мои финансовые данные:**")
        sb.appendLine()
        sb.appendLine("💰 **Общий баланс:** ${"%.0f".format(totalBalance)} ₽")
        sb.appendLine("📈 **Доходы за месяц:** ${"%.0f".format(monthlyIncome)} ₽")
        sb.appendLine("📉 **Расходы за месяц:** ${"%.0f".format(monthlyExpenses)} ₽")
        sb.appendLine()

        if (categories.isNotEmpty()) {
            sb.appendLine("📂 **Расходы по категориям:**")
            categories.forEach { (cat, amount) ->
                sb.appendLine("   • $cat: ${"%.0f".format(amount.toDouble())} ₽")
            }
            sb.appendLine()
        }

        sb.appendLine("📋 **Всего транзакций:** ${allTransactions.size}")
        sb.appendLine()
        sb.appendLine("Проанализируй мои финансы и дай рекомендации.")

        return sb.toString()
    }

    // Проверка сервера + приветствие
    LaunchedEffect(Unit) {
        serverAvailable = ai.checkServer()

        if (serverAvailable == true && messages.isEmpty()) {
            messages = listOf(
                ChatMessage(
                    role = "assistant",
                    content = "Привет! Я Алиса — твой финансовый помощник 👋\n\n" +
                            "Я вижу все твои доходы, расходы и накопления.\n" +
                            "Спрашивай меня о чём угодно:\n" +
                            "• Сколько я потратил в этом месяце?\n" +
                            "• Какая у меня самая большая трата?\n" +
                            "• Проанализируй мои расходы\n" +
                            "• Как мне лучше экономить?"
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.current.screenBG)
    ) {
        // Верхняя панель
        TopAppBar(
            modifier = Modifier
                .background(AppColors.current.screenBG)
                .fillMaxWidth(),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ai),
                        modifier = Modifier.size(30.dp),
                        contentDescription = "AI",
                        tint = AppColors.current.icones
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    if (serverAvailable == true) {
                        Text(
                            text = " ● Онлайн",
                            style = TextStyle(AppColors.forestMetalGradientL),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    } else if (serverAvailable == false) {
                        Text(
                            text = " ● Офлайн",
                            style = TextStyle(AppColors.alphaRedGradientL),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }



                    Button(
                        onClick = { expensesViewModel.analyzeBlackHoles() },
                        enabled = true,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .padding(start = 15.dp)
                            .height(30.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B1A1A),
                        ),
                    ) {
                        Text(
                            text = "Анализ трат",
                            style = TextStyle(color = Color.White),
                            fontSize = 14.sp
                        )
                    }


                    // Кнопка "Делиться"
                    Button(
                        onClick = {
                            if (!isLoading && serverAvailable == true) {
                                val shareMsg = buildString {
                                    appendLine("**Мои финансовые данные:**")
                                    appendLine()
                                    appendLine("**Общий баланс:** ${"%,.0f".format(totalBalance)} ₽")
                                    appendLine("**Доходы за месяц:** ${"%,.0f".format(monthlyIncome)} ₽")
                                    appendLine("**Расходы за месяц:** ${"%,.0f".format(monthlyExpenses)} ₽")
                                    appendLine()

                                    if (categories.isNotEmpty()) {
                                        appendLine("📂 **Расходы по категориям:**")
                                        categories.forEach { (cat, amount) ->
                                            appendLine("   • $cat: ${"%,.0f".format(amount.toDouble())} ₽")
                                        }
                                        appendLine()
                                    }

                                    appendLine("📋 **Всего транзакций:** ${allTransactions.size}")
                                    appendLine()
                                    appendLine("Проанализируй мои финансы и дай рекомендации по экономии.")
                                }

                                messages = messages + ChatMessage(role = "user", content = "Делюсь финансовыми данными")
                                isLoading = true

                                scope.launch {
                                    val response = ai.sendMessage(
                                        message = shareMsg,
                                        history = messages,
                                        context = financialContext
                                    )

                                    messages = messages + when (response) {
                                        is AIResponse.Success -> ChatMessage("assistant", response.message)
                                        is AIResponse.Error -> ChatMessage("assistant", "${response.error}")
                                    }

                                    isLoading = false
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        },
                        enabled = serverAvailable == true && !isLoading,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .padding(start = 15.dp)
                            .height(30.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFA97C09),
                            disabledContainerColor = Color.Gray
                        ),
                    ) {
                        Text(
                            style = TextStyle(AppColors.current.cardBox),
                            text = "Делиться",
                            fontSize = 14.sp
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )


        // Окно с дырами
        if (blackHoles.isNotEmpty()) {
            AlertDialog(
                containerColor = AppColors.current.screenBG,
                onDismissRequest = { expensesViewModel.clearBlackHoles() },
                title = { Text("Избыточные расходы",fontWeight = FontWeight.Bold, color = AppColors.current.text, fontSize = 20.sp) },
                text = {
                    LazyColumn {
                        items(blackHoles) { hole ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                                colors = CardDefaults.cardColors(containerColor = AppColors.current.cardBox)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("${hole.category} по ${hole.dayOfWeek}", fontWeight = FontWeight.Bold, color = AppColors.current.text)
                                    Text("Средний чек: ${hole.avgAmount.toInt()}₽ | Норма: ${hole.normalMedian.toInt()}₽", color = AppColors.current.textAlfa)
                                    Text("Потери: ${hole.monthlyLoss.toInt()}₽/мес",color = AppColors.casesRed)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { expensesViewModel.clearBlackHoles() }) {
                        Text("Закрыть",  color = AppColors.current.text)
                    }
                }
            )
        }
        // Список сообщений
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            state = listState
        ) {
            items(messages) { message ->
                ChatBubble(message)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFD4AF37))
                    }
                }
            }
        }

        // Поле ввода
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .background(AppColors.current.cardBox, shape = RoundedCornerShape(25.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Спроси Алису...", color = AppColors.current.icones) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = AppColors.current.icones,
                        unfocusedTextColor = AppColors.current.icones,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = AppColors.current.icones,
                        unfocusedIndicatorColor = AppColors.current.icones
                    ),
                    maxLines = 3
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isLoading) {
                            val userMsg = inputText.trim()
                            messages = messages + ChatMessage(role = "user", content = userMsg)
                            inputText = ""
                            isLoading = true

                            scope.launch {
                                val response = ai.sendMessage(
                                    message = userMsg,
                                    history = messages,
                                    context = financialContext
                                )

                                messages = messages + when (response) {
                                    is AIResponse.Success -> ChatMessage("assistant", response.message)
                                    is AIResponse.Error -> ChatMessage("assistant", "❌ ${response.error}")
                                }

                                isLoading = false
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    },
                    enabled = !isLoading && inputText.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Отправить",
                        tint = if (inputText.isNotBlank()) AppColors.current.icones else AppColors.current.iconsWhite
                    )
                }
            }
        }

        // Нижняя навигация
        bottomNavigationBar(
            { onHomeClick() },
            { onClipAddOperation() },
            { onClipAddGoal() },
            { onSettingScreen() },
            { onAI() }
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
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    if (isUser) AppColors.current.icones else Color(0xFF9A8C75),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                ),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Text(
                text = message.content,
                color = Color.White,
                modifier = Modifier.padding(14.dp),
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
    }
}
