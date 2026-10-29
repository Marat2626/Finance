package com.example.finances


import android.graphics.BlurMaskFilter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finances.utils.AppColors
import com.example.finances.utils.cardShadow
import org.apache.poi.sl.usermodel.Background
import java.nio.file.WatchEvent

import android.util.Log
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun mainScreen(
    onClipAddGoal: () -> Unit,
    onClipAddOperation: () -> Unit,
    onHomeClick: () -> Unit,
    onSettingScreen: () -> Unit,
    onAI: () -> Unit
) {
    val filterViewModel: FilterViewModel = viewModel()

    val showFilterPanel by filterViewModel.showFilterPanel.collectAsState()
    val selectedPeriod by filterViewModel.selectedPeriod.collectAsState()
    val filteredTransactions by filterViewModel.filteredTransactions.collectAsState()

    val expensesSum by filterViewModel.expensesSum.collectAsState()
    val incomesSum by filterViewModel.incomesSum.collectAsState()
    val expensesChangePercent by filterViewModel.expensesChangePercent.collectAsState()
    val incomesChangePercent by filterViewModel.incomesChangePercent.collectAsState()

    val context = LocalContext.current
    val db = DataTransactions.getDatabase(context)
    val transactionsFlow = db.dataExcelDao().GetAll()
    val allTransactions by transactionsFlow.collectAsState(initial = emptyList())


    val pieCharts = PieCharts()

    val viewModelB: BalanseViewModel = viewModel()
    val balance by viewModelB.balanceFlow.collectAsState()
    LaunchedEffect(allTransactions) {
        filterViewModel.setTransactions(allTransactions)
    }
    LaunchedEffect(balance) {
        Log.d("MainScreen", "Баланс: $balance")
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(AppColors.current.screenBG)
    ) {
            LazyColumn(
                modifier = Modifier
                    .padding(top = 40.dp)

                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                item {

                    if (showFilterPanel) {
                        FilterPanel(
                            selectedPeriod = selectedPeriod,
                            onPeriodChange = { filterViewModel.updatePeriod(it) },
                            onClose = { filterViewModel.toggleFilterPanel() },
                            onReset = { filterViewModel.resetFilters() }
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Column(modifier = Modifier
                        .height(160.dp)
                        .cardShadow()

                        .padding(horizontal = 16.dp)
                        .background(AppColors.current.cardBox, RoundedCornerShape(20.dp))
                        .padding( 16.dp),
                        )
                    {

                    Row(

                      ) {
                        boxStatic(
                            text = "Расходы",
                            amount = expensesSum,
                            changePercent = expensesChangePercent,
                            procentColor = if (expensesChangePercent > 15) AppColors.current.text else AppColors.current.text,
                            color =  AppColors.current.text,
                            modifier = Modifier
                                .weight(1f)
                                .background(AppColors.current.cardBoxRed, RoundedCornerShape(20.dp))
                                .padding(vertical = 5.dp)

                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        boxStatic(
                            text = "Доходы",
                            amount = incomesSum,
                            changePercent = incomesChangePercent,
                            procentColor = if (incomesChangePercent < 0) AppColors.current.text else AppColors.current.text,
                            color = AppColors.current.text,
                            modifier = Modifier
                                .weight(1f)

                                .background(AppColors.current.cardBoxGreen, RoundedCornerShape(20.dp))
                                .padding(vertical = 5.dp)
                        )
                    }
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding( top = 10.dp, )
                                .background(AppColors.current.screenBG,RoundedCornerShape(10.dp))
                                .padding(start = 5.dp, end = 5.dp)
                             ,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Text(
                                text = "Остаток",
                                style = TextStyle( AppColors.current.text),
                                fontSize = 12.sp
                            )

                            Text(
                                text = "~ ${balance.formatWithSpace()} ₽",
                                style = TextStyle(   AppColors.current.text,),
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier

                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 17.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .cardShadow()

                                .background(
                                    AppColors.current.cardBox,
                                    shape = RoundedCornerShape(25.dp)
                                )
                                .weight(1f),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            pieCharts.Chart(
                                transactions = filteredTransactions,
                                centerText = "Структура",
                                modifier = Modifier
                                    .size(130.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier.weight(1f)
                                .cardShadow()
                                .background(
                                    AppColors.current.cardBox,
                                    shape = RoundedCornerShape(25.dp)
                                )
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            categoryOfImportance()
                        }
                    }

                        TopCategoriesChart(filteredTransactions)

                    Spacer( modifier = Modifier.padding(top = 20.dp))


            }

        }


        Row (
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ){


                Text(
                    text = "Главная",
                    style = TextStyle(
                        color = AppColors.current.icones,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(start = 20.dp)
                )


            Button(
                modifier = Modifier.padding(end = 20.dp),
                onClick = { filterViewModel.toggleFilterPanel() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.filter),
                        modifier = Modifier
                            .size(24.dp),
                        contentDescription = "Фильтер",
                        tint = AppColors.current.icones,

                    )
                    Text(
                        text = if (selectedPeriod == "Все время") "Фильтр" else selectedPeriod,
                        style = TextStyle(
                           color = AppColors.current.icones,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
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
                { onAI() },
            )
        }
    }
}

@Composable
fun boxStatic(
    text: String,
    amount: Double,
    changePercent: Int,
    procentColor: Color,
    modifier: Modifier,
    color: Color
) {
    val procentText = when {
        changePercent > 0 -> "+$changePercent%"
        changePercent < 0 -> "$changePercent%"
        else -> "0%"
    }

    Box(
        modifier = modifier
    ) {
        Column {
            Text(
                style = TextStyle(  color = AppColors.current.text,),
                textAlign = TextAlign.Center,
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 10.dp, top = 5.dp)
            )

            Text(
                color = color,
                textAlign = TextAlign.Center,
                text = "${amount.formatWithSpace()} ₽",
                fontSize = 23.sp,
                fontWeight = FontWeight.W500,
                modifier = Modifier.padding(start = 10.dp, )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    style = TextStyle(procentColor),
                    textAlign = TextAlign.Center,
                    text = procentText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 10.dp, top = 5.dp)
                )
                Text(
                     style = TextStyle(color =  AppColors.current.text) ,
                    text = "к прошлому мес.",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 5.dp, top = 5.dp)
                )
            }
        }
    }
}

@Composable
fun FilterPanel(
    selectedPeriod: String,
    onPeriodChange: (String) -> Unit,
    onClose: () -> Unit,
    onReset: () -> Unit
) {
    val periodOptions = listOf(
        "Все время",
        "Год",
        "Текущий месяц",
        "Прошлый месяц",
        "Сегодня",
        "Неделя",
        "6 месяцев",
    )


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(AppColors.current.cardBox, shape = RoundedCornerShape(25.dp))
            .border(BorderStroke(
                width =  1.dp,
                color = AppColors.current.cardBox,

            ),
                shape = RoundedCornerShape(25.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Фильтры",
                    style = TextStyle(color = AppColors.current.text) ,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    TextButton(onClick = onReset) {
                        Text("Сброс", style = TextStyle(color = AppColors.current.text) , fontSize = 16.sp)
                    }
                    IconButton(onClick = onClose) {
                        Text("✕",  style = TextStyle(color =  AppColors.current.text) , fontSize = 16.sp)
                    }
                }
            }


            Text(
                "Период",
                style = TextStyle(color =  AppColors.current.text) ,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))


            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(periodOptions) { period ->
                    FilterChip(
                        text = period,
                        isSelected = selectedPeriod == period,
                        onSelected = { onPeriodChange(period) }
                    )
                }
            }


            Spacer(modifier = Modifier.height(24.dp))


            Box(modifier = Modifier.background( AppColors.casesBlue, shape = RoundedCornerShape(12.dp))) {
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Применить",    style = TextStyle(AppColors.current.iconsWhite), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}



