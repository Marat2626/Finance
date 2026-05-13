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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finances.utils.AppColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun selectCategiry(
    onSave: () -> Unit,
    onNavigateToExpenseSettings: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val scope = rememberCoroutineScope()

    val settingsViewModel: CategorySettingsViewModel = viewModel()
    val expensesViewModel: EISviewModel = viewModel(
        key = "expenses",
        factory = EISViewModelFactory(app, { it.getAllExpenses() }, "Расходы")
    )

    val savedSettings by settingsViewModel.categories.collectAsState()
    val transactions by expensesViewModel.transactions.collectAsState()

    val categorySelections = remember { mutableStateMapOf<String, ImportanceType>() }

    LaunchedEffect(Unit) {
        settingsViewModel.loadCategories()
    }

    LaunchedEffect(savedSettings) {
        categorySelections.clear()
        savedSettings.forEach { setting ->
            setting.importance?.let { categorySelections[setting.category] = it }
        }
    }

    val allCategories = remember(transactions) {
        transactions.map { it.category }.distinct().sorted()
    }


    val categoriesWithSelection = remember(allCategories, categorySelections.toMap(), savedSettings) {
        allCategories.map { cat ->
            CategoryWithSelection(
                category = cat,
                importance = categorySelections[cat],
                count = transactions.count { it.category == cat },
            )
        }.sortedByDescending { it.count }
    }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showSkipDialog by remember { mutableStateOf(false) }


    val planViewModel: PlanViewModel = viewModel()
    var showBudgetWarning by remember { mutableStateOf(false) }
    var averageIncome by remember { mutableStateOf(0.0) }
    var currentPlan by remember { mutableStateOf(0.0) }
    var overAmount by remember { mutableStateOf(0.0) }

    var totalAfterSave by remember { mutableStateOf(0.0) }
// Загружаем доход при входе на экран
    LaunchedEffect(Unit) {
        val db = DataTransactions.getDatabase(app)
        val incomes = db.dataExcelDao().getAllIncome().first()
        val threeMonthsAgo = java.time.LocalDate.now().minusMonths(3)
        val recentIncomes = incomes.filter { it.date.toLocalDate().isAfter(threeMonthsAgo) }
        val monthsCount = recentIncomes.map { it.date.toLocalDate().withDayOfMonth(1) }.distinct().size.coerceAtLeast(1)
        averageIncome = recentIncomes.sumOf { it.amount } / monthsCount

        val planSettings = db.planSettingsDao().getAll().first()
        currentPlan = planSettings.sumOf { it.plannedAmount }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.current.screenBG)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.current.cardBox)
                .height(55.dp)
                .padding(start = 10.dp),
            verticalAlignment = Alignment.CenterVertically

        ) {

            Icon(
                painter = painterResource(id = R.drawable.arrow_left),
                modifier = Modifier
                    .size(30.dp)
                    .clickable { onBack() },  // ← кликабельность без отступов
                contentDescription = "Назад",
                tint = AppColors.current.iconsWhite
            )

            Spacer(modifier = Modifier.padding(horizontal = 8.dp))

            Text(
                "Приоритет категорий",
                style = TextStyle(AppColors.current.cardBox),
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
            )
        }


        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp, start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categoriesWithSelection) { item ->
                CategoryCardWithDetail(
                    category = item.category,
                    count = item.count,
                    importance = item.importance,
                    onImportanceChange = { newImp ->
                        if (categorySelections[item.category] == newImp) {
                            categorySelections.remove(item.category)
                        } else {
                            categorySelections[item.category] = newImp
                        }
                    },
                    onDetailClick = { onNavigateToExpenseSettings(item.category) }
                )
            }
        }



        Row {
            // Кнопка сохранения
            Button(
                onClick = {
                    val allSelected = allCategories.all { categorySelections.containsKey(it) }
                    if (allSelected) {
                        showSaveDialog = true
                    } else {
                        showSkipDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp, start = 16.dp, bottom = 10.dp)
                    .background(AppColors.current.cardBox, shape = RoundedCornerShape(16.dp))
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            ) {
                Text(
                    "Сохранить настройки категорий",
                    style = TextStyle(AppColors.current.cardBox),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
// Кнопка:
            // Кнопка:
            Button(
                onClick = {
                    scope.launch {
                        val db = DataTransactions.getDatabase(app)
                        val allExpenses = db.dataExcelDao().getAllExpenses().first()

                        // Считаем новый план по выбранным категориям
                        val newPlanAmount = categorySelections.map { (category, _) ->
                            allExpenses.filter { it.category == category }.sumOf { it.amount }
                        }.sum()

                        // Общий план = существующий (включая цели) + новый
                        totalAfterSave = currentPlan + newPlanAmount

                        if (totalAfterSave > averageIncome && averageIncome > 0) {
                            overAmount = totalAfterSave - averageIncome
                            showBudgetWarning = true
                        } else {
                            settingsViewModel.saveSelectedCategories(categorySelections)
                            kotlinx.coroutines.delay(300)
                            planViewModel.createPlanFromDb(categorySelections.toMap())
                            Toast.makeText(context, "План создан!", Toast.LENGTH_SHORT).show()
                            onSave()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.current.cardBox, shape = RoundedCornerShape(16.dp))
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            ) {
                Text(
                    "Создать план",
                    style = TextStyle(AppColors.current.cardBox),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
    }

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
                    Toast.makeText(context, "План создан!", Toast.LENGTH_SHORT).show()
                    onSave()
                }
            },
            onGoToCategories = {
                showBudgetWarning = false
                // Остаёмся на этом же экране, пользователь может изменить выбор
            },
            onDismiss = {
                showBudgetWarning = false
            }
        )
    }


    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Сохранить настройки?", color = Color.White) },
            text = {
                Text(
                    "Настройки будут использоваться для расчёта стратегий накопления",
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            settingsViewModel.saveSelectedCategories(categorySelections)
                            showSaveDialog = false
                            onSave()
                        }
                    }
                ) {
                    Text("Сохранить", color = Color(0xFF6BCB6B))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Отмена", color = Color(0xFFFF6B6B))
                }
            },
            containerColor = Color(0xFF1E2F4A)
        )
    }


    if (showSkipDialog) {
        val unselectedCount = allCategories.count { !categorySelections.containsKey(it) }


        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            title = { Text("Не все категории отмечены", color = Color.White) },
            text = {
                Column {
                    Text(
                        "Вы не выбрали важность для $unselectedCount категорий.",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Сохранить только отмеченные?", color = Color.White.copy(alpha = 0.7f))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            settingsViewModel.saveSelectedCategories(categorySelections)
                            showSkipDialog = false
                            onSave()
                        }
                    }
                ) {
                    Text("Сохранить отмеченные", color = Color(0xFF6BCB6B))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipDialog = false }) {
                    Text("Вернуться", color = Color(0xFFFF6B6B))
                }
            },
            containerColor = Color(0xFF1E2F4A)
        )
    }
}


