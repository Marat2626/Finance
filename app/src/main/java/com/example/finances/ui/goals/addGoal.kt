package com.example.finances.ui.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChipDefaults.filterChipColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.finances.R
import com.example.finances.utils.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun addGoal(
    showAddGoalSheet: (Boolean) -> Unit,
    onGoalCreated: (name: String, amount: Double, category: String) -> Unit
) {
    val goalName = remember { mutableStateOf("") }
    val goalAmount = remember { mutableStateOf("") }
    val selectedCategory = remember { mutableStateOf("") }  // ← пустая строка
    val snackbarHostState = remember { SnackbarHostState() }

//    val categories = listOf("Одежда", "Квартира", "Мебель", "Машина", "Телефон", "Отпуск")
//    val categoryIcons = mapOf(
//        "Одежда" to "",
//        "Квартира" to "🏠",
//        "Мебель" to "🛋",
//        "Машина" to "",
//        "Телефон" to "",
//        "Отпуск" to ""
//    )

    ModalBottomSheet(
        modifier = Modifier.height(650.dp),
        onDismissRequest = { showAddGoalSheet(false) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.Transparent,
        dragHandle = {
            Box(
                modifier = Modifier
                    .width(50.dp)
                    .height(5.dp)
                    .background(AppColors.current.screenBG, RoundedCornerShape(3.dp))
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    AppColors.current.screenBG,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Новая цель",
                    style = TextStyle(
                        color = AppColors.current.text,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Название цели
                OutlinedTextField(
                    value = goalName.value,
                    onValueChange = { goalName.value = it },
                    placeholder = {
                        Text(
                            "Название цели",
                            style = TextStyle(
                                color = AppColors.current.textAlfa,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        )
                    },
                    textStyle = TextStyle(
                        color = AppColors.current.text,
                        fontSize = 20.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.current.icones,
                        focusedTextColor = AppColors.current.icones,
                        unfocusedBorderColor = AppColors.current.icones.copy(alpha = 0.3f),
                        unfocusedTextColor = AppColors.current.icones,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Сумма цели
                OutlinedTextField(
                    value = goalAmount.value,
                    onValueChange = { goalAmount.value = it },
                    placeholder = {
                        Text(
                            "Сумма цели",
                            style = TextStyle(
                                color = AppColors.current.textAlfa,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(
                        color = AppColors.current.text,
                        fontSize = 20.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.current.icones,
                        focusedTextColor = AppColors.current.icones,
                        unfocusedBorderColor = AppColors.current.icones.copy(alpha = 0.3f),
                        unfocusedTextColor = AppColors.current.icones,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Выбор категории
                Text(
                    text = "Категория цели",
                    style = TextStyle(
                        color = AppColors.current.textAlfa,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

// Первый ряд
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Без категории
                    FilterChip(
                        selected = selectedCategory.value.isEmpty(),
                        onClick = { selectedCategory.value = "" },
                        label = { Text("Без", fontSize = 14.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.current.icones,
                            selectedLabelColor = Color.White,
                            containerColor = AppColors.current.cardBox,
                            labelColor = AppColors.current.text
                        ),
                        border = BorderStroke(1.dp, AppColors.current.borderLiner)
                    )

                    // Одежда
                    FilterChip(
                        selected = selectedCategory.value == "Одежда",
                        onClick = { selectedCategory.value = "Одежда" },
                        label = { Text("Одежда", fontSize = 14.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.current.icones,
                            selectedLabelColor = Color.White,
                            containerColor = AppColors.current.cardBox,
                            labelColor = AppColors.current.text
                        ),
                        border = BorderStroke(1.dp, AppColors.current.borderLiner)
                    )

                    // Квартира
                    FilterChip(
                        selected = selectedCategory.value == "Квартира",
                        onClick = { selectedCategory.value = "Квартира" },
                        label = { Text("Квартира", fontSize = 14.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.current.icones,
                            selectedLabelColor = Color.White,
                            containerColor = AppColors.current.cardBox,
                            labelColor = AppColors.current.text
                        ),
                        border = BorderStroke(1.dp, AppColors.current.borderLiner)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

// Второй ряд
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Мебель
                    FilterChip(
                        selected = selectedCategory.value == "Мебель",
                        onClick = { selectedCategory.value = "Мебель" },
                        label = { Text(" Мебель", fontSize = 14.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.current.icones,
                            selectedLabelColor = Color.White,
                            containerColor = AppColors.current.cardBox,
                            labelColor = AppColors.current.text
                        ),
                        border = BorderStroke(1.dp, AppColors.current.borderLiner)
                    )

                    // Машина
                    FilterChip(
                        selected = selectedCategory.value == "Машина",
                        onClick = { selectedCategory.value = "Машина" },
                        label = { Text(" Машина", fontSize = 14.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.current.icones,
                            selectedLabelColor = Color.White,
                            containerColor = AppColors.current.cardBox,
                            labelColor = AppColors.current.text
                        ),
                        border = BorderStroke(1.dp, AppColors.current.borderLiner)
                    )

                    // Телефон
                    FilterChip(
                        selected = selectedCategory.value == "Телефон",
                        onClick = { selectedCategory.value = "Телефон" },
                        label = { Text("Телефон", fontSize = 14.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.current.icones,
                            selectedLabelColor = Color.White,
                            containerColor = AppColors.current.cardBox,
                            labelColor = AppColors.current.text
                        ),
                        border = BorderStroke(1.dp, AppColors.current.borderLiner)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

// Третий ряд
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Отпуск
                    FilterChip(
                        selected = selectedCategory.value == "Отпуск",
                        onClick = { selectedCategory.value = "Отпуск" },
                        label = { Text("Отпуск", fontSize = 14.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.current.icones,
                            selectedLabelColor = Color.White,
                            containerColor = AppColors.current.cardBox,
                            labelColor = AppColors.current.text
                        ),
                        border = BorderStroke(1.dp, AppColors.current.borderLiner)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val amount = goalAmount.value.toDoubleOrNull()
                        if (goalName.value.isBlank() || amount == null || amount <= 0) {
                            // можно показать snackbar
                        } else {
                            showAddGoalSheet(false)
                            onGoalCreated(goalName.value, amount, selectedCategory.value)
                        }
                    },
                    modifier = Modifier
                        .height(60.dp)
                        .width(220.dp)
                        .background(
                            AppColors.current.icones,
                            RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(Color.Transparent),
                    border = BorderStroke(2.dp, brush = AppColors.current.borderLiner)
                ) {
                    Text(
                        "Далее →",
                        style = TextStyle(
                            color = AppColors.current.screenBG,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                SnackbarHost(hostState = snackbarHostState)
            }
        }
    }
}