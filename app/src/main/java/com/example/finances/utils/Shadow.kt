package com.example.finances.utils

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp

fun Modifier.cardShadow(
    blurRadiusDp: Float = 20f,
    shadowColor: Int = android.graphics.Color.argb(28, 44, 40, 36),
    offsetXDp: Float = 4f,
    offsetYDp: Float = 4f,
    cornerRadiusDp: Float = 30f
): Modifier = this.drawBehind {
    val blurRadius = blurRadiusDp.dp.toPx()
    val offsetX = offsetXDp.dp.toPx()
    val offsetY = offsetYDp.dp.toPx()
    val cornerRadius = cornerRadiusDp.dp.toPx()

    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            this.color = shadowColor
            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            isAntiAlias = true
        }
        canvas.nativeCanvas.drawRoundRect(
            offsetX,
            offsetY,
            size.width - offsetX,
            size.height + offsetY,
            cornerRadius,
            cornerRadius,
            paint
        )
    }
}