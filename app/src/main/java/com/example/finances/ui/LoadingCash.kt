package com.example.finances

import android.R.attr.category
import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun loadingCash(onClip:() -> Unit){

    val context = LocalContext.current
    val app = context.applicationContext as Application


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









    Column (
        Modifier
            .fillMaxSize()
            .background(Color(0xFF5D5C5D)),
        Arrangement.SpaceEvenly,
        Alignment.CenterHorizontally
    )
    {
        Column (Modifier
            .height(270.dp)
            .width(300.dp)
            .border(width = 3.dp, shape = RoundedCornerShape(10.dp), brush = AppColors.YRG),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            cashInput( "Накопление", AppColors.forestMetalGradientL, 170,70,   onSaveClicked = { amount, category ->  // ← убрал _
                viewModelS.addManualSave(amount, category)  // ← передаем выбранную категорию
            }  )
            Spacer(modifier = Modifier.height(45.dp))

            Row {
                cashInput( "Расход", AppColors.alphaRedGradientL, 120, 70,  onExpenseClicked = { amount, category -> viewModelE.addManualSave(amount, category ) })

                Spacer(modifier = Modifier.width(20.dp))

                cashInput( "Доход", AppColors.tbankYellowGradientL, 120,70, onIncomeClicked = { amount, category ->viewModelI.addManualSave(amount, category,) })

//                viewModelI.addManualIncome(amount, category)
            }
        }


        Button(
            onClick = { onClip() },
            border = BorderStroke(3.dp, brush = AppColors.royalGoldGradientL),
            colors = ButtonDefaults.buttonColors(Color(0xD7343434)),
            modifier = Modifier.height(100.dp).width(250.dp),
            shape = RoundedCornerShape(10.dp),
            ){
            Text(
                style = TextStyle(AppColors.royalGoldGradientV),
                textAlign = TextAlign.Center,
                text = "Перейти на главную",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun cashInput(
              text: String,
              borderColor: Brush,
              width: Int,
              height: Int,
              onExpenseClicked: ((Double, String) -> Unit)? = null,
              onIncomeClicked: ((Double, String) -> Unit)? = null,
              onSaveClicked: ((Double, String) -> Unit)? = null

)
{


    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedAmount by remember { mutableStateOf("") }


    var showOutline by remember { mutableStateOf(false) }



    Button(
        border = BorderStroke(3.dp, brush = borderColor),
        colors = ButtonDefaults.buttonColors(Color(0xD7343434)),
        modifier = Modifier.height(height.dp).width(width.dp),
        shape = RoundedCornerShape(10.dp),
        onClick = { showBottomSheet = true }

    ) {
        Text(
            text = text,
            style = TextStyle(AppColors.royalGoldGradientV),
            fontSize = 21.sp,
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            dragHandle = {
                Box(
                    modifier = Modifier
                        .background(
                            color = Color.Gray,  // или твой цвет
                            shape = RoundedCornerShape(2.dp)
                        )

                )
            },

            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xD74F4D4D)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Spacer(Modifier.padding(top = 20.dp))
                Text(
                    text = text,
                    style = TextStyle(AppColors.royalGoldGradientV),
                    fontSize = 30.sp,
                )

                Spacer(Modifier.padding(top = 10.dp))

                Text(
                    text = "Выбирете категорию",
                    style = TextStyle(AppColors.royalGoldGradientV),
                    fontSize = 30.sp,
                )
                Spacer(Modifier.padding(top = 15.dp))

                val expenseCategories = listOf("Супермаркет", "Транспорт", "Кафе и рестораны",
                    "Развлечения", "Одежда и обувь", "Подписки",
                    "Здоровье", "Переводы", "Коммуналка", "Топливо")

                val incomeCategories = listOf("Зарплата", "Фриланс", "Инвестиции",
                    "Подарок", "Возврат долга", "Проценты по вкладу")

                val saveCategories = listOf("Резервный фонд", "На отпуск", "На машину",
                    "На квартиру", "Инвестиции", "Образование")


                val categories = when (text) {
                    "Расход" -> expenseCategories
                    "Доход" -> incomeCategories
                    "Накопление" -> saveCategories
                    else -> emptyList()
                }

                LazyRow(
                ) {
                    items(categories) { categoryName ->
                        category(
                            categoryName,
                            showOutline = { isVisible ->
                                showOutline = isVisible
                            },
                            onCategorySelected = { category ->
                                selectedCategory = category
                            }
//
                        )
                    }
                }

                Spacer(Modifier.padding(top = 30.dp))


                if (showOutline) {
                    OutlinedTextField(
                        value = selectedAmount  ,
                        onValueChange = { newValue ->
                            selectedAmount = newValue
                        },
                        placeholder = {
                            Text(
                                text = "Введите сумму",
                                style = TextStyle(AppColors.royalGoldGradientV),
                                fontSize = 19.sp,
                            )

                        },
                        textStyle = TextStyle(
                            brush = AppColors.royalGoldGradientV, // Тот же градиент
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .width(300.dp)
                            .height(55.dp)
                            .border(
                                width = 3.dp,
                                shape = RoundedCornerShape(10.dp),
                                brush = AppColors.royalGoldGradientL
                            )
                            .background(Color(0x703B3B3B)),
                    )
                }
                Button(
                    border = BorderStroke(3.dp, brush = borderColor),
                    colors = ButtonDefaults.buttonColors(Color(0xD7343434)),
                    modifier = Modifier.height(50.dp).width(160.dp),
                    shape = RoundedCornerShape(10.dp),
                    onClick = {


                        val amount = selectedAmount.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            when (text) {
                                "Расход" -> onExpenseClicked?.invoke(amount, selectedCategory)
                                "Доход" -> onIncomeClicked?.invoke(amount, selectedCategory)
                                "Накопление" -> onSaveClicked?.invoke(amount, selectedCategory)
                            }
                        }

                        showBottomSheet = false
                    }
                ) {
                    Text(text = "Сохранить",
                        style = TextStyle(AppColors.royalGoldGradientV),
                        fontSize = 21.sp,)
                }
            }
        }
    }
}


@Composable
fun category(text: String, showOutline: (Boolean) -> Unit,  onCategorySelected: (String) -> Unit ){

    Button(
        onClick = {
            showOutline(true)
            onCategorySelected(text) },
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