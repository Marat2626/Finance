package com.example.finances

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.poi.sl.usermodel.TextParagraph

@Composable
fun StartScreen(onClipMainScreen:() -> Unit,
                onClipMyDate: () -> Unit,
                onClipSber: () -> Unit,
                onClipSettingBalance: () -> Unit
) {


    val viewModelE: ExpensesViewModel = viewModel()
    val viewModelI: IncomesViewModel = viewModel()
    val scope = rememberCoroutineScope()

    Box(
        Modifier
            .background(Color(0xFF5D5C5D))
            .fillMaxSize(),
    ) {
        Button(
            onClick = { onClipSettingBalance() },
            border = BorderStroke(1.dp, brush = AppColors.royalGoldGradientV ),
            colors = ButtonDefaults.buttonColors(Color(0xD7343434)),
            modifier = Modifier
                .height(50.dp)
                .width(105.dp)
                .padding(top = 10.dp, start = 16.dp)
                .align(Alignment.TopStart),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(0.dp)

        ) {
            Text(
                text = "Обновить баланс",
                style = TextStyle(
                    brush = AppColors.royalGoldGradientV,
                    fontSize = 14.sp
                ),
                textAlign = TextAlign.Center
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .background(Color(0xFF5D5C5D))
                .align(Alignment.BottomStart),
            verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
        ){

            Spacer(modifier = Modifier.height(70.dp))
            download(
                R.drawable.cash,
                text = "Загрузить свои данные",
                colorText = AppColors.royalGoldGradientV,
                colorboard =  AppColors.royalGoldGradientL,
                onClick1 = { onClipMyDate() }
            )
            Spacer(modifier = Modifier.height(30.dp))

            download(
                R.drawable.sber,
                text = "Загрузить из Сбера",
                colorText = AppColors.sberGreenGradientV,
                colorboard = AppColors.sberGreenGradientL,
                onClick1 = {
                        scope.launch {
                            viewModelI.addTransactionsFromExcel("Income.xlsx")
                            viewModelE.addTransactionsFromExcel("Expenses.xlsx")
                            delay(500)
                            onClipSber()
                        }

                }
            )
            Spacer(modifier = Modifier.height(30.dp))

            download(
                R.drawable.alfa,
                text = "Загрузить из Альфы",
                colorText = AppColors.alphaRedGradientV,
                colorboard = AppColors.alphaRedGradientL,
                onClick1 = { onClipMainScreen() }
            )
            Spacer(modifier = Modifier.height(30.dp))

            download(
                R.drawable.tbank,
                text = "Загрузить из Т-Банка",
                colorText = AppColors.tbankTextV,
                colorboard = AppColors.tbankYellowGradientL,
                onClick1 = { onClipMainScreen() }
            )
            Spacer(modifier = Modifier.padding(25.dp))

            Button(
                onClick = { onClipMainScreen() },
                border = BorderStroke(2.dp, brush = AppColors.royalGoldGradientH),
                colors = ButtonDefaults.buttonColors(Color(0xD7343434)),
                modifier = Modifier
                    .height(80.dp)
                    .width(220.dp),
                shape = RoundedCornerShape(10.dp)
            ){
                Text(
                    style = TextStyle( AppColors.royalGoldGradientV),
                    textAlign = TextAlign.Center,
                    text = "Продолжить без загрузки",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun download(
    img: Int,
    text: String,
    colorText: Brush,
    colorboard: Brush,
    onClick1: () -> Unit
){
    Row(
        Modifier
            .fillMaxWidth(0.83f)
            .height(55.dp)
            .background(Color(0xD7343434), shape = RoundedCornerShape(15.dp))
            .border(
                width = 1.dp,
                brush = colorboard,
                shape = RoundedCornerShape(13.dp)
            )
            .clickable { onClick1() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(8.dp))
        Image(
            painter = painterResource(id = img),
            contentDescription = "Сбербанк",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(65.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(5.dp))
        )
        Spacer(modifier = Modifier.width(13.dp))
        Text(
            text = text,
            style = TextStyle(
                brush = colorText,
                fontSize = 19.sp
            )
        )
    }
}
