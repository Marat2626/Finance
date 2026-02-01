package com.example.finances

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch


@Composable
fun addGoals() {

    val viewModelG: GoalViewModel = viewModel()

    val goals by viewModelG.transactions.collectAsState()

    var goalAmount by remember { mutableStateOf("") }
    var goalDescription by remember { mutableStateOf("") }
    Column (
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color(0xFF5D5C5D)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ){

        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(330.dp)
                .height(300.dp)
                .background(
                    Color(0xD7525252),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(vertical = 20.dp),

            ) {
            items(goals) { item ->
                Row(
                    modifier = Modifier
                        .width(300.dp)
                        .padding(vertical = 8.dp, horizontal = 10.dp) // отступ между рамками
                        .border(
                            shape = RoundedCornerShape(10.dp),
                            brush = AppColors.tbankYellowGradientL,
                            width = 0.4.dp,
                        )
                        .shadow(
                            elevation = 13.dp,
                            shape = RoundedCornerShape(5.dp),
                            clip = false,
                            spotColor = Color.Black.copy(alpha = 1f),
                            ambientColor = Color.Black.copy(alpha = 1f)
                        )
                        .background(
                            Color(0xD72F2E2E),
                            shape = RoundedCornerShape(15.dp)
                        )
                        .padding(vertical = 5.dp, horizontal = 2.dp), // отступ между текстом и рамкой
                    horizontalArrangement = Arrangement.SpaceBetween, // Распределяет пространство между элементами
                    verticalAlignment = Alignment.CenterVertically // Выравнивание по вертикали
                ) {
                    // Название цели - слева
                    Text(
                        text = item.name,
                        fontSize = 25.sp,
                        style = TextStyle(AppColors.royalGoldGradientV),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f) // Занимает доступное пространство
                    )

                    // Сумма - справа
                    Text(
                        text = "${item.amount} ₽",
                        fontSize = 23.sp,
                        style = TextStyle(AppColors.royalGoldGradientV),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.padding(top = 20.dp))


        OutlinedTextField(
            value = goalDescription,
            onValueChange = { goalDescription = it },
            placeholder = {
                Text(
                    text = "Введите вашу цель",
                    style = TextStyle(AppColors.royalGoldGradientV),
                    fontSize = 19.sp
                )
            },
            textStyle = TextStyle(
                brush = AppColors.royalGoldGradientV, // Тот же градиент
                fontSize = 19.sp
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
                .background(Color(0x703B3B3B))
        )
        Spacer(Modifier.padding(top = 15.dp))

        OutlinedTextField(
            value = goalAmount,
            onValueChange = { goalAmount = it },
            placeholder = {
                Text(
                    text = "Введите сумму цели",
                    style = TextStyle(AppColors.royalGoldGradientV),
                    fontSize = 19.sp,
                )
            },
            textStyle = TextStyle(
                brush = AppColors.royalGoldGradientV, // Тот же градиент
                fontSize = 19.sp
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
            shape = RoundedCornerShape(10.dp), // Форма самого TextField


        )

        Spacer(Modifier.padding(top = 20.dp))

        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        SnackbarHost(hostState = snackbarHostState)
        Button(

            onClick = {
                val goalAmountNum = goalAmount.toDoubleOrNull()

                if (goalAmount.isEmpty() || goalDescription.isEmpty()){
                    scope.launch {
                        snackbarHostState.showSnackbar("Заполните все поля!")
                    }
                }
                else {
                    viewModelG.saveGoal(goalDescription, goalAmountNum)
                    goalAmount = ""
                    goalDescription = ""
                }

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

        Spacer(Modifier.padding(top = 40.dp))
    }
}