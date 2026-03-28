package com.example.finances


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp






@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalStrategySelector(
    goalAmount: Double,
    onDismiss: () -> Unit,
    onStrategySelected: (StrategyRecommendation) -> Unit
) {
    val calculator = GoalSavingsCalculator(DataTransactions.getDatabase(LocalContext.current))
    var recs by remember { mutableStateOf<List<StrategyRecommendation>>(emptyList()) }


    LaunchedEffect(goalAmount) {
        recs = calculator.calculateStrategies(goalAmount)
    }


    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF1E2F4A)) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text("Как накопить на цель?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Сумма: ${String.format("%,.0f", goalAmount)} ₽", color = Color(0xFFD4AF37))


            Spacer(modifier = Modifier.height(24.dp))


            recs.forEach { rec ->
                StrategyCard(rec, onClick = { onStrategySelected(rec) })
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}







@Composable
fun StrategyCard(
    recommendation: StrategyRecommendation,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A3A6A)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(recommendation.emoji, fontSize = 40.sp)


            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(recommendation.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(recommendation.description, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            }


            Column(horizontalAlignment = Alignment.End) {
                Text("${recommendation.months} мес", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                Text("${String.format("%,.0f", recommendation.monthlyPayment)} ₽/мес", fontSize = 15.sp, color = Color.White)
            }
        }
    }
}


