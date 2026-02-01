package com.example.finances

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun loadingCash(onClip:() -> Unit){
    val viewModelE: ExpensesViewModel = viewModel()
    val viewModelI: IncomesViewModel = viewModel()
    val viewModelS: SaveViewModel = viewModel()


    var save by remember { mutableStateOf("")}
    var expense  by remember { mutableStateOf("")}
    var income by remember { mutableStateOf("")}
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
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedTextField(
                value = save,
                onValueChange = { newValue ->
                    save = newValue.filter { it.isDigit() || it == '.' }
                },
                placeholder = {
                    Text(
                        text = "Накопление",
                        style = TextStyle(AppColors.royalGoldGradientV),
                        fontSize = 23.sp
                    )
                              },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .width(170.dp)
                    .height(70.dp)
                    .background(Color(0xD7343434),
                        shape = RoundedCornerShape(14.dp))
                    .border(width = 3.dp, shape = RoundedCornerShape(10.dp), brush = AppColors.forestMetalGradientL),
                textStyle = TextStyle(AppColors.royalGoldGradientV, fontSize = 23.sp),
            )
            Row {
                OutlinedTextField(
                    value = expense,
                    onValueChange = { newValue ->
                        expense = newValue.filter { it.isDigit() || it == '.' }
                                    },
                    placeholder = {
                        Text(
                            text = "Расход",
                            style = TextStyle(AppColors.royalGoldGradientV),
                            fontSize = 23.sp
                        )
                    },                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .width(120.dp)
                        .height(70.dp)
                        .background(Color(0xD7343434),
                            shape = RoundedCornerShape(14.dp))
                        .border(width = 3.dp, shape = RoundedCornerShape(10.dp), brush = AppColors.alphaRedGradientL),
                    textStyle = TextStyle(AppColors.royalGoldGradientV, fontSize = 23.sp),
                    )
                Spacer(modifier = Modifier.width(15.dp))
                OutlinedTextField(
                    value = income,
                    onValueChange = { newValue ->
                income = newValue.filter { it.isDigit() || it == '.' }
                                    },
                    placeholder = { Text(text = "Доход", style = TextStyle(AppColors.royalGoldGradientV), fontSize = 23.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .width(120.dp)
                        .height(70.dp)
                        .background(Color(0xD7343434),
                            shape = RoundedCornerShape(14.dp))
                        .border(width = 3.dp, shape = RoundedCornerShape(10.dp), brush = AppColors.tbankYellowGradientL),
                    textStyle = TextStyle(AppColors.royalGoldGradientV, fontSize = 23.sp)
                )
            }
        }
        Button(
            onClick = {
                val expenseAmount = expense.toDoubleOrNull()
                val incomeAmount = income.toDoubleOrNull()
                val saveAmount = save.toDoubleOrNull()


                if (expenseAmount != null && expenseAmount > 0) {
                    viewModelE.addManualExpense(expenseAmount)
                }
                if (incomeAmount != null && incomeAmount > 0) {
                    viewModelI.addManualExpense(incomeAmount)
                }
                if (saveAmount != null && saveAmount > 0){
                    viewModelS.addManualSave(saveAmount)
                }
                expense = ""
                income = ""
                save = ""
                onClip()
            },
            border = BorderStroke(3.dp, brush = AppColors.royalGoldGradientL),
            colors = ButtonDefaults.buttonColors(Color(0xD7343434)),
            modifier = Modifier.height(100.dp).width(250.dp),
            shape = RoundedCornerShape(10.dp),
            ){
            Text(
                style = TextStyle(AppColors.royalGoldGradientV),
                textAlign = TextAlign.Center,
                text = "Загрузить данные ",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

