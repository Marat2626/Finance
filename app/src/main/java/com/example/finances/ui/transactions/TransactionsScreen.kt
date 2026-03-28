package com.example.finances

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finances.utils.AppColors


import kotlin.Unit

@Composable
fun loadingCash(
    onClipAddGoal: () -> Unit,
    onHomeClick: () -> Unit,
    onClipAddOperation: () -> Unit,
    onSettingScreen: () -> Unit,
    onOpenCamera: () -> Unit,
    onAI: () -> Unit
){
    val context = LocalContext.current
    val app = context.applicationContext as Application

    val showTransactionSheet = remember { mutableStateOf(false) }
    val operationType = remember { mutableStateOf("") }
    val selectedCategory = remember { mutableStateOf("") }
    val amountTransaction = remember { mutableStateOf("") }
    val description = remember { mutableStateOf("") }

    // Состояния для фильтрации
    val selectedFilterType = remember { mutableStateOf("Все") }
    val selectedFilterCategory = remember { mutableStateOf("Все категории") }
    val selectedFilterDate = remember { mutableStateOf("Все время") }
    val minPrice = remember { mutableStateOf("") }
    val maxPrice = remember { mutableStateOf("") }
    val showFilterPanel = remember { mutableStateOf(false) }

    val viewModelE: EISviewModel = viewModel(
        key = "expenses",
        factory = EISViewModelFactory(app, { it.getAllExpenses() }, "Расходы")
    )
    val viewModelI: EISviewModel = viewModel(
        key = "incomes",
        factory = EISViewModelFactory(app, { it.getAllIncome() }, "Зачисления")
    )
    val viewModelS: EISviewModel = viewModel(
        key = "savings",
        factory = EISViewModelFactory(app, { it.getAllSave() }, "Накопление")
    )

    val viewModelG: GoalViewModel = viewModel()

    // Получаем транзакции из ViewModel
    val expenses by viewModelE.transactions.collectAsState()
    val incomes by viewModelI.transactions.collectAsState()
    val savings by viewModelS.transactions.collectAsState()

    // Объединяем все транзакции
    val allTransactions = remember(expenses, incomes, savings) {
        (expenses.map { Triple(it, "Расход", viewModelE) } +
                incomes.map { Triple(it, "Доход", viewModelI) } +
                savings.map { Triple(it, "Накопление", viewModelS) })
            .sortedByDescending { it.first.date }
    }

    // Считаем статистику
    val totalIncome = remember(incomes) { incomes.sumOf { it.amount } }
    val totalExpense = remember(expenses) { expenses.sumOf { it.amount } }
    val totalSavings = remember(savings) { savings.sumOf { it.amount } }
    val balance = totalIncome - totalExpense

    // Фильтруем транзакции
    val filteredTransactions = remember(
        allTransactions,
        selectedFilterType.value,
        selectedFilterCategory.value,
        selectedFilterDate.value,
        minPrice.value,
        maxPrice.value
    ) {
        allTransactions.filter { (transaction, type, _) ->
            // Фильтр по типу
            val typeMatch = when (selectedFilterType.value) {
                "Все" -> true
                else -> type == selectedFilterType.value
            }

            // Фильтр по категории
            val categoryMatch = when (selectedFilterCategory.value) {
                "Все категории" -> true
                else -> transaction.category == selectedFilterCategory.value
            }

            // Фильтр по дате
            val dateMatch = when (selectedFilterDate.value) {
                "Все время" -> true
                "Сегодня" -> {
                    val today = java.time.LocalDate.now()
                    transaction.date.toLocalDate() == today
                }
                "Неделя" -> {
                    val today = java.time.LocalDate.now()
                    val weekAgo = today.minusWeeks(1)
                    transaction.date.toLocalDate() >= weekAgo
                }
                "Месяц" -> {
                    val today = java.time.LocalDate.now()
                    val monthAgo = today.minusMonths(1)
                    transaction.date.toLocalDate() >= monthAgo
                }
                "Год" -> {
                    val today = java.time.LocalDate.now()
                    val yearAgo = today.minusYears(1)
                    transaction.date.toLocalDate() >= yearAgo
                }
                else -> true
            }

            // Фильтр по цене
            val min = minPrice.value.toDoubleOrNull()
            val max = maxPrice.value.toDoubleOrNull()
            val priceMatch = when {
                min != null && max != null -> transaction.amount in min..max
                min != null -> transaction.amount >= min
                max != null -> transaction.amount <= max
                else -> true
            }

            typeMatch && categoryMatch && dateMatch && priceMatch
        }
    }

    val optionCategory = listOf(
        "Авто", "Аптеки", "Детские товары", "Дом и ремонт", "Здоровье",
        "Зоотовары", "Кафе, рестораны, фастфуд", "Кино", "Коммунальные услуги",
        "Красота", "Магазины продуктов", "Одежда и обувь", "Развлечения",
        "Связь и интернет", "Спорт", "Такси", "Транспорт", "Путешествия",
        "Фастфуд", "Цветы", "Электроника", "Другое"
    )

    val options = listOf("Накопление", "Расход", "Доход")
    val filterTypeOptions = listOf("Все", "Расход", "Доход", "Накопление")
    val filterDateOptions = listOf("Все время", "Сегодня", "Неделя", "Месяц", "Год")
    val filterCategoryOptions = listOf("Все категории") + optionCategory



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.screenBlue)
    ) {
        // Верхняя панель с кнопками
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка фильтра
            Button(
                onClick = { showFilterPanel.value = !showFilterPanel.value },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡️",
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Фильтр",
                        style = TextStyle(
                            brush = AppColors.royalGoldGradientV,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            IconButton(
                onClick = { onOpenCamera() },
                modifier = Modifier.size(48.dp)
            ) {
                Text(
                    text = "📷",
                    style = TextStyle(fontSize = 28.sp)
                )
            }


            // Кнопка "+" без фона
            IconButton(
                onClick = { showTransactionSheet.value = true },
                modifier = Modifier.size(48.dp)
            ) {
                Text(
                    text = "➕",
                    style = TextStyle(
                        fontSize = 28.sp
                    )
                )
            }
        }

        // Статистика сверху
        StatisticsCards(
            income = totalIncome,
            expense = totalExpense,
            balance = balance
        )

        // Панель фильтров (появляется при нажатии)
        if (showFilterPanel.value) {
            FilterPanel(
                selectedType = selectedFilterType.value,
                onTypeChange = { selectedFilterType.value = it },
                selectedCategory = selectedFilterCategory.value,
                onCategoryChange = { selectedFilterCategory.value = it },
                selectedDate = selectedFilterDate.value,
                onDateChange = { selectedFilterDate.value = it },
                minPrice = minPrice.value,
                onMinPriceChange = { minPrice.value = it },
                maxPrice = maxPrice.value,
                onMaxPriceChange = { maxPrice.value = it },
                typeOptions = filterTypeOptions,
                categoryOptions = filterCategoryOptions,
                dateOptions = filterDateOptions,
                onClose = { showFilterPanel.value = false },
                onReset = {
                    selectedFilterType.value = "Все"
                    selectedFilterCategory.value = "Все категории"
                    selectedFilterDate.value = "Все время"
                    minPrice.value = ""
                    maxPrice.value = ""
                }
            )
        }

        // Заголовок списка с количеством
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "История операций",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = "${filteredTransactions.size} операций",
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            )
        }

        // Если нет транзакций
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📭",
                        fontSize = 48.sp
                    )
                    Text(
                        text = "Нет операций",
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "Нажмите ➕ чтобы добавить",
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 14.sp
                        )
                    )
                }
            }
        } else {
            // СПИСОК ТРАНЗАКЦИЙ
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTransactions) { (transaction, type, _) ->
                    TransactionCard(
                        transaction = transaction,
                        type = type
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(35.dp))

        // Навигационные кнопки
        bottomNavigationBar(
            { onHomeClick()},
            {  onClipAddOperation() },
            { onClipAddGoal()  },
            { onSettingScreen()},
            { onAI()},
        )
    }

    // BottomSheet для добавления
    TransactionBottomSheet(
        showBottomSheet = showTransactionSheet,
        operationType = operationType,
        selectedCategory = selectedCategory,
        amountTransaction = amountTransaction,
        description = description,
        options = options,
        optionCategory = optionCategory,
        onSaveClick = {
            val amount = amountTransaction.value.toDoubleOrNull() ?: 0.0

            when (operationType.value) {
                "Расход" -> {
                    viewModelE.addManualSave(amount, selectedCategory.value, description.value)
                }
                "Доход" -> {
                    viewModelI.addManualSave(amount, selectedCategory.value, description.value)
                }
                "Накопление" -> {
                    viewModelS.addManualSave(amount, selectedCategory.value, description.value)
                    viewModelG.updateAmount(description.value, amount)
                }
            }

            // Очищаем поля
            operationType.value = ""
            selectedCategory.value = ""
            amountTransaction.value = ""
            description.value = ""
        }
    )
}