fun getTransactionKey(description: String): String {
    // Убираем лишние пробелы в начале и конце
    val trimmed = description.trim()

    // Ищем первый пробел или дефис
    val spaceIndex = trimmed.indexOf(' ')
    val dashIndex = trimmed.indexOf('-')

    // Берем минимальный индекс (какой встретился раньше)
    val separatorIndex = when {
        spaceIndex >= 0 && dashIndex >= 0 -> minOf(spaceIndex, dashIndex)
        spaceIndex >= 0 -> spaceIndex
        dashIndex >= 0 -> dashIndex
        else -> trimmed.length
    }

    // Если разделитель найден — берем часть до него, иначе всё описание
    return if (separatorIndex < trimmed.length) {
        trimmed.substring(0, separatorIndex)
    } else {
        trimmed
    }
}


@Composable
fun ImportanceChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color

) {
    Button(
        onClick = onClick,
        modifier = Modifier

            .height(30.dp)
            .padding(horizontal = 5.dp)
            .background(
                if (isSelected) color else AppColors.current.iconsWhite,
                shape = RoundedCornerShape(25.dp)
            )
            .border(
                BorderStroke(
                    width = 1.dp,
                    color  = if (isSelected) AppColors.casesBlue else AppColors.current.textAlfa,
                ),
                shape = RoundedCornerShape(25.dp)
            )

            .clip(RoundedCornerShape(25.dp))
            .wrapContentSize(Alignment.Center),

        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = if (isSelected) Color.Black else AppColors.current.icones
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        shape = RoundedCornerShape(15.dp),


    ) {
        Text(
            text,
            fontSize = 14.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,  // ← Центрируем текст
            style = TextStyle(if (isSelected) AppColors.current.iconsWhite else AppColors.current.text),
        )
    }
}

@Composable
fun CategoryCardWithDetail(
    category: String,
    count: Int,
    importance: ImportanceType?,
    hasDetailedSettings: Boolean = false,
    onImportanceChange: (ImportanceType) -> Unit,
    onDetailClick: () -> Unit
) {

    Spacer(modifier = Modifier.height(16.dp))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.current.cardBox, shape = RoundedCornerShape(12.dp),)
            .height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        onClick =  onDetailClick
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = category,
                    style = TextStyle(AppColors.current.text),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )



                if (hasDetailedSettings) {  // ← просто проверяем поле
                    Text(
                        text = "📋",
                        style = TextStyle(
                            color = AppColors.current.textAlfa,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }


                Text(
                    "$count раз",
                    style = TextStyle(AppColors.current.text),
                    fontSize = 12.sp
                )
            }


            Spacer(modifier = Modifier.height(8.dp))


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ImportanceChip("Обязательные",  importance == ImportanceType.ESSENTIAL,
                     { onImportanceChange(ImportanceType.ESSENTIAL) },
                    AppColors.current.cardBoxRed,)
                ImportanceChip("Нейтральные", importance == ImportanceType.NEUTRAL,
                     { onImportanceChange(ImportanceType.NEUTRAL) },
                    AppColors.casesYellow,)
                ImportanceChip("Необязательные", importance == ImportanceType.NON_ESSENTIAL,
                     { onImportanceChange(ImportanceType.NON_ESSENTIAL) },
                    AppColors.casesBlue,)
            }


            Spacer(modifier = Modifier.height(8.dp))

            if (importance == null) {
                Text("⚠️ Важность не выбрана", color = Color(0xFFFF6B6B), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
