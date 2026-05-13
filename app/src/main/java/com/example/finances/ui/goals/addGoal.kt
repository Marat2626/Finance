package com.example.finances.ui.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finances.GoalViewModel
import com.example.finances.utils.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun addGoal(
    showAddGoalSheet: (Boolean) -> Unit,
    onGoalCreated: (name: String, amount: Double) -> Unit
) {



    val goalName = remember { mutableStateOf("") }
    val goalAmount = remember { mutableStateOf("") }
    val pendingGoalName = remember { mutableStateOf("") }
    val pendingGoalAmount = remember { mutableStateOf<Double?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }


    ModalBottomSheet(
        modifier = Modifier.height(600.dp),
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
            modifier = Modifier.fillMaxSize().background(
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


                OutlinedTextField(
                    value = goalName.value,
                    onValueChange = { goalName.value = it },
                    placeholder = {
                        Text(
                            "Название цели",
                            style = TextStyle(
                                color =  AppColors.current.textAlfa,
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
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp)
                )


                Spacer(modifier = Modifier.height(16.dp))


                OutlinedTextField(
                    value = goalAmount.value,
                    onValueChange = { goalAmount.value = it },
                    placeholder = {
                        Text(
                            "Сумма цели",
                            style = TextStyle(
                                color =  AppColors.current.textAlfa,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(
                        color =  AppColors.current.text,
                        fontSize = 20.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.current.icones,
                        focusedTextColor = AppColors.current.icones,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp)
                )



                Spacer(modifier = Modifier.height(32.dp))


                Button(
                    onClick = {
                        val amount = goalAmount.value.toDoubleOrNull()
                        if (goalName.value.isBlank() || amount == null || amount <= 0) {
                            // snackbar
                        } else {
                            pendingGoalName.value = goalName.value
                            pendingGoalAmount.value = amount
                            showAddGoalSheet(false)
                            onGoalCreated(goalName.value, amount)
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