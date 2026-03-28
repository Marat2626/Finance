package com.example.finances


import android.app.Application
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Database
import com.example.finances.utils.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.Boolean
import kotlin.String
import kotlin.collections.set

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun settingsScreen(
    onSave: () -> Unit,
    onClipAddGoal: () -> Unit,
    onHomeClick: () -> Unit,
    onClipAddOperation: () -> Unit,
    onSettingScreen: () -> Unit,
    onAI: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val scope = rememberCoroutineScope()

    var showExpenseSettings by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var expensesForCategory by remember { mutableStateOf<List<ExpenseWithImportance>>(emptyList()) } // === ViewModel для категорий расходов ===


    val expensesViewModel: EISviewModel = viewModel(
        key = "expenses",
        factory = EISViewModelFactory(app, { it.getAllExpenses() }, "Расходы")
    )

    val importViewModel: EISviewModel = viewModel(
        key = "import",
        factory = EISViewModelFactory(app, { it.GetAll() }, "Импорт")
    )

    val transactions by expensesViewModel.transactions.collectAsState()

    val settingsViewModel: CategorySettingsViewModel = viewModel()
    val savedSettings by settingsViewModel.categories.collectAsState()

    val categorySelections = remember { mutableStateMapOf<String, ImportanceType>() }
    var isLoading by remember { mutableStateOf(true) }
    var isImporting by remember { mutableStateOf(false) } // Состояние для индикатора импорта


    val expenseSettingsViewModel: ExpenseSettingsViewModel = viewModel()




    // Загрузка
    LaunchedEffect(Unit) {
        settingsViewModel.loadCategories()
    }
    LaunchedEffect(savedSettings) {
        categorySelections.clear()
        savedSettings.forEach { setting ->
            setting.importance?.let { categorySelections[setting.category] = it }
        }
        isLoading = false
    }

    val allCategories = remember(transactions) {
        transactions.map { it.category }.distinct().sorted()
    }

    val categoriesWithSelection = remember(allCategories, categorySelections.toMap()) {
        allCategories.map { category ->
            CategoryWithSelection(
                category = category,
                importance = categorySelections[category],
                count = transactions.count { it.category == category }
            )
        }.sortedByDescending { it.count }
    }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showSkipDialog by remember { mutableStateOf(false) }

    // === Launcher для выбора файла ===
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isImporting = true
                try {
                    // Используем существующий ViewModel для импорта

                    importViewModel.addTransactionsFromUri(context, it)
                    Toast.makeText(context, "Файл успешно загружен", Toast.LENGTH_SHORT).show()
                    // Обновляем данные
                    expensesViewModel.loadData()
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isImporting = false
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        } else {
            Toast.makeText(context, "Нужно разрешение для чтения файлов", Toast.LENGTH_SHORT).show()
        }
    }




    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.screenBlue),

    ) {

        Spacer(modifier = Modifier.height(15.dp))

        Text(
            text = "Финансовые настройки",
            color = Color(0xFFD4AF37),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                // === СЕКЦИЯ КАТЕГОРИИ (главная фича) ===
                item {


                    // Список категорий с возможностью перехода к детальной настройке
                    categoriesWithSelection.forEach { categoryWithSelection ->
                        CategoryCardWithDetail(
                            category = categoryWithSelection.category,
                            count = categoryWithSelection.count,
                            importance = categoryWithSelection.importance,
                            onImportanceChange = { newImportance ->
                                // Обновление на уровне категории
                                if (categorySelections[categoryWithSelection.category] == newImportance) {
                                    categorySelections.remove(categoryWithSelection.category)
                                } else {
                                    categorySelections[categoryWithSelection.category] =
                                        newImportance
                                }
                            },
                            onDetailClick = {
                                val currentCategory =
                                    categoryWithSelection.category   // ← сохраняем сразу

                                Log.d(
                                    "DETAIL_DEBUG",
                                    "═══════════════════════════════════════════════════════════════"
                                )
                                Log.d(
                                    "DETAIL_DEBUG",
                                    "Открываем детальную настройку для: $currentCategory"
                                )


                                // 1. Загружаем все детальные настройки
                                expenseSettingsViewModel.loadExpensesWithSettings()


                                val allDetailed =
                                    expenseSettingsViewModel.categoriesWithExpenses.value
                                        .find { it.category == currentCategory }
                                        ?.expenses ?: emptyList()

                                Log.d(
                                    "DETAIL_DEBUG",
                                    "Загружено детальных настроек: ${allDetailed.size}"
                                )


                                // 3. Получаем уникальные ключи из транзакций
                                val uniqueKeys = transactions
                                    .filter { it.category == currentCategory }
                                    .map { getTransactionKey(it.description) }
                                    .distinct()
                                    .sorted()

                                uniqueKeys.forEach { key ->
                                    Log.d("DETAIL_DEBUG", "  • '$key'")
                                }

                                Log.d(
                                    "DETAIL_DEBUG",
                                    "Уникальных ключей из транзакций: ${uniqueKeys.size}"
                                )
                                // 4. Формируем список с сохранёнными значениями
                                expensesForCategory = uniqueKeys.map { key ->
                                    val saved = allDetailed.find { it.description == key }
                                    Log.d(
                                        "DETAIL_DEBUG",
                                        "Ключ '$key' → сохранён: ${saved?.importance ?: "НЕТ"}"
                                    )
                                    ExpenseWithImportance(
                                        description = key,
                                        importance = saved?.importance,
                                        isUserDefined = saved?.isUserDefined ?: false
                                    )
                                }


                                // 5. Открываем экран
                                selectedCategory = currentCategory
                                showExpenseSettings = true
                            }


                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
        }

        // === ДОПОЛНИТЕЛЬНЫЕ НАСТРОЙКИ ===
            importingStatements(
                permissionLauncher,
                filePickerLauncher,
                isImporting
            )

        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
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
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD4AF37)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Сохранить настройки категорий",
                    color = Color(0xFF1E2F4A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
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

    // ====================== ДИАЛОГИ СОХРАНЕНИЯ ======================
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Сохранить настройки?", color = Color.White) },
            text = { Text("Настройки будут использоваться для расчёта стратегий накопления", color = Color.White.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            settingsViewModel.saveSelectedCategories(categorySelections)
                            showSaveDialog = false
                            onSave()           // ← твой callback
                        }
                    }
                ) { Text("Сохранить", color = Color(0xFF6BCB6B)) }
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
                    Text("Вы не выбрали важность для $unselectedCount категорий.", color = Color.White.copy(alpha = 0.7f))
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
                ) { Text("Сохранить отмеченные", color = Color(0xFF6BCB6B)) }
            },
            dismissButton = {
                TextButton(onClick = { showSkipDialog = false }) {
                    Text("Вернуться", color = Color(0xFFFF6B6B))
                }
            },
            containerColor = Color(0xFF1E2F4A)
        )
    }


    // Детальный экран (оставляем как есть)
    if (showExpenseSettings && selectedCategory != null) {
        ExpenseSettingsScreen(
            category = selectedCategory!!,
            onBack = {
                showExpenseSettings = false
                selectedCategory = null
            }
        )
    }

}

