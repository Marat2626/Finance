package com.example.finances

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val showBottomSheet = remember { mutableStateOf(false) }
       Row (
         // темный фон
        modifier = Modifier
            .height(65.dp).width(380.dp)
            .background(Color(0xFF425281),
            shape = RoundedCornerShape(
            topStart = 15.dp,
            topEnd = 15.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        ))
        .padding(top = 7.dp),
    ) {
           NavigationBarItem(
               selected = selectedItem == 5,
               onClick = {
                   selectedItem = 5
                   onOpenSettings()
               },
               icon = {

                   Icon(
                       painter = painterResource(id = R.drawable.setting_line),
                       modifier = Modifier
                           .size(26.dp),
                       contentDescription = "Настройки",
                       tint = Color.Unspecified

                   )
               },
               label = { Text("Редактор" )},
               colors = NavigationBarItemDefaults.colors(
                   selectedIconColor = Color.Unspecified,
                   unselectedTextColor = Color.Gray
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
                    painter = painterResource(id = R.drawable.addsave),
                    modifier = Modifier
                        .size(26.dp),
                    contentDescription = "Операции",
                    tint = Color.Unspecified

                    )
            },
            label = { Text("Операции") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Unspecified,
                unselectedTextColor = Color.Gray
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
                       painter = painterResource(id = R.drawable.house),
                       modifier = Modifier
                           .size(26.dp),
                       contentDescription = "Домой",
                       tint = Color.Unspecified

                   )
               },
               label = { Text("Домой") },
               colors = NavigationBarItemDefaults.colors(
                   selectedIconColor = Color.Unspecified,
                   unselectedTextColor = Color.Gray
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
                    painter = painterResource(id = R.drawable.addgoal),
                    modifier = Modifier
                        .size(26.dp),
                    contentDescription = "Цели",
                    tint = Color.Unspecified

                    )
            },
            label = { Text("Цели") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Unspecified,
                unselectedTextColor = Color.Gray
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
                       painter = painterResource(id = R.drawable.message_alt_fill),
                       modifier = Modifier
                           .size(26.dp),
                       contentDescription = "ИИ",
                       tint = Color.Unspecified

                   )
               },
               label = { Text("ИИ") },
               colors = NavigationBarItemDefaults.colors(
                   selectedIconColor = Color.Unspecified,
                   unselectedTextColor = Color.Gray
               )
           )
    }
}