@Composable
fun FilterPanel(
    selectedType: String,
    onTypeChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    selectedDate: String,
    onDateChange: (String) -> Unit,
    minPrice: String,
    onMinPriceChange: (String) -> Unit,
    maxPrice: String,
    onMaxPriceChange: (String) -> Unit,
    typeOptions: List<String>,
    categoryOptions: List<String>,
    dateOptions: List<String>,
    onClose: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A3A6A)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Brush.verticalGradient(
            colors = listOf(
                Color(0xFFD4AF37),
                Color(0xFF9A7D0C)
            )
        ))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Заголовок с кнопками
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Фильтры",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Row {
                    // Кнопка сброса
                    TextButton(
                        onClick = onReset,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFD4AF37)
                        )
                    ) {
                        Text("Сброс", fontSize = 14.sp)
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = "✕",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Фильтр по типу
            Text(
                text = "Тип операции",
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(typeOptions) { type ->
                    FilterChip(
                        text = type,
                        isSelected = type == selectedType,
                        onSelected = { onTypeChange(type) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Фильтр по категории
            Text(
                text = "Категория",
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categoryOptions) { category ->
                    FilterChip(
                        text = category,
                        isSelected = category == selectedCategory,
                        onSelected = { onCategoryChange(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Фильтр по дате
            Text(
                text = "Период",
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dateOptions) { date ->
                    FilterChip(
                        text = date,
                        isSelected = date == selectedDate,
                        onSelected = { onDateChange(date) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ценовой диапазон
            Text(
                text = "Ценовой диапазон",
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Поле для минимальной цены
                OutlinedTextField(
                    value = minPrice,
                    onValueChange = { onMinPriceChange(it) },
                    placeholder = {
                        Text(
                            text = "От",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD4AF37),
                        unfocusedBorderColor = Color(0xFF4C67A7),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFD4AF37)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                )

                // Поле для максимальной цены
                OutlinedTextField(
                    value = maxPrice,
                    onValueChange = { onMaxPriceChange(it) },
                    placeholder = {
                        Text(
                            text = "До",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD4AF37),
                        unfocusedBorderColor = Color(0xFF4C67A7),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFD4AF37)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                )
            }
        }
    }
}

@Composable
fun FilterChip(
    text: String,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Button(
        onClick = onSelected,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected)
                Color(0xFFD4AF37)
            else
                Color(0x30FFFFFF)
        ),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
fun StatisticsCards(
    income: Double,
    expense: Double,
    balance: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Баланс
        StatCard(
            title = "Остаток",
            amount = balance,
            color = if (balance >= 0) Color(0xFF6BCB6B) else Color(0xFFFF6B6B),
            icon = "💰",
            modifier = Modifier.weight(1f)
        )

        // Доход
        StatCard(
            title = "Доход",
            amount = income,
            color = Color(0xFF6B9CFF),
            icon = "📈",
            modifier = Modifier.weight(1f)
        )

        // Расход
        StatCard(
            title = "Расход",
            amount = expense,
            color = Color(0xFFFF6B6B),
            icon = "📉",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    amount: Double,
    color: Color,
    icon: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(70.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x703B3B3B)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = icon, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            Text(
                text = "${String.format("%,.0f", amount)} ₽",
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
// Улучшенная карточка транзакции с датой
@Composable
fun TransactionCard(
    transaction: DateExcel,
    type: String
) {
    val typeColor = when (type) {
        "Расход" -> Color(0xFFFF6B6B)
        "Доход" -> Color(0xFF6B9CFF)
        "Накопление" -> Color(0xFF6BCB6B)
        else -> Color.Gray
    }

    val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Детали транзакции */ },
        colors = CardDefaults.cardColors(
            containerColor = Color(0x703B3B3B)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Верхний ряд: категория и сумма
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Категория с иконкой
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кружок с цветом типа
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(typeColor.copy(alpha = 0.2f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (type) {
                                "Расход" -> "💳"
                                "Доход" -> "💰"
                                "Накопление" -> "🏦"
                                else -> "💵"
                            },
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = transaction.category,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Сумма
                Text(
                    text = "${String.format("%,.0f", transaction.amount)} ₽",
                    style = TextStyle(
                        color = typeColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Описание (если есть)
            if (!transaction.description.isNullOrEmpty()) {
                Text(
                    text = transaction.description,
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    ),
                    maxLines = 2,
                    modifier = Modifier.padding(start = 46.dp) // отступ под иконкой
                )

                Spacer(modifier = Modifier.height(4.dp))
            }

            // Дата
            Text(
                text = transaction.date.format(dateFormatter),
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp
                ),
                modifier = Modifier.padding(start = 46.dp) // отступ под иконкой
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionBottomSheet(
    showBottomSheet: MutableState<Boolean>,
    operationType: MutableState<String>,
    selectedCategory: MutableState<String>,
    amountTransaction: MutableState<String>,
    description: MutableState<String>,
    options: List<String>,
    optionCategory: List<String>,
    onSaveClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    if (showBottomSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet.value = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            containerColor = Color(0xFF203565),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.Gray, RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Заголовок
                Text(
                    text = "Добавление транзакции",
                    style = TextStyle(
                        brush = AppColors.royalGoldGradientV,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Spacer(modifier = Modifier.height(45.dp))

                // Тип операции
                DropdownInput(
                    value = operationType.value,
                    onValueChange = { operationType.value = it },
                    label = "Тип операции",
                    options = options
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Категория
                DropdownInput(
                    value = selectedCategory.value,
                    onValueChange = { selectedCategory.value = it },
                    label = "Выбрать категорию",
                    options = optionCategory
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Сумма
                input(
                    value = amountTransaction.value,
                    onValueChange = { amountTransaction.value = it },
                    placeholder = "Сумма транзакции"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Описание
                input(
                    value = description.value,
                    onValueChange = { description.value = it },
                    placeholder = "Описание"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Кнопка сохранения
                Button(
                    onClick = {
                        onSaveClick()
                        showBottomSheet.value = false
                    },
                    border = BorderStroke(3.dp, brush = AppColors.royalGoldGradientL),
                    colors = ButtonDefaults.buttonColors(Color.Transparent),
                    modifier = Modifier
                        .height(55.dp)
                        .width(200.dp)
                        .background(
                            AppColors.radialBlueGradient,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = operationType.value.isNotEmpty() &&
                            selectedCategory.value.isNotEmpty() &&
                            amountTransaction.value.isNotEmpty() &&
                            description.value.isNotEmpty()
                ) {
                    Text(
                        text = "Сохранить",
                        style = TextStyle(
                            brush = AppColors.royalGoldGradientV,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
@Composable
fun ActionButton(
    options: String,
    amount: Double,
    selectedCategory: String,
    description: String,
    onExpenseClicked: ((Double, String, String) -> Unit)? = null,
    onIncomeClicked: ((Double, String, String) -> Unit)? = null,
    onSaveClicked: ((Double, String, String) -> Unit)? = null,
    viewModelG: GoalViewModel,
    onClip: () -> Unit,
    onValidationError: (String, String) -> Unit

) {

    Button(
        onClick = {


            if (options.isEmpty()) {
                onValidationError("type", "Выберите тип операции")
                return@Button
            }

            // Проверка категории
            if (selectedCategory.isEmpty()) {
                onValidationError("category", "Выберите категорию")
                return@Button
            }

            // Проверка описания
            if (description.isEmpty()) {
                onValidationError("description", "Введите описание")
                return@Button
            }

            // Проверка суммы
            if (amount <= 0) {
                onValidationError("amount", "Введите сумму больше 0")
                return@Button
            }



            when (options) {
                "Расход" -> {
                    if (amount > 0) {
                        onExpenseClicked?.invoke(amount, selectedCategory, description)
                    }
                }
                "Доход" -> {
                    if (amount > 0) {
                        onIncomeClicked?.invoke(amount, selectedCategory, description)
                    }
                }
                "Накопление" -> {
                    if (amount > 0) {
                        onSaveClicked?.invoke(amount, selectedCategory, description)
                        viewModelG.updateAmount(description, amount,)
                    }
                }
                else -> {

                }
            }
            onClip()


        },
        border = BorderStroke(3.dp, brush = AppColors.royalGoldGradientL),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .height(100.dp)
            .width(250.dp)
            .background(
                AppColors.radialBlueGradient,
                shape = RoundedCornerShape(10.dp)
            ),
        shape = RoundedCornerShape(10.dp),
        enabled = amount > 0 && selectedCategory.isNotEmpty() && options.isNotEmpty()
    ) {
        Text(
            style = TextStyle(AppColors.royalGoldGradientV),
            textAlign = TextAlign.Center,
            text = "Добавить" ,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {onValueChange},
            readOnly = true,
            placeholder = {
                Text(
                    text = label,
                    style = TextStyle(AppColors.royalGoldGradientV),
                    fontSize = 18.sp
                )
            },
            textStyle = TextStyle(
                brush = AppColors.royalGoldGradientV,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            ),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4C67A7),
                unfocusedBorderColor = Color(0xFF4C67A7),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF4C67A7)
            ),
            shape = RoundedCornerShape(13.dp),
            modifier = Modifier
                .width(320.dp)
                .height(55.dp)
                .background(Color(0x703B3B3B), shape = RoundedCornerShape(13.dp))
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(13.dp),
            modifier = Modifier.background(AppColors.radialBlueGradient, shape = RoundedCornerShape(13.dp))

        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            style = TextStyle(AppColors.royalGoldGradientV,),
                            fontSize = 18.sp

                        )
                    },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = Color.White
                    ),

                    modifier = Modifier.drawWithContent {
                        drawContent()
                        drawLine(
                            color = Color(0xFF2D447E),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun input(value: String,
          onValueChange: (String) -> Unit,
          placeholder: String){
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = TextStyle(AppColors.royalGoldGradientV),
                fontSize = 19.sp,
            )
        },
        textStyle = TextStyle(
            brush = AppColors.royalGoldGradientV,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF4C67A7),  // цвет рамки при фокусе
            unfocusedBorderColor = Color(0xFF4C67A7, ), // цвет рамки без фокуса
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color(0xFF4C67A7)  // цвет курсора
        ),
        shape = RoundedCornerShape(13.dp),
        modifier = Modifier
            .width(320.dp)
            .height(55.dp)
            .background(Color(0x703B3B3B), shape = RoundedCornerShape(13.dp))

    )
}



@Composable
fun category(text: String,   onCategorySelected: (String) -> Unit ){

    Button(
        onClick = {
            onCategorySelected(text)
        },
        border = BorderStroke(1.dp, brush = AppColors.royalGoldGradientV ),
        colors = ButtonDefaults.buttonColors(Color(0xD7343434)),
        modifier = Modifier
            .height(45.dp)
            .width(87.dp)
            .padding(top = 10.dp, start = 12.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp)

    ) {
        Text(
            text = text,
            style = TextStyle(
                brush = AppColors.royalGoldGradientV,
                fontSize = 12.sp
            ),
            textAlign = TextAlign.Center
        )
    }

}
