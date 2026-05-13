package com.example.finances

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.finances.utils.AppColors

@Composable
fun bottomNavigationBar(
    onHomeClick: () -> Unit,
    onClipAddOperation: () -> Unit,
    onClipAddGoal: () -> Unit,
    onOpenSettings: () -> Unit,
    onAI: () -> Unit
) {
    // Выбранный пункт меню
    var selectedItem by remember { mutableStateOf(0) }

        Row(
            // темный фон
            modifier = Modifier

                .height(60.dp).width(350.dp)
                .drawBehind {
                    val blurRadius = 20.dp.toPx()
                    val shadowColor = android.graphics.Color.argb(25, 44, 40, 36)
                    val offsetX = 4.dp.toPx()
                    val offsetY = 4.dp.toPx()

                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            this.color = shadowColor
                            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                            isAntiAlias = true
                        }
                        val cornerRadiusPx = 30.dp.toPx()
                        // Рисуем прямоугольник с закруглениями
                        canvas.nativeCanvas.drawRoundRect(
                            offsetX,
                            offsetY,
                            size.width - offsetX,
                            size.height + offsetY, // чуть расширим из-за размытия
                            cornerRadiusPx,
                            cornerRadiusPx,
                            paint
                        )
                    }
                }

                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                .background(
                  AppColors.current.cardBox,
                    shape = RoundedCornerShape(
                        topStart = 30.dp,
                        topEnd = 30.dp,
                        bottomStart = 30.dp,
                        bottomEnd = 30.dp
                    )
                )

        ) {
            NavigationBarItem(
                selected = selectedItem == 5,
                onClick = {
                    selectedItem = 5
                    onOpenSettings()
                },
                icon = {

                    Icon(
                        painter = painterResource(id = R.drawable.setting),
                        modifier = Modifier
                            .size(30.dp),
                        contentDescription = "Настройки",
                        tint = AppColors.current.icones

                    )
                },
//                label = { Text("Редактор") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Unspecified,
                    unselectedTextColor = AppColors.current.icones
                )
            )




            NavigationBarItem(
                selected = selectedItem == 3,
                onClick = {
                    selectedItem = 3
                    onClipAddOperation()
                },
                icon = {

                    Icon(
                        painter = painterResource(id = R.drawable.transaction),
                        modifier = Modifier
                            .size(30.dp),
                        contentDescription = "Операции",
                        tint = AppColors.current.icones

                    )
                },
//                label = { Text("Операции") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Unspecified,
                    unselectedTextColor = AppColors.current.icones
                )
            )
            NavigationBarItem(
                selected = selectedItem == 2,
                onClick = {
                    selectedItem = 2
                    onHomeClick()
                },
                icon = {

                    Icon(
                        painter = painterResource(id = R.drawable.houme),
                        modifier = Modifier
                            .size(30.dp),
                        contentDescription = "Домой",
                        tint = AppColors.current.icones

                    )
                },
//                label = { Text("Домой") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Unspecified,
                    unselectedTextColor = AppColors.current.icones
                )
            )

            NavigationBarItem(
                selected = selectedItem == 4,
                onClick = {
                    selectedItem = 4
                    onClipAddGoal()
                },
                icon = {

                    Icon(
                        painter = painterResource(id = R.drawable.goals),
                        modifier = Modifier
                            .size(30.dp),
                        contentDescription = "Цели",
                        tint = AppColors.current.icones

                    )
                },
//                label = { Text("Цели") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Unspecified,
                    unselectedTextColor = AppColors.current.icones
                )
            )

            NavigationBarItem(
                selected = selectedItem == 6,
                onClick = {
                    selectedItem = 6
                    onAI()
                },
                icon = {

                    Icon(
                        painter = painterResource(id = R.drawable.ai),
                        modifier = Modifier
                            .size(30.dp),
                        contentDescription = "ИИ",
                        tint = AppColors.current.icones

                    )
                },
//                label = { Text("ИИ") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Unspecified,
                    unselectedTextColor = AppColors.current.icones
                )
            )
        }
}