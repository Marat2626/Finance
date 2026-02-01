package com.example.finances

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun incomesScren() {

    val viewModelI: IncomesViewModel = viewModel()
    val viewModelB: BalanseViewModel = viewModel()

    val balance by viewModelB.balance.collectAsState()
    val transactions by viewModelI.transactions.collectAsState()
    Column (
        Modifier
            .fillMaxSize()
            .background(Color(0xD7343434))

    ){
        Spacer(modifier = Modifier.padding(top = 50.dp))
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
                .padding(vertical = 5.dp)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        )
        {
            Text(
                style = TextStyle(AppColors.royalGoldGradientL),
                textAlign = TextAlign.Center,
                text ="Баланс \n${String.format("%.2f", balance)} ₽",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            Modifier
                .height(285.dp)
                .width(330.dp)
                .align(Alignment.CenterHorizontally)

        ) {
            Text(
                text = "История доходов",
                style = TextStyle(
                    brush =  AppColors.tbankYellowGradientL,
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                    drawStyle = Stroke(width = 2f)
                )
            )

            Text(
                text = "История доходов",
                style = TextStyle(AppColors.royalGoldGradientL),
                fontWeight = FontWeight.Bold,
                fontSize = 23.sp
            )
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .border(
                        shape = RoundedCornerShape(15.dp),
                        brush = AppColors.tbankYellowGradientL,
                        width = 3.dp
                    )
                    .align(Alignment.BottomCenter)
                    .background(
                        Color(0xD7525252),
                        shape = RoundedCornerShape(15.dp)
                    )
                    .padding(vertical = 20.dp),

            ) {
                items(transactions) { item ->
                    Text(
                        text = "${item.amount} ₽\n${item.category}",

                        modifier = Modifier
                            .width(310.dp)
                            .padding(vertical = 8.dp, horizontal = 10.dp)// отступ между рамками
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
                            .background( Color(0xD72F2E2E),
                                shape = RoundedCornerShape(15.dp))
                            .padding(vertical = 5.dp, horizontal = 2.dp),// отступ между текстом и рамкой
                        fontSize = 18.sp,
                        style = TextStyle(AppColors.royalGoldGradientV)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}