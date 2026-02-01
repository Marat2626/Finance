package com.example.finances

import androidx.benchmark.traceprocessor.Row
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun settingBalance(onClik:() -> Unit){
    val viewModel: BalanseViewModel = viewModel(key = "expenses_vm_key")
    var balanceText by remember { mutableStateOf("") }
    var salaryText by remember { mutableStateOf("") }

    Column (
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color(0xFF5D5C5D)),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Spacer(modifier = Modifier.padding(80.dp))


        Row {

            OutlinedTextField(
                value = balanceText,
                onValueChange = { balanceText = it },
                placeholder = {
                    Text(
                        text = "Введите ваш баланс",
                        style = TextStyle(AppColors.royalGoldGradientV),
                        fontSize = 19.sp
                    )
                },
                textStyle = TextStyle(
                    brush = AppColors.royalGoldGradientV, // Тот же градиент
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .width(140.dp)
                    .height(75.dp)
                    .border(
                        width = 3.dp,
                        shape = RoundedCornerShape(10.dp),
                        brush = AppColors.royalGoldGradientL
                    )
                    .background(Color(0x703B3B3B))
            )

            Spacer(modifier = Modifier.width(20.dp))

            OutlinedTextField(
                value = salaryText,
                onValueChange = { salaryText = it },
                placeholder = {
                    Text(
                        text = "Введите вашу ЗП",
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
                    .width(140.dp)
                    .height(75.dp)
                    .border(
                        width = 3.dp,
                        shape = RoundedCornerShape(10.dp),
                        brush = AppColors.royalGoldGradientL
                    )
                    .background(Color(0x703B3B3B))
            )
        }

        Spacer(modifier = Modifier.padding(120.dp))


        Button(
            onClick = {
                val balanceValue = balanceText.toDoubleOrNull()
                val salaryValue = salaryText.toDoubleOrNull()
                viewModel.saveBalance(balanceValue, salaryValue)
                onClik()
            },
            border = BorderStroke(3.dp, brush = AppColors.royalGoldGradientL),
            colors = ButtonDefaults.buttonColors(Color(0xD73B3B3B)),
            modifier = Modifier
                .height(80.dp)
                .width(200.dp),
            shape = RoundedCornerShape(10.dp),

        ){
            Text(
                style = TextStyle(AppColors.royalGoldGradientV),
                textAlign = TextAlign.Center,
                text = "Ввести данные",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}