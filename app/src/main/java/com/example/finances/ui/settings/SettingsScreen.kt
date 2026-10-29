package com.example.finances


import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finances.utils.AppColors
import com.example.finances.utils.AppTheme
import com.example.finances.utils.cardShadow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.WatchEvent


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun settingsScreen(
    onClipAddGoal: () -> Unit,
    onHomeClick: () -> Unit,
    onClipAddOperation: () -> Unit,
    onSettingScreen: () -> Unit,
    onAI: () -> Unit,
) {

    val balanceViewModel: BalanseViewModel = viewModel()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importViewModel: EISviewModel = viewModel(
        key = "import",
        factory = EISViewModelFactory(
            context.applicationContext as Application,
            { it.GetAll() },
            "Импорт",

            )
    )


    // ViewModel для получения данных
    val expensesViewModel: EISviewModel = viewModel(
        key = "expenses",
        factory = EISViewModelFactory(
            context.applicationContext as Application,
            { it.getAllExpenses() },
            "Расходы",

            )
    )
    val expenses by expensesViewModel.transactions.collectAsState()

    val incomesViewModel: EISviewModel = viewModel(
        key = "incomes",
        factory = EISViewModelFactory(
            context.applicationContext as Application,
            { it.getAllIncome() },
            "Зачисления",
        )
    )
    val incomes by incomesViewModel.transactions.collectAsState()

    val savingsViewModel: EISviewModel = viewModel(
        key = "savings",
        factory = EISViewModelFactory(
            context.applicationContext as Application,
            { it.getAllSave() },
            "Накопление",
        )
    )
    val savings by savingsViewModel.transactions.collectAsState()

    val goalViewModel: GoalViewModel = viewModel()
    val goals by goalViewModel.transactions.collectAsState()

    val categorySettingsViewModel: CategorySettingsViewModel = viewModel()
    val categorySettings by categorySettingsViewModel.categories.collectAsState()

    val expenseSettingsViewModel: ExpenseSettingsViewModel = viewModel()
    val expenseSettings by expenseSettingsViewModel.categoriesWithExpenses.collectAsState()


    // Все транзакции
    val allTransactions = remember(expenses, incomes, savings) {
        expenses + incomes + savings
    }

    var isClearing by remember { mutableStateOf(false) }
    val databaseHelper = remember { DatabaseHelper(context) }

    val exportHelper = remember { ExportHelper(context) }

    var showBalanceDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    // Launcher для выбора Excel файла
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    importViewModel.addTransactionsFromUri(context, it)
                    Toast.makeText(context, "Файл успешно загружен", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    Box {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.current.screenBG)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                var showClearDialog by remember { mutableStateOf(false) }

                // Секция Данные
                boxes(
                    text = "Загрузка excel",
                    text1 = "Выгрузка excel",
                    text3 = "Очистка данных",
                    title = "Данные и резервное копирование",
                    iconSber = R.drawable.sber,
                    iconExcel = R.drawable.file_type_icon,
                    iconBasket = R.drawable.basket,
                    filePickerLauncher = filePickerLauncher,
                    onExportClick = {
                        scope.launch {
                            val success = exportHelper.exportAllData(
                                transactions = allTransactions,
                                goals = goals,
                                categorySettings = categorySettings.map {
                                    CategorySettings(
                                        category = it.category,
                                        importance = it.importance ?: ImportanceType.ESSENTIAL,
                                        isUserDefined = it.isUserDefined
                                    )
                                },
                                expenseSettings = expenseSettings.flatMap { category ->
                                    category.expenses.map { expense ->
                                        ExpenseSettings(
                                            category = category.category,
                                            description = expense.description,
                                            importance = expense.importance
                                                ?: ImportanceType.ESSENTIAL,
                                            isUserDefined = expense.isUserDefined
                                        )
                                    }
                                },
                                balance = Balance(
                                    currentBalance = 0.0
                                )
                            )
                            if (success) {
                                Toast.makeText(context, "Экспорт завершён", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                Toast.makeText(context, "Ошибка экспорта", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    },
                    onClearClick = {
                        scope.launch {
                            val success = databaseHelper.clearAllData()
                            if (success) {
                                Toast.makeText(context, "Все данные очищены", Toast.LENGTH_SHORT)
                                    .show()
                                // Обновляем UI (перезагружаем данные)

                            } else {
                                Toast.makeText(context, "Ошибка очистки данных", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }

                    },
                )

                Text(
                    modifier = Modifier.padding(start = 30.dp, bottom = 5.dp),
                    text = "Настройка финансов",
                    style = TextStyle(
                        color =  AppColors.current.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                Card(
                    modifier = Modifier
                        .padding(end = 20.dp, start = 20.dp)
                        .background(AppColors.current.cardBox, shape = RoundedCornerShape(25.dp))
                        .height(70.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent  // ← Важно! Прозрачный контейнер
                    ),

                    ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {

                        settingBox(
                            R.drawable.balanssetting,
                            "Настроить баланс",
                            { showBalanceDialog = true })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (showBalanceDialog) {
                    AlertDialog(
                        modifier = Modifier
                            .background(
                                AppColors.current.cardBox,
                                shape = RoundedCornerShape(20.dp)
                            ),
                        onDismissRequest = { showClearDialog = false },
                        title = {
                            Text(
                                text = "ИЗМЕНИТЬ БАЛАНС",
                                style = TextStyle(
                                    color =  AppColors.current.text,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        },
                        text = {
                            Column {
                                Text(
                                    text = "Введите новую сумму баланса:",
                                    style = TextStyle(
                                        color =  AppColors.current.text,
                                        fontSize = 14.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Поле ввода
                                TextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = {
                                        Text(
                                            "0.00",
                                            style = TextStyle(AppColors.current.textAlfa)
                                        )
                                    },
                                    leadingIcon = {
                                        Text(
                                            "₽",
                                            style = TextStyle(
                                                AppColors.current.textAlfa,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number
                                    ),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedIndicatorColor = Color.Black,
                                        unfocusedIndicatorColor = Color.Black,
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        cursorColor = Color.Black,
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val newBalance = inputText.toDoubleOrNull() ?: 0.0
                                    balanceViewModel.updateBalance(newBalance)
                                    showBalanceDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD32F2F)  // красный
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Ввести", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showBalanceDialog = false }
                            ) {
                                Text(
                                    "Отмена",
                                    style = TextStyle(color = AppColors.current.text)
                                )
                            }
                        },
                        containerColor = Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                // Секция Внешний вид
                boxesTwo(
                    text = "Тёмная/светлая тема",
                    text1 = "Фирменная тема",
                    title = "Внешний вид и интерфейс",
                    iconDartAndWhite = R.drawable.theme,
                    iconStarMyTheme = R.drawable.heart
                )


                Spacer(modifier = Modifier.height(16.dp))





            }

        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)  // вот это прижимает к низу
        ) {
            bottomNavigationBar(
                { onHomeClick() },
                { onClipAddOperation() },
                { onClipAddGoal() },
                { onSettingScreen() },
                { onAI() }
            )
        }
    }
}



@Composable
fun settingsBox(text: String,  iconRes: Int, onClick: () -> Unit, color: Color ){

    Row(
        modifier = Modifier
            .clickable{ onClick() }
            .fillMaxWidth().height(60.dp)
            .padding(top = 15.dp, start = 10.dp),


        ) {

        Icon(
            painter = painterResource(id = iconRes),
            modifier = Modifier
                .size(26.dp),
            contentDescription = "Excel",
            tint = color
        )

        Text(
            modifier = Modifier.padding(start = 10.dp,top = 5.dp),
            text = text,
            style = TextStyle(
                color = AppColors.current.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        )

    }
    Divider(
        modifier = Modifier

            .width(300.dp)
            .padding(start = 25.dp)
            .background(AppColors.current.text),

        thickness = 1.dp
    )
}


@Composable
fun boxes(
    text: String,
    text1: String,
    text3: String,
    title: String,
    iconSber: Int,
    iconExcel: Int,
    iconBasket: Int,
    filePickerLauncher: ManagedActivityResultLauncher<String, Uri?>,   // только этот нужен
    onExportClick: () -> Unit,
    onClearClick: () -> Unit,
) {


    Column(modifier = Modifier

        .padding(20.dp)) {


        Text(
            modifier = Modifier.padding(start = 10.dp, bottom = 5.dp),
            text = title,
            style = TextStyle(
                color = AppColors.current.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        )

        var showClearDialog by remember { mutableStateOf(false) }


        Column(

            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(AppColors.current.cardBox,
                    RoundedCornerShape(25.dp),
                )


        ) {
            Column(modifier = Modifier.fillMaxSize()) {






                // Загрузка Excel
                settingsBox(
                    text = text,
                    iconRes = iconSber,
                    onClick = {
                        filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")

                    },
                    color = Color.Unspecified
                )


                // Выгрузка Excel (пока заглушка)
                settingsBox(
                    text1,
                    iconExcel,
                    onClick = {
                        onExportClick()
                    },
                    color = Color.Unspecified
                )


                settingsBox(
                    text3,
                    iconBasket,
                    onClick = { showClearDialog = true },
                    color = AppColors.current.icones
                )


            }

            if (showClearDialog) {
                AlertDialog(
                    modifier = Modifier
                        .background(AppColors.current.cardBox, shape = RoundedCornerShape(20.dp)),
                    onDismissRequest = { showClearDialog = false },
                    title = {
                        Text(
                            text = "⚠️ Очистка данных",
                            style = TextStyle(
                                color = AppColors.current.text,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    text = {
                        Text(
                            text = "Вы уверены, что хотите удалить ВСЕ данные?\n\n" +
                                    "Будут удалены:\n" +
                                    "• Все транзакции\n" +
                                    "• Все цели\n" +
                                    "• Настройки категорий\n\n" +
                                    "Это действие нельзя отменить!",
                            style = TextStyle(
                                color =  AppColors.current.text,
                                fontSize = 16.sp
                            )
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                onClearClick()  // ← выполняем очистку
                                showClearDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F)  // красный
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Удалить всё", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showClearDialog = false }
                        ) {
                            Text(
                                "Отмена",
                                style = TextStyle(color = AppColors.current.icones)
                            )
                        }
                    },
                    containerColor = Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                )
            }





        }
    }
}


// Первый переключатель: Темная/Светлая тема
@Composable
fun themeSwitch() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = 10.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Switch(
            checked = AppColors.currentTheme == AppTheme.LIGHT,
            onCheckedChange = {
                AppColors.toggleDarkLight()
            },
            enabled = !AppColors.isFavouriteTheme,  // отключаем, если включена фирменная
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF262626),
                checkedTrackColor = Color(0x9A414040).copy(alpha = 0.5f),
                uncheckedThumbColor = Color(0xFF4C67A7),
                uncheckedTrackColor = Color(0xFF4C67A7).copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun favouriteThemeSwitch() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = 10.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Switch(
            checked = AppColors.isFavouriteTheme,
            onCheckedChange = {
                AppColors.toggleFavourite()
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF2196F3),
                checkedTrackColor = Color(0xFF2196F3).copy(alpha = 0.5f),
                uncheckedThumbColor = Color(0xFF4C67A7),
                uncheckedTrackColor = Color(0xFF4C67A7).copy(alpha = 0.5f)
            )
        )
    }
}


@Composable
fun settingBox(icons: Int, text: String, onClick:() -> Unit) {


    Row (
        modifier = Modifier

            .background(AppColors.current.cardBox, shape = RoundedCornerShape(25.dp))
            .height(70.dp)
            .clickable{ onClick() }
            .padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            painter = painterResource(id = icons),
            modifier = Modifier
                .size(26.dp),
            contentDescription = "Excel",
            tint = AppColors.current.icones

        )

        Text(
            modifier = Modifier.padding(start = 10.dp,top = 5.dp),
            text = text,
            style = TextStyle(
                color = AppColors.current.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
    Divider(
        modifier = Modifier
            .width(300.dp)
            .padding(start = 25.dp)
            .background(AppColors.current.text),

        thickness = 1.dp
    )

}
@Composable
fun settingsBoxTwo(text: String,  iconRes: Int,isThemeSwitch: Boolean = false, isFavouriteSwitch: Boolean = false, color: Color){

    Row(
        modifier = Modifier
            .fillMaxWidth().height(60.dp)
            .padding(bottom = 5.dp, start = 10.dp),
        verticalAlignment = Alignment.CenterVertically

        ) {

        Icon(
            painter = painterResource(id = iconRes),
            modifier = Modifier
                .size(26.dp),
            contentDescription = "Excel",
            tint = color

        )

        Text(
            modifier = Modifier.padding(start = 10.dp,top = 5.dp),
            text = text,
            style = TextStyle(
                color = AppColors.current.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        )

        when {
            isThemeSwitch -> themeSwitch()
            isFavouriteSwitch -> favouriteThemeSwitch()
            else -> Spacer(modifier = Modifier)
        }


    }
    Divider(
        modifier = Modifier
            .width(300.dp)
            .padding(start = 25.dp)
            .background(AppColors.current.text),

        thickness = 1.dp
    )
}
@Composable
fun boxesTwo(
    text: String,
    text1: String,
    title: String,
    iconDartAndWhite: Int,
    iconStarMyTheme: Int,
) {

    Column(
        modifier = Modifier

            .padding(end = 20.dp, start = 20.dp)
            .height(150.dp),
        verticalArrangement = Arrangement.Center
    ) {


        Text(
            modifier = Modifier.padding(start = 10.dp, bottom = 10.dp),
            text = title,
            style = TextStyle(
                color =  AppColors.current.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.current.cardBox,
                    RoundedCornerShape(23.dp),
                ),
            verticalArrangement = Arrangement.Center
        ) {

            settingsBoxTwo(text, iconDartAndWhite, isThemeSwitch = true, color = Color.Unspecified)
            // Переключатель Фирменная синяя тема
            settingsBoxTwo(text1, iconStarMyTheme, isFavouriteSwitch = true, color = AppColors.current.icones)
        }

    }

}


class DatabaseHelper(private val context: Context) {

    suspend fun clearAllData(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val db = DataTransactions.getDatabase(context)

            // Очищаем все таблицы
            db.dataExcelDao().deleteAll()
            db.goalDao().deleteAll()
            db.categorySettingsDao().deleteAll()
            db.expenseSettingsDao().deleteAll()
            db.balanceDao().deleteAll()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}