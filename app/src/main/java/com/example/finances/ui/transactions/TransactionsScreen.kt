package com.example.finances

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finances.utils.AppColors
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter


import kotlin.Unit
// Добавьте этот import в начало файла
import androidx.navigation.NavHostController

@Composable
fun loadingCash(
    onClipAddGoal: () -> Unit,
    onHomeClick: () -> Unit,
    onClipAddOperation: () -> Unit,
    onSettingScreen: () -> Unit,
    onOpenCamera: () -> Unit,
    onAI: () -> Unit,
    onNavigateToExpenseSettings: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application


    // Основной ViewModel для фильтров (общий для всех экранов)
    val filterViewModel: FilterViewModel = viewModel()

    val operationType = remember { mutableStateOf("") }
    val amountTransaction = remember { mutableStateOf("") }
    val description = remember { mutableStateOf("") }

    val selectedCategory = remember { mutableStateOf("") }

    // Состояния из FilterViewModel
    val showFilterPanel by filterViewModel.showFilterPanel.collectAsState()
    val selectedPeriod by filterViewModel.selectedPeriod.collectAsState()
    val selectedType by filterViewModel.selectedType.collectAsState()
    val selectedCategoryF by filterViewModel.selectedCategory.collectAsState()
    val minPrice by filterViewModel.minPrice.collectAsState()
    val maxPrice by filterViewModel.maxPrice.collectAsState()
    val filteredTransactions by filterViewModel.filteredTransactions.collectAsState()
    val showTransactionSheet = remember { mutableStateOf(false) }

    // ViewModel'ы для получения всех транзакций
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


    val goals by viewModelG.transactions.collectAsState()


    val expenses by viewModelE.transactions.collectAsState()
    val incomes by viewModelI.transactions.collectAsState()
    val savings by viewModelS.transactions.collectAsState()


    // Объединяем все aтранзакции и передаём во FilterViewModel
    val allTransactions = remember(expenses, incomes, savings) {
        (expenses + incomes + savings)
            .sortedByDescending { it.date }
    }

    val priorityMode by filterViewModel.priorityMode.collectAsState()

    LaunchedEffect(allTransactions, priorityMode) {
        val data = if (priorityMode) expenses else allTransactions
        filterViewModel.setTransactions(data)
    }

    val options = listOf("Накопление", "Расходы", "Зачисления")

    // Список категорий (можно вынести в отдельный объект)
    val userCategories = remember(allTransactions) {
        allTransactions
            .map { it.category }
            .distinct()
            .sorted()
    }


    val filterTypeOptions = listOf("Все", "Расходы", "Зачисления", "Накопление")
    val filterDateOptions = listOf("Все время", "Сегодня", "Неделя", "Месяц", "6 месяцев", "Год")
    val filterCategoryOptions = listOf("Все категории") + userCategories


    val categoryGroupMode by filterViewModel.categoryGroupMode.collectAsState()

// ←←← ДОБАВЬ ЭТУ СТРОКУ
    val categorySelections = remember { mutableStateMapOf<String, ImportanceType>() }

    val scope = rememberCoroutineScope()

    val settingsViewModel: CategorySettingsViewModel = viewModel()
    val planViewModel: PlanViewModel = viewModel()


    // В начале loadingCash, рядом с другими var:
    var showBudgetWarning by remember { mutableStateOf(false) }
    var averageIncome by remember { mutableStateOf(0.0) }
    var currentPlan by remember { mutableStateOf(0.0) }
    var overAmount by remember { mutableStateOf(0.0) }
    var totalAfterSave by remember { mutableStateOf(0.0) }


    val filterViewModel1: FilterViewModel = viewModel()
    val selectedPeriod1 by filterViewModel1.selectedPeriod.collectAsState()



    LaunchedEffect(priorityMode) {
        if (priorityMode) {
            val db = DataTransactions.getDatabase(app)
            val incomes = db.dataExcelDao().getAllIncome().first()
            val threeMonthsAgo = java.time.LocalDate.now().minusMonths(3)
            val recentIncomes = incomes.filter { it.date.toLocalDate().isAfter(threeMonthsAgo) }
            val monthsCount = recentIncomes.map { it.date.toLocalDate().withDayOfMonth(1) }
                .distinct().size.coerceAtLeast(1)
            averageIncome = recentIncomes.sumOf { it.amount } / monthsCount

            val planSettings = db.planSettingsDao().getAll().first()
            currentPlan = planSettings.sumOf { it.plannedAmount }

            // Загрузка сохранённых настроек приоритетов
            val savedCategories = db.categorySettingsDao().getAllSettings().first()
            val savedExpenses = db.expenseSettingsDao().getAllSettings().first()
            categorySelections.clear()
            savedCategories.forEach { s ->
                s.importance?.let {
                    categorySelections[s.category] = it
                }
            }
            savedExpenses.forEach { s ->
                s.importance?.let {
                    categorySelections["${s.category}|||${s.description}"] = it
                }
            }
        }
    }

// После всех LaunchedEffect добавь:
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    val db = DataTransactions.getDatabase(app)
                    val savedCategories = db.categorySettingsDao().getAllSettings().first()
                    val savedExpenses = db.expenseSettingsDao().getAllSettings().first()
                    categorySelections.clear()
                    savedCategories.forEach { s ->
                        s.importance?.let {
                            categorySelections[s.category] = it
                        }
                    }
                    savedExpenses.forEach { s ->
                        s.importance?.let {
                            categorySelections["${s.category}|||${s.description}"] = it
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.current.screenBG)
    ) {

        topBar(
            showTransactionSheet = showTransactionSheet,
            onOpenCamera = onOpenCamera,
            priorityMode = priorityMode,
            onTogglePriorityMode = {
                filterViewModel.togglePriorityMode()
            },
            onShowSaved = {
                scope.launch {
                    val db = DataTransactions.getDatabase(app)
                    val savedCategories = db.categorySettingsDao().getAllSettings().first()
                    val savedExpenses = db.expenseSettingsDao().getAllSettings().first()

                    categorySelections.clear()

                    savedCategories.forEach { s ->
                        s.importance?.let { categorySelections[s.category] = it }
                    }

                    savedExpenses.forEach { s ->
                        s.importance?.let {
                            categorySelections["${s.category}|||${s.description}"] = it
                        }
                    }

                    Toast.makeText(
                        context,
                        "Показаны сохраненные (${categorySelections.size})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },

            onClearUI = {
                categorySelections.clear()
                Toast.makeText(context, " очищено", Toast.LENGTH_SHORT).show()
            },

            filterViewModel = filterViewModel,
            periodFilter = selectedPeriod,
            onPeriodFilterChange = {
                val newPeriod = when (selectedPeriod) {
                    "Все время" -> "Текущий месяц"
                    "Текущий месяц" -> "Прошлый месяц"
                    "Прошлый месяц" -> "Все время"
                    else -> "Все время"
                }
                filterViewModel.updatePeriod(newPeriod)
            },
            onToggleFilter = {
                filterViewModel.toggleFilterPanel()
            }
        )

        if (showFilterPanel) {
            FilterPanel(
                selectedType = selectedType,
                onTypeChange = { filterViewModel.updateType(it) },
                selectedCategory = selectedCategoryF,
                onCategoryChange = { filterViewModel.updateCategory(it) },
                selectedDate = selectedPeriod,
                onDateChange = { filterViewModel.updatePeriod(it) },
                minPrice = minPrice,
                onMinPriceChange = { filterViewModel.updateMinPrice(it) },
                maxPrice = maxPrice,
                onMaxPriceChange = { filterViewModel.updateMaxPrice(it) },
                typeOptions = filterTypeOptions,
                categoryOptions = filterCategoryOptions,
                dateOptions = filterDateOptions,
                onClose = { filterViewModel.toggleFilterPanel() },
                onReset = { filterViewModel.resetFilters() }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {


            // Группировка по датам
            val groupedTransactions = remember(filteredTransactions) {
                filteredTransactions
                    .groupBy { it.date.toLocalDate() }
                    .toSortedMap(compareByDescending { it })
            }



            if (groupedTransactions.isEmpty()) {

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.addmoney),
                        modifier = Modifier.size(160.dp),
                        contentDescription = "Добавить транзакцию",
                        tint = AppColors.current.icones
                    )
                    Text(
                        text = "Нет операций",
                        style = TextStyle(
                            color = AppColors.current.text,
                            fontSize = 20.sp
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {


                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        if (categoryGroupMode) {
                            // Общая группировка по категориям за весь период (без разделения по дням)
                            val allFiltered =
                                filteredTransactions  // или priorityFilteredTransactions
                            val groupedByCategory = allFiltered.groupBy { it.category }

                            // Одна группа с заголовком


                            item(key = "priority_group") {
                                DateGroupContainer {
                                    groupedByCategory.forEach { (category, cats) ->
                                        val first = cats.first()
                                        TransactionCard(
                                            transaction = first.copy(
                                                amount = cats.sumOf { it.amount },
                                                description = "${cats.size} операций"
                                            ),
                                            type = first.typeOfOperation,
                                            showPriority = priorityMode,
                                            currentImportance = categorySelections[category],
                                            onImportanceChange = { imp ->
                                                if (categorySelections[category] == imp) {
                                                    // Снимаем выделение
                                                    categorySelections.remove(category)
                                                } else {
                                                    // Меняем важность — удаляем все подкатегории этой категории
                                                    val keysToRemove =
                                                        categorySelections.keys.filter {
                                                            it.startsWith(
                                                                "$category|||"
                                                            )
                                                        }
                                                    keysToRemove.forEach {
                                                        categorySelections.remove(
                                                            it
                                                        )
                                                    }
                                                    // Устанавливаем новую важность
                                                    categorySelections[category] = imp
                                                }
                                            },
                                            onDetailClick = {
                                                onNavigateToExpenseSettings(category)
                                            }

                                        )
                                    }
                                }
                                Spacer(Modifier.padding(vertical = 50.dp))
                            }


                        } else {
                            // Обычный режим — ВОТ ЭТО НУЖНО ДОБАВИТЬ
                            groupedTransactions.forEach { (localDate, transactions) ->
                                item(key = "header_$localDate") {
                                    DateHeader(date = localDate)
                                }

                                item(key = "group_$localDate") {
                                    DateGroupContainer {
                                        transactions.forEachIndexed { index, transaction ->
                                            TransactionCard(
                                                transaction = transaction,
                                                type = transaction.typeOfOperation,
                                                showPriority = priorityMode,
                                                currentImportance = categorySelections["${transaction.category}|||${transaction.description}"],
                                                onImportanceChange = { imp ->
                                                    val key =
                                                        "${transaction.category}|||${transaction.description}"
                                                    if (categorySelections[key] == imp) {
                                                        categorySelections.remove(key)
                                                    } else {
                                                        categorySelections[key] = imp
                                                    }
                                                }
                                            )

                                            if (index != transactions.lastIndex) {
                                                Divider(
                                                    modifier = Modifier.padding(horizontal = 12.dp),
                                                    color = Color.White.copy(alpha = 0.07f),
                                                    thickness = 0.8.dp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)  // вот это прижимает к низу
                    ) {

                        // Кнопки приоритетов — появляются только в режиме priorityMode
                        if (priorityMode) {


                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 60.dp)
                                    .background(Color.Transparent)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Кнопка "Сохранить категории"
                                Button(
                                    onClick = {
                                        scope.launch {
                                            settingsViewModel.saveSelectedCategories(
                                                categorySelections.toMap()
                                            )
//                                            val db = DataTransactions.getDatabase(app)
//                                            val savedCategories =
//                                                db.categorySettingsDao().getAllSettings().first()
//                                            val savedExpenses =
//                                                db.expenseSettingsDao().getAllSettings().first()
//                                            categorySelections.clear()
//                                            savedCategories.forEach { s ->
//                                                s.importance?.let {
//                                                    categorySelections[s.category] = it
//                                                }
//                                            }
//                                            savedExpenses.forEach { s ->
//                                                s.importance?.let {
//                                                    categorySelections["${s.category}|||${s.description}"] =
//                                                        it
//                                                }
//                                            }

                                            Toast.makeText(
                                                context,
                                                "Категории сохранены",
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                        }
                                    },

                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .background(
                                            AppColors.current.icones,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Сохранить\nкатегории",
                                        style = TextStyle(AppColors.current.cardBox),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                // Кнопка "Создать план"
                                // Кнопка "Создать план"
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val db = DataTransactions.getDatabase(app)
                                            val allExpenses =
                                                db.dataExcelDao().getAllExpenses().first()

                                            // Считаем сумму из categorySelections (то что нажато в UI)
                                            var newPlanAmount = 0.0
                                            categorySelections.forEach { (key, _) ->
                                                newPlanAmount += if (key.contains("|||")) {
                                                    val parts = key.split("|||")
                                                    allExpenses.filter { it.category == parts[0] && it.description == parts[1] }
                                                        .sumOf { it.amount }
                                                } else {
                                                    allExpenses.filter { it.category == key }
                                                        .sumOf { it.amount }
                                                }
                                            }

                                            if (newPlanAmount > averageIncome && averageIncome > 0) {
                                                overAmount = newPlanAmount - averageIncome
                                                showBudgetWarning = true
                                            } else {
                                                planViewModel.createPlanFromDb(categorySelections.toMap())

                                                // Перезагружаем
                                                val sc =
                                                    db.categorySettingsDao().getAllSettings()
                                                        .first()
                                                val se =
                                                    db.expenseSettingsDao().getAllSettings().first()
                                                categorySelections.clear()
                                                sc.forEach { s ->
                                                    s.importance?.let {
                                                        categorySelections[s.category] = it
                                                    }
                                                }
                                                se.forEach { s ->
                                                    s.importance?.let {
                                                        categorySelections["${s.category}|||${s.description}"] =
                                                            it
                                                    }
                                                }

                                                Toast.makeText(
                                                    context,
                                                    "План создан!",
                                                    Toast.LENGTH_SHORT
                                                )
                                                    .show()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .background(
                                            AppColors.current.icones,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Создать\nплан",
                                        style = TextStyle(AppColors.current.cardBox),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                        }
                        // Диалог предупреждения о бюджете
                        if (showBudgetWarning) {
                            showBudgetWarningDialog(
                                averageIncome = averageIncome,
                                totalPlan = totalAfterSave,
                                overAmount = overAmount,
                                onIgnore = {
                                    showBudgetWarning = false
                                    scope.launch {
                                        settingsViewModel.saveSelectedCategories(categorySelections)
                                        planViewModel.createPlanFromDb(categorySelections.toMap())
                                        Toast.makeText(context, "План создан!", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                },
                                onGoToCategories = {
                                    showBudgetWarning = false
                                    // Можно перейти на экран категорий или просто закрыть
                                },
                                onDismiss = {
                                    showBudgetWarning = false
                                }
                            )
                        }
                    }


                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)  // вот это прижимает к низу
            ) {
                // Нижняя навигация
                bottomNavigationBar(
                    { onHomeClick() },
                    { onClipAddOperation() },
                    { onClipAddGoal() },
                    { onSettingScreen() },
                    { onAI() },
                )
            }

        }

        TransactionBottomSheet(
            showBottomSheet = showTransactionSheet,
            operationType = operationType,
            selectedCategory = selectedCategory,
            amountTransaction = amountTransaction,
            description = description,
            options = options,
            optionCategory = filterCategoryOptions,
            onSaveClick = {
                val amount = amountTransaction.value.toDoubleOrNull() ?: 0.0

                when (operationType.value) {
                    "Расходы" -> {
                        viewModelE.addManualSave(amount, selectedCategory.value, description.value)
                    }

                    "Зачисления" -> {
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
            },
            goals = goals
        )
    }
}
@Composable
fun topBar(

    showTransactionSheet: MutableState<Boolean>,
    onOpenCamera: () -> Unit,
    priorityMode: Boolean,
    onTogglePriorityMode: () -> Unit,

    onShowSaved: () -> Unit,
    onClearUI: () -> Unit,

    filterViewModel: FilterViewModel,  // ← ViewModel
    periodFilter: String,              // ← текущий период
    onPeriodFilterChange: () -> Unit,
    // Добавляем параметр для фильтра
    onToggleFilter: () -> Unit
) {



        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

                    Column ( modifier = Modifier.fillMaxWidth()

                    ) {
                        Row(modifier = Modifier
                            .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {


                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Transparent
                                ),
                                onClick = onToggleFilter,

                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {

                                    Icon(
                                        painter = painterResource(id = R.drawable.filter),
                                        modifier = Modifier.size(30.dp),
                                        contentDescription = "Фильтр",
                                        tint = AppColors.current.icones
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "ФИЛЬТЕР",
                                        fontSize = 20.sp,
                                        color = AppColors.current.icones,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }



                            Row(
                                modifier = Modifier,
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onTogglePriorityMode,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.stark),
                                        modifier = Modifier.size(30.dp),
                                        contentDescription = "Приоритеты",
                                        tint = if (priorityMode) Color(0xFFD4AF37) else AppColors.current.icones
                                    )
                                }

                                IconButton(onClick = onOpenCamera) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.cameras),
                                        modifier = Modifier.size(32.dp),
                                        contentDescription = "Камера",
                                        tint = AppColors.current.icones
                                    )
                                }

                                IconButton(
                                    onClick = { showTransactionSheet.value = true },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.pluss),
                                        modifier = Modifier.size(30.dp),
                                        contentDescription = "Плюс",
                                        tint = AppColors.current.icones
                                    )
                                }
                            }
                        }
                        if (priorityMode) {

                            Row(
                                modifier = Modifier.weight(1f)
                                    .padding(horizontal = 15.dp)
                            ,
                                verticalAlignment = Alignment.CenterVertically,


                                ) {

                                // Кнопка "Сохраненные" - синяя
                                Button(
                                    onClick = onShowSaved,
                                    modifier = Modifier

                                        .padding(bottom = 10.dp)
                                        .background(
                                            AppColors.current.icones,
                                            shape = RoundedCornerShape(15.dp),
                                        )
                                        .height(30.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                    ),
                                    contentPadding = PaddingValues(
                                        horizontal = 12.dp,
                                        vertical = 0.dp
                                    ),

                                    ) {
                                    Text(
                                        text = "Сохраненные",
                                        fontSize = 15.sp,
                                        style = TextStyle(AppColors.current.cardBox),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(Modifier.padding(horizontal = 4.dp))
                                // Кнопка "Очистить UI" - красная
                                Button(
                                    onClick = onPeriodFilterChange,

                                    modifier = Modifier
                                        .padding(bottom = 10.dp)
                                        .background(
                                            AppColors.current.icones,
                                            shape = RoundedCornerShape(15.dp),
                                        )
                                        .height(30.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                    ),
                                    contentPadding = PaddingValues(
                                        horizontal = 12.dp,
                                        vertical = 0.dp
                                    ),
                                ) {
                                    Text(
                                        text = when (periodFilter) {
                                            "Текущий месяц" -> "Месяц"
                                            "Прошлый месяц" -> "Прошлый"
                                            else -> "Всё время"
                                        },
                                        fontSize = 13.sp,
                                        style = TextStyle(AppColors.current.cardBox),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(Modifier.padding(horizontal = 4.dp))

                                Button(
                                    onClick = onClearUI,
                                    modifier = Modifier

                                        .padding(bottom = 10.dp)
                                        .background(
                                            AppColors.current.icones,
                                            shape = RoundedCornerShape(15.dp),
                                        )
                                        .height(30.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                    ),
                                    contentPadding = PaddingValues(
                                        horizontal = 12.dp,
                                        vertical = 0.dp
                                    ),

                                    ) {
                                    Text(
                                        text = "Очистить",
                                        fontSize = 15.sp,
                                        style = TextStyle(AppColors.current.cardBox),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // Растягиваем пространство если нет кнопок важности
                            Spacer(modifier = Modifier.weight(1f))
                        }
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
    onSaveClick: () -> Unit,
    goals: List<DataGoal>, // Список целей

) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    if (showBottomSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet.value = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            containerColor = Color.Transparent,
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
                    .fillMaxSize()
                    .background(AppColors.current.screenBG)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Заголовок
                Text(
                    text = "Добавление транзакции",
                    style = TextStyle(
                        color =  AppColors.current.text,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (goals.isNotEmpty()) {
                    Row(
                        modifier = Modifier

                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        goals.forEach { goal ->
                            Button(
                                onClick = {

                                    operationType.value = "Накопление"
                                    selectedCategory.value = "Автомобиль"
                                    description.value = goal.name
                                },
                                modifier = Modifier.background(AppColors.current.icones,shape = RoundedCornerShape(8.dp)).weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent
                                ),
                            ) {
                                Text(
                                    text = goal.name,
                                    style = TextStyle(color = AppColors.current.cardBox) ,
                                    fontSize = 17.sp
                                )
                            }
                        }
                    }
                } else {
                    // Простая подсказка
                    Text(
                        text = "Нет активных целей",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
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
                    placeholder = "Сумма транзакции",
                    keyboardType = KeyboardType.Number
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Описание
                input(
                    value = description.value,
                    onValueChange = { description.value = it },
                    placeholder = "Описание",
                    keyboardType = KeyboardType.Text
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Кнопка сохранения
                Button(
                    onClick = {
                        onSaveClick()
                        showBottomSheet.value = false
                    },
                    colors = ButtonDefaults.buttonColors(Color.Transparent),
                    modifier = Modifier
                        .height(55.dp)
                        .width(200.dp)
                        .background(
                            AppColors.current.icones,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = operationType.value.isNotEmpty() &&
                           ( selectedCategory.value.isNotEmpty() || description.value.isNotEmpty()) &&
                            amountTransaction.value.isNotEmpty()
                ) {
                    Text(
                        text = "Сохранить",
                        style = TextStyle(
                            color = AppColors.current.cardBox,
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
    Card (
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(AppColors.current.cardBox,shape = RoundedCornerShape(25.dp))
            .border(
                width = 1.dp,
                shape = RoundedCornerShape(25.dp),
                color = AppColors.current.cardBox,
            ),

        colors = CardDefaults.cardColors(containerColor = Color.Transparent),

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
                        color = AppColors.current.text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Row (
                    verticalAlignment = Alignment.CenterVertically
                ){
                    // Кнопка сброса
                    TextButton(
                        onClick = onReset,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFD4AF37)
                        ),
                    ) {
                        Text(
                            text = "Сброс",
                            fontSize = 16.sp,
                            style = TextStyle( color = AppColors.current.text),
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = "✕",
                            style = TextStyle(color =  AppColors.current.text),
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
                    color =  AppColors.current.text,
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
                    color = AppColors.current.text,
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
                    color = AppColors.current.text,
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
                    color = AppColors.current.text,
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
                            style = TextStyle( color = AppColors.current.cardBox,),
                            fontSize = 14.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.current.icones,
                        unfocusedBorderColor = AppColors.current.icones,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = AppColors.current.icones
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
                            style = TextStyle( color = AppColors.current.cardBox,),
                            fontSize = 14.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.current.icones,
                        unfocusedBorderColor = AppColors.current.icones,
                        focusedTextColor = AppColors.current.icones,
                        unfocusedTextColor = AppColors.current.icones,
                        cursorColor = AppColors.current.icones
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

    Box(
        modifier = Modifier
            .background(
                if (isSelected)
                    AppColors.casesYellow
                else
                    AppColors.casesBlue,

                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Button(
            onClick = onSelected,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = text,
                style = TextStyle(
                    color = AppColors.current.iconsWhite
                ),

                fontSize = 14.sp,
                fontWeight = FontWeight.Bold ,
                maxLines = 1
            )
        }
    }
}

// Улучшенная карточка транзакции с датой
@Composable
fun TransactionCard(
    transaction: DateExcel,
    type: String,
    showPriority: Boolean = false,
    currentImportance: ImportanceType? = null,
    onImportanceChange: (ImportanceType) -> Unit = {},
    onDetailClick: (() -> Unit)? = null
) {
    val typeColor = when (type) {
        "Расходы" -> Color(0xFF510F18)
        "Зачисления" -> Color(0xFF007B2D)
        "Накопление" -> Color(0xFFC0933D)
        else -> Color.Unspecified
    }

    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")



    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    )
    {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {


            Icon(
                painter = when (type) {
                    "Расходы" -> painterResource(id = R.drawable.mony)
                    "Зачисления" -> painterResource(id = R.drawable.income)
                    "Накопление" -> painterResource(id = R.drawable.save)
                    else -> painterResource(id = R.drawable.save)
                },
                modifier = Modifier
                    .size(50.dp).padding(end = 10.dp),
                contentDescription = "Домой",
                tint = typeColor

            )



                // Категория с иконкой
            Column(
                modifier = Modifier.weight(1f)
            ) {
                    Text(
                        text = transaction.category,
                        style = TextStyle(
                            color =  AppColors.current.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                if (!transaction.description.isNullOrEmpty()) {
                    Text(
                        text = transaction.description,
                        style = TextStyle(
                            color = AppColors.current.text,
                            fontSize = 14.sp
                        ),
                        maxLines = 2,
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Дата
                Text(
                    text = transaction.date.format(dateFormatter),
                    style = TextStyle(
                        color =  AppColors.current.text,
                        fontSize = 11.sp
                    ),
                )

            }

            Column() {
                Text(
                    text = "${transaction.amount.formatWithSpace()} ₽",
                    style = TextStyle(
                        color = typeColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (onDetailClick != null && showPriority) {
                    IconButton(
                        onClick = onDetailClick,
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.End)


                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.stark),
                            modifier = Modifier.size(20.dp),
                            contentDescription = "Детали категории",
                            tint = AppColors.current.icones
                        )
                    }
                }
            }



        }

        if (showPriority) {
            LazyRow( modifier = Modifier
                .fillMaxWidth()
                .height(35.dp)

            ) {
                item {
                    ImportanceChip(
                        "Обязательные",
                        currentImportance == ImportanceType.ESSENTIAL,
                        { onImportanceChange(ImportanceType.ESSENTIAL) },
                        AppColors.casesRed,
                    )
                    ImportanceChip(
                        "Нейтральные",
                        currentImportance == ImportanceType.NEUTRAL,
                        { onImportanceChange(ImportanceType.NEUTRAL) },
                        AppColors.casesYellow,
                    )
                    ImportanceChip(
                        "Необязательные",
                        currentImportance == ImportanceType.NON_ESSENTIAL,
                        { onImportanceChange(ImportanceType.NON_ESSENTIAL) },
                        AppColors.casesBlue,
                    )
                }
            }
        }
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
                    style = TextStyle(AppColors.current.text),
                    fontSize = 18.sp
                )
            },
            textStyle = TextStyle(
                color = AppColors.current.cardBox,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            ),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.current.icones,
                unfocusedBorderColor =  AppColors.current.icones,
                focusedTextColor = AppColors.current.icones,
                unfocusedTextColor =  AppColors.current.icones,
                cursorColor = AppColors.current.icones
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
            modifier = Modifier.background(AppColors.current.cardBox, shape = RoundedCornerShape(13.dp))

        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            style = TextStyle(AppColors.current.text,),
                            fontSize = 18.sp

                        )
                    },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = AppColors.current.icones
                    ),

                    modifier = Modifier.drawWithContent {
                        drawContent()
                        drawLine(
                            color = Color(0xFFC5CADC),
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
          placeholder: String,
          keyboardType: KeyboardType
){
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = TextStyle(AppColors.current.cardBox),
                fontSize = 19.sp,
            )
        },
        textStyle = TextStyle(
            color = AppColors.current.cardBox,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFC5CAD7),  // цвет рамки при фокусе
            unfocusedBorderColor = Color(0xFFC9CED9), // цвет рамки без фокуса
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color(0xFFDCDFE7)  // цвет курсора
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
@Composable
fun DateHeader(date: java.time.LocalDate) {
    val today = java.time.LocalDate.now()
    val yesterday = today.minusDays(1)

    val monthFormatter = DateTimeFormatter.ofPattern("dd MMMM")
        .withLocale(java.util.Locale("ru"))  // ← русская локаль

    val headerText = when {
        date == today -> "Сегодня"
        date == yesterday -> "Вчера"
        else -> date.format(monthFormatter)
    }

    Text(
        text = headerText,
        style = TextStyle(
            color =  AppColors.current.text,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, bottom = 4.dp)  // ← уменьшил bottom с 6.dp до 4.dp
    )
}

@Composable
fun DateGroupContainer(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.current.cardBox, shape = RoundedCornerShape(20.dp))
            .border(1.dp, AppColors.current.borderLiner, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {  // ← уменьшил с 6.dp до 4.dp
            content()
        }
    }
}