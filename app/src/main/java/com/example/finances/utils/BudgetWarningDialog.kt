package com.example.finances

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finances.utils.AppColors
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun showBudgetWarningDialog(
    averageIncome: Double,
    totalPlan: Double,
    overAmount: Double,
    onIgnore: () -> Unit,
    onGoToCategories: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .background(AppColors.current.cardBox, shape = RoundedCornerShape(20.dp)),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚠️", fontSize = 24.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Внимание",
                    style = TextStyle(
                        color = AppColors.current.text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        text = {
            Column {
                Text(
                    "Ваш план расходов превышает средний доход:",
                    style = TextStyle(color =  AppColors.current.text, fontSize = 14.sp)
                )
                Spacer(Modifier.height(12.dp))

                // Доход
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("💰 Средний доход:", style = TextStyle(color = AppColors.current.textAlfa, fontSize = 13.sp))
                    Text(
                        "${averageIncome.toInt().formatWithSpace()} ₽",
                        style = TextStyle(color = AppColors.current.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    )
                }

                // План
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("📋 Общий план:", style = TextStyle(color =  AppColors.current.textAlfa, fontSize = 13.sp))
                    Text(
                        "${totalPlan.toInt().formatWithSpace()} ₽",
                        style = TextStyle(color = AppColors.current.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    )
                }

                // Превышение
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("❌ Превышение:", style = TextStyle(color = AppColors.current.textAlfa, fontSize = 13.sp))
                    Text(
                        "${overAmount.toInt().formatWithSpace()} ₽",
                        style = TextStyle(
                            color = Color(0xFFFF6B6B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "Рекомендуем снизить необязательные или нейтральные расходы.",
                    style = TextStyle(color =  AppColors.current.text, fontSize = 13.sp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onGoToCategories,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Перейти к категориям", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onIgnore) {
                Text("Игнорировать", style = TextStyle(color =  AppColors.current.textAlfa))
            }
        },
        containerColor = Color.Transparent,
        shape = RoundedCornerShape(20.dp)
    )
}