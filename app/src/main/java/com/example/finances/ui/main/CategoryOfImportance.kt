package com.example.finances

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finances.utils.AppColors

@Composable
fun categoryOfImportance() {
    val categoryVM: CategorySettingsViewModel = viewModel()
    val planVM: PlanViewModel = viewModel()

    val planItems by planVM.planItems.collectAsState()
    val categories by categoryVM.categories.collectAsState()

    LaunchedEffect(Unit) {
        planVM.loadPlanFromDb()  // загружаем план
        if (categories.isEmpty()) {
            categoryVM.loadCategories()
        }
    }

    // Суммируем план и факт по важностям
    val planEssential = planItems.filter { it.importance == ImportanceType.ESSENTIAL }.sumOf { it.planned }
    val planNeutral = planItems.filter { it.importance == ImportanceType.NEUTRAL }.sumOf { it.planned }
    val planNonEssential = planItems.filter { it.importance == ImportanceType.NON_ESSENTIAL }.sumOf { it.planned }

    val spentEssential = planItems.filter { it.importance == ImportanceType.ESSENTIAL }.sumOf{it.spent}
    val spentNeutral = planItems.filter { it.importance == ImportanceType.NEUTRAL }.sumOf { it.spent }
    val spentNonEssential = planItems.filter { it.importance == ImportanceType.NON_ESSENTIAL }.sumOf { it.spent }

    val totalSpent = spentEssential + spentNeutral + spentNonEssential

    Card(
        modifier = Modifier
            .width(150.dp)
            .background(AppColors.current.cardBox, RoundedCornerShape(25.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(5.dp)
        ) {
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    Icon(
                        painter = painterResource(id = R.drawable.plan),
                        modifier = Modifier
                            .size(20.dp),
                        contentDescription = "ПЛАН",
                        tint = AppColors.current.icones

                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        "ПЛАН",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(AppColors.current.text)
                    )
                }
                Text(
                    "${(totalSpent / 1000).toInt()}к ₽",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    style = TextStyle(AppColors.current.textAlfa)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))


            Log.d("PlanVM", "Загружено ${spentEssential}")
            Log.d("PlanVM", "Загружено ${planNeutral}")
            Log.d("PlanVM", "Загружено ${planNonEssential}")



            ProgressItem(R.drawable.necessarily, "Обяз", spentEssential, planEssential, Color(
                0xFF007B2D
            )
            )
            Spacer(modifier = Modifier.height(10.dp))
            ProgressItem(R.drawable.mandatorymoneys, "Нейт", spentNeutral, planNeutral, Color(
                0xFFC0933D
            )
            )
            Spacer(modifier = Modifier.height(10.dp))
            ProgressItem(R.drawable.mandatorymoneys, "Необ", spentNonEssential, planNonEssential, Color(
                0xFF510F18
            )
            )
        }


    }
}
@Composable
private fun ProgressItem(
    emoji: Int,
    label: String,
    spent: Double,
    planned: Double,
    color: Color
) {
    val progress: Float = if (planned > 0) ((spent / planned).toFloat()).coerceIn(0f, 1f) else 0f
    val remaining = if (planned > 0) (planned - spent).coerceAtLeast(0.0) else 0.0

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = emoji),
                    modifier = Modifier
                        .size(15.dp),
                    contentDescription = "ИИ",
                    tint = AppColors.current.icones

                )
                Spacer(Modifier.width(6.dp))
                Text(label, fontSize = 14.sp, style = TextStyle(AppColors.current.text))
            }
            Text(
                "${planned.toInt().formatWithSpace()} ₽",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.Gray.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            if (planned > 0) "осталось ${remaining.toInt().formatWithSpace()} ₽"
            else "план не задан",
            fontSize = 10.sp,
            style = TextStyle(AppColors.current.textAlfa)
        )
    }
}