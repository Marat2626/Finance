package com.example.finances

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.nio.file.WatchEvent

@Composable
fun mainScreen(onClipExpenses:() -> Unit, onClipIncomes:() -> Unit, onClipSave:() -> Unit, onClipAddGoal: () -> Unit) {

    val viewModelE: ExpensesViewModel = viewModel()
    val viewModelI: IncomesViewModel = viewModel()
    val viewModelB: BalanseViewModel = viewModel()
    val viewModelS: SaveViewModel = viewModel()


    val totalAmountE by viewModelE.totalAmount.collectAsState()
    val totalAmountI by viewModelI.totalAmount.collectAsState()
    val totalAmountS by viewModelS.totalAmount.collectAsState()
    val balance by viewModelB.balance.collectAsState()

//    val expenses by viewModelE.transactions.collectAsState()
//    val incomes by viewModelI.transactions.collectAsState()
//
//
//    LaunchedEffect(expenses, incomes) {
//        Log.d("MAIN_SCREEN", "Кол-во расходов: ${expenses.size}, сумма: $totalAmountE")
//        Log.d("MAIN_SCREEN", "Кол-во доходов: ${incomes.size}, сумма: $totalAmountI")
//    }

  Column(
      modifier = Modifier
          .fillMaxSize()
          .background(Color(0xD7343434)),
      horizontalAlignment = Alignment.CenterHorizontally,

      )
  {

      Spacer(modifier = Modifier.padding( bottom = 15.dp))
          Button(
              onClick = {
                  onClipAddGoal()
              },
              border = BorderStroke(1.dp, brush = AppColors.royalGoldGradientL),
              colors = ButtonDefaults.buttonColors(Color(0xD7484747)),
              modifier = Modifier
                  .height(40.dp)
                  .width(70.dp)
                  .align(Alignment.End),
              contentPadding = PaddingValues(0.dp), // внутренний отступ кнопки


              shape = RoundedCornerShape(10.dp),

              ) {
              Text(
                  text = "Добавить цель",
                  lineHeight = 12.sp, // растояние от текста  по высоте
                  style = TextStyle(AppColors.royalGoldGradientL),
                  fontSize = 12.sp,
                  textAlign = TextAlign.Center,
                  modifier = Modifier
                      .padding(horizontal = 2.dp) // растояние от текста до рамки

              )
          }

      Spacer(modifier = Modifier.padding( bottom = 50.dp))
      Box(
            modifier = Modifier
                .height(50.dp)
                .width(120.dp)
                .background(
                    Color(0xD74B4949),
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = 2.dp,
                    brush = AppColors.royalGoldGradientV,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(vertical = 5.dp),
            contentAlignment = Alignment.Center
        )
        {
            Text(
                style = TextStyle(AppColors.royalGoldGradientL),
                textAlign = TextAlign.Center,
                text ="Баланс \n${String.format("%.2f", balance)} ₽",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

      Spacer(modifier = Modifier.padding( bottom = 200.dp))
        Column(
            modifier = Modifier
                .height(250.dp)
                .width(340.dp)
                .background(
                    Color(0xFF3A3838),
                    shape = RoundedCornerShape(15.dp)
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            look("Доходы:", totalAmountI, AppColors.tbankYellowGradientL, onClipIncomes)
            look("Расходы:", totalAmountE, AppColors.alphaRedGradientL,onClipExpenses)
            look("Накопление:", totalAmountS, AppColors.forestMetalGradientL,onClipSave)
        }

    }
}
@Composable
fun look(name: String, sum: Double, colorBorder: Brush, onClip:() -> Unit){
    Row(modifier = Modifier
        .clickable { onClip() }
        .width(300.dp)
        .height(60.dp)
        .background(Color(0xD75D5C5C), shape = RoundedCornerShape(10.dp))
        .border(
            width = 2.dp,
            brush = colorBorder,
            shape = RoundedCornerShape(10.dp)
        ),

        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
        ){
        Text(
            text = "$name ${String.format("%.2f", sum)} ₽",
            fontSize = 25.sp,
            style = TextStyle(AppColors.royalGoldGradientV)
        )
    }
}


