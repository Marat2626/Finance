package com.example.finances

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finances.utils.AppColors
import kotlin.collections.set






@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseSettingsScreen(
    selectedCategory: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val expenseVM: ExpenseSettingsViewModel = viewModel()


    val categoriesWithExpenses by expenseVM.categoriesWithExpenses.collectAsState()
    val selections = remember { mutableStateMapOf<String, ImportanceType>() }


    LaunchedEffect(selectedCategory) {
        expenseVM.loadExpensesWithSettings()
    }


    val currentExpenses = categoriesWithExpenses
        .find { it.category == selectedCategory }
        ?.expenses ?: emptyList()


    LaunchedEffect(currentExpenses) {
        selections.clear()
        currentExpenses.forEach { expense ->
            expense.importance?.let {
                selections["$selectedCategory|||${expense.description}"] = it
            }
        }
    }


    Column(modifier = Modifier.fillMaxSize().background(AppColors.current.screenBG)) {


        Row (
            modifier = Modifier
                .fillMaxWidth()

                .height(55.dp)
                .padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically

        ){

            Icon(
                painter = painterResource(id = R.drawable.arrow_left),
                modifier = Modifier
                    .size(30.dp)
                    .clickable { onBack() },  // ← кликабельность без отступов
                contentDescription = "Назад",
                tint = AppColors.current.icones
            )

            Spacer(modifier = Modifier.padding(horizontal = 8.dp))

            Text(
                "Категории: $selectedCategory",
                style = TextStyle(AppColors.current.text),
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
            )

        }


        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(currentExpenses) { expense ->

                ExpenseSettingCard(
                    description = expense.description,
                    amount = expense.amount,
                    importance = selections["$selectedCategory|||${expense.description}"],
                    onImportanceChange = { imp ->
                        val key = "$selectedCategory|||${expense.description}"
                        if (selections[key] == imp) selections.remove(key) else selections[key] = imp
                    }
                )
            }
        }


        Button(
            onClick = {
                if (selections.isNotEmpty()) {
                    expenseVM.saveSelectedExpenses(selections.toMap())
                    Toast.makeText(context, "Сохранено", Toast.LENGTH_SHORT).show()
                    onBack()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp, start = 16.dp, bottom = 10.dp)
                .background(AppColors.current.cardBox, shape = RoundedCornerShape(16.dp) )
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),

        ) {
            Text(
                text = "Сохранить настройки трат",
                fontSize = 25.sp,
                style = TextStyle(AppColors.current.text),
                fontWeight = FontWeight.Bold,

            )
        }
    }
}

@Composable
fun ExpenseSettingCard(
    description: String,
    amount: Double,
    importance: ImportanceType?,
    onImportanceChange: (ImportanceType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().background(AppColors.current.cardBox, shape = RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box (
                    modifier = Modifier.width(145.dp),
                ) {
                    Text(
                        text = description,
                        style = TextStyle(AppColors.current.text),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                    )
                }

                Row (
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "${amount.formatWithSpace()} ₽",
                        style = TextStyle(AppColors.current.text),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),

                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ImportanceChip(
                        "Обязательные",
                        importance == ImportanceType.ESSENTIAL,
                        { onImportanceChange(ImportanceType.ESSENTIAL) },
                        AppColors.casesRed,
                    )
                    ImportanceChip(
                        "Нейтральные",
                        importance == ImportanceType.NEUTRAL,
                        { onImportanceChange(ImportanceType.NEUTRAL) },
                        AppColors.casesYellow,

                        )
                    ImportanceChip(
                        "Необязательные",
                        importance == ImportanceType.NON_ESSENTIAL,
                        { onImportanceChange(ImportanceType.NON_ESSENTIAL) },
                        AppColors.casesBlue,
                    )
                }
            }
        }
    }
}
