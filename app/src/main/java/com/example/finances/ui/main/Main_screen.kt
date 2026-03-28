package com.example.finances

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finances.utils.AppColors
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun mainScreen(
    onClipAddGoal: () -> Unit,
    onClipAddOperation: () -> Unit,
    onHomeClick: () -> Unit,
    onSettingScreen: () -> Unit,
    onAI: () -> Unit
) {


    val viewModelB: BalanseViewModel = viewModel()
    val balance by viewModelB.balance.collectAsState()

    val context = LocalContext.current
    val db = DataTransactions.getDatabase(context)
    val showFilterPanel = remember { mutableStateOf(false) }


    val pieCharts = PieCharts()

    val app = context.applicationContext as Application


    val transactionsFlow = db.dataExcelDao().GetAll()
    val transaction by transactionsFlow.collectAsState(initial = emptyList())


    val viewModelE: EISviewModel = viewModel(
        key = "expenses",
        factory = EISViewModelFactory(app, { it.getAllExpenses() }, "Расходы")
    )
    val viewModelI: EISviewModel = viewModel(
        key = "incomes",
        factory = EISViewModelFactory(app, { it.getAllIncome() }, "Зачисления")
    )

    val viewModelS: EISviewModel = viewModel(
        key = "save",
        factory = EISViewModelFactory(app, { it.getAllSave() }, "Накопление")
    )
    val getAllS by viewModelS.transactions.collectAsState()
    val getAllE by viewModelE.transactions.collectAsState()
    val getAllI by viewModelI.transactions.collectAsState()

    var amuntE by remember { mutableStateOf(0.0) }
    var amuntI by remember { mutableStateOf(0.0) }


    val now = LocalDate.now()
    val currentYear = now.year
    val currentMonth = now.month

    LaunchedEffect(getAllE, getAllI) {


        amuntE = getAllE.filter {
            it.date.year == currentYear && it.date.month == currentMonth
        }.sumOf { it.amount }

        amuntI = getAllI.filter {
            it.date.year == currentYear && it.date.month == currentMonth
        }.sumOf { it.amount }

    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.screenBlue),


        horizontalAlignment = Alignment.CenterHorizontally,

        )
    {
        Spacer(modifier = Modifier.padding(bottom = 5.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp,),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка фильтра
            Button(
                onClick = { },
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
        }
        if (showFilterPanel.value) {
//            FilterPanel( )
        }



            Spacer(modifier = Modifier.padding(bottom = 5.dp))

            Row(

            ) {
                box("Расходы", amuntE,)
                Spacer(modifier = Modifier.width(15.dp))
                box("Доходы", amuntI,)
            }

            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .width(330.dp)
                    .height(180.dp)
            ) {
                pieCharts.Chart(
                    db, "План", modifier = Modifier
                        .height(270.dp)
                        .width(270.dp), transactionsFlow
                )
            }




            Box(
                modifier = Modifier.weight(1f) // занимает всё доступное место
            ) {
                // График транзакций
                TransactionsLineChart(transaction)
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
    fun box(text: String, amount: Double) {

        Box(
            modifier = Modifier
                .height(130.dp)
                .width(150.dp)
                .background(
                    AppColors.radialBlueGradient,
                    shape = RoundedCornerShape(17.dp)
                )
                .padding(vertical = 5.dp)
        )
        {
            Column {
                Text(
                    style = TextStyle(AppColors.royalGoldGradientL),
                    textAlign = TextAlign.Center,
                    text = "$text",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.W500,
                    modifier = Modifier.padding(start = 10.dp, top = 10.dp)
                )

                Text(
                    style = TextStyle(AppColors.royalGoldGradientL),
                    textAlign = TextAlign.Center,
                    text = "₽${"%.2f".format(amount)} ",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.W500,
                    modifier = Modifier.padding(start = 10.dp, top = 10.dp)
                )

                Text(
                    style = TextStyle(AppColors.royalGoldGradientL),
                    textAlign = TextAlign.Center,
                    text = "+20%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.W500,
                    modifier = Modifier.padding(start = 10.dp, top = 10.dp)
                )
            }
        }
    }