@Composable
fun selectCategory(
    allCategories: List<String>,
    transactions: List<DateExcel>,
    isLoading: Boolean,
    categoriesWithSelection: List<CategoryWithSelection>,
    categorySelections: SnapshotStateMap<String, ImportanceType>,
    showSaveDialog:() -> Unit,
    showSkipDialog: () -> Unit



    ){

    Text(
        text = "Финансовые настройки",
        color = Color(0xFFD4AF37),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A3A6A)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier  .height(250.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Категории расходов", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${allCategories.size} категорий • ${transactions.size} операций",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
                Button(
                    onClick = {
                        val allSelected = allCategories.all { categorySelections.containsKey(it) }
                        if (allSelected) showSaveDialog() else showSkipDialog()
                    },
                    modifier = Modifier
                        .width(70.dp)
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Сохранить",
                        color = Color(0xFF1E2F4A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFFD4AF37), modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(
                    modifier = Modifier.height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoriesWithSelection) { item ->
                        CategoryCard(
                            category = item.category,
                            count = item.count,
                            importance = item.importance,
                            onImportanceChange = { newImportance ->
                                if (categorySelections[item.category] == newImportance) {
                                    categorySelections.remove(item.category)
                                } else {
                                    categorySelections[item.category] = newImportance
                                }
                            }
                        )
                    }
                }
            }
        }
    }


}
@Composable
fun user(){

    Column(
        modifier = Modifier
            .width(356.dp)
            .height(200.dp)
            .background(Color(0xFF2A3A6A),
                shape = RoundedCornerShape(16.dp)),

    ){

    }

}
@Composable
fun importingStatements(
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    filePickerLauncher:ManagedActivityResultLauncher<String, Uri?>,
    isImporting: Boolean){
    Text(
        text = "Дополнительно",
        color = Color(0xFFD4AF37),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
    )

    // Обновленный SettingsItem с выбором файла
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A3A6A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // Проверяем версию Android и запрашиваем разрешения
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        // Android 13+
                        permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                    } else if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
                        // Android 10 и ниже
                        permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    } else {
                        // Android 11-12 - не требуют разрешений
                        filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    }
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Импорт банковской выписки",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Загрузить Excel из памяти телефона",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
            if (isImporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFFD4AF37),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.AccountBox, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun CategoryCardWithDetail(
    category: String,
    count: Int,
    importance: ImportanceType?,
    onImportanceChange: (ImportanceType) -> Unit,
    onDetailClick: () -> Unit
) {

    Spacer(modifier = Modifier.height(16.dp))
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A3A6A)),
        shape = RoundedCornerShape(12.dp),
        onClick =  onDetailClick
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text("$count раз", color = Color(0xFFD4AF37).copy(alpha = 0.7f), fontSize = 12.sp)
            }


            Spacer(modifier = Modifier.height(8.dp))


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ImportanceChip("Обязательные", "🏠", importance == ImportanceType.ESSENTIAL, Color(0xFF6BCB6B), { onImportanceChange(ImportanceType.ESSENTIAL) }, Modifier.weight(1f))
                ImportanceChip("Нейтральные", "⚖️", importance == ImportanceType.NEUTRAL, Color(0xFF6B9CFF), { onImportanceChange(ImportanceType.NEUTRAL) }, Modifier.weight(1f))
                ImportanceChip("Необязательные", "🎯", importance == ImportanceType.NON_ESSENTIAL, Color(0xFFFF6B6B), { onImportanceChange(ImportanceType.NON_ESSENTIAL) }, Modifier.weight(1f))
            }


            Spacer(modifier = Modifier.height(8.dp))

            if (importance == null) {
                Text("⚠️ Важность не выбрана", color = Color(0xFFFF6B6B), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: String,
    count: Int,
    importance: ImportanceType?,
    onImportanceChange: (ImportanceType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A3A6A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(category, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text("$count раз", color = Color(0xFFD4AF37).copy(alpha = 0.7f), fontSize = 12.sp)
            }


            Spacer(modifier = Modifier.height(8.dp))


            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ImportanceChip("Обязательные", "🏠", importance == ImportanceType.ESSENTIAL, Color(0xFF6BCB6B), { onImportanceChange(ImportanceType.ESSENTIAL) }, Modifier.weight(1f))
                ImportanceChip("Нейтральные", "⚖️", importance == ImportanceType.NEUTRAL, Color(0xFF6B9CFF), { onImportanceChange(ImportanceType.NEUTRAL) }, Modifier.weight(1f))
                ImportanceChip("Необязательные", "🎯", importance == ImportanceType.NON_ESSENTIAL, Color(0xFFFF6B6B), { onImportanceChange(ImportanceType.NON_ESSENTIAL) }, Modifier.weight(1f))
            }


            if (importance == null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("⚠️ Важность не выбрана", color = Color(0xFFFF6B6B), fontSize = 10.sp)
            }
        }
    }
}


@Composable
fun ImportanceChip(
    text: String,
    icon: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) color else Color(0xFF1E2F4A),
            contentColor = if (isSelected) Color.Black else Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        border = if (!isSelected) BorderStroke(1.dp, Color(0xFF4C67A7)) else null
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 14.sp)
            Spacer(Modifier.width(4.dp))
            Text(text, fontSize = 11.sp, maxLines = 1)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseSettingsScreen(
    category: String,
    onBack: () -> Unit
) {
    val expenseVM: ExpenseSettingsViewModel = viewModel()
    val categoriesWithExpenses by expenseVM.categoriesWithExpenses.collectAsState()
    val selections = remember { mutableStateMapOf<String, ImportanceType>() }
    val context = LocalContext.current


    // Загружаем самые свежие данные каждый раз при открытии
    LaunchedEffect(category) {
        expenseVM.loadExpensesWithSettings()
    }


    val currentCategoryExpenses = categoriesWithExpenses
        .find { it.category == category }
        ?.expenses ?: emptyList()


    // Инициализация выбранных значений (детальные настройки имеют приоритет)
    LaunchedEffect(currentCategoryExpenses) {
        selections.clear()
        currentCategoryExpenses.forEach { exp ->
            exp.importance?.let {
                selections["$category|||${exp.description}"] = it
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.screenBlue)
    ) {
        TopAppBar(
            title = { Text("Настройка: $category", color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, tint = Color.White, contentDescription = "Назад")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2A3A6A))
        )


        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(currentCategoryExpenses) { expense ->
                ExpenseSettingCard(
                    description = expense.description,
                    importance = selections["$category|||${expense.description}"],
                    onImportanceChange = { newImportance ->
                        val key = "$category|||${expense.description}"
                        if (selections[key] == newImportance) {
                            selections.remove(key)
                        } else {
                            selections[key] = newImportance
                        }
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }


        // Кнопка сохранения внизу (всегда видна)
        Surface(
            color = Color(0xFF2A3A6A),
            tonalElevation = 8.dp
        ) {
            Column {
                Button(
                    onClick = {
                        if (selections.isEmpty()) {
                            Toast.makeText(context, "Выберите важность хотя бы для одной траты", Toast.LENGTH_SHORT).show()
                            return@Button
                        }


                        // Сохраняем
                        expenseVM.saveSelectedExpenses(selections.toMap())


                        // Безопасное форматирование для Toast (если хочешь показать сумму)
                        val totalSelected = selections.size
                        Toast.makeText(
                            context,
                            "✅ Сохранено $totalSelected описаний",
                            Toast.LENGTH_SHORT
                        ).show()


                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Сохранить настройки трат",
                        color = Color(0xFF1E2F4A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }



                // Дополнительная кнопка — быстро применить настройку категории ко всем тратам
                TextButton(
                    onClick = { /* позже добавим логику */ },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text("Применить настройку категории ко всем тратам", color = Color(0xFFD4AF37))
                }
            }
        }
    }
}







@Composable
fun ExpenseSettingCard(
    description: String,
    importance: ImportanceType?,
    onImportanceChange: (ImportanceType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A3A6A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                text = description,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ImportanceChip(
                    "Обязательные", "🏠",
                    importance == ImportanceType.ESSENTIAL,
                    Color(0xFF6BCB6B),
                    { onImportanceChange(ImportanceType.ESSENTIAL) },
                    Modifier.weight(1f)
                )
                ImportanceChip(
                    "Нейтральные", "⚖️",
                    importance == ImportanceType.NEUTRAL,
                    Color(0xFF6B9CFF),
                    { onImportanceChange(ImportanceType.NEUTRAL) },
                    Modifier.weight(1f)
                )
                ImportanceChip(
                    "Необязательные", "🎯",
                    importance == ImportanceType.NON_ESSENTIAL,
                    Color(0xFFFF6B6B),
                    { onImportanceChange(ImportanceType.NON_ESSENTIAL) },
                    Modifier.weight(1f)
                )
            }
        }
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
data class CategoryWithSelection(
    val category: String,
    val importance: ImportanceType?,
    val count: Int
)
data class ExpenseWithImportance(
    val description: String,
    val importance: ImportanceType? = null,
    val isUserDefined: Boolean = false
)