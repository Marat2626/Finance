package com.example.finances

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color


object AppColors {
    val bullion1 = Color(0xFFD4AF37)
    val bullion2 = Color(0xFFD4AF37)
    val bullion3 = Color(0xFFE7CF8C)
    val bullion4 = Color(0xFFB8860B)
    val bullion5 = Color(0xFFCFB53B)
    val bullion6 = Color(0xFFF4C430)

    val royalGoldGradientV = Brush.verticalGradient(
        colors = listOf(bullion1, bullion2, bullion3, bullion4, bullion5, bullion6)
    )
    val royalGoldGradientH = Brush.horizontalGradient(
        colors = listOf(bullion1, bullion2, bullion3, bullion4, bullion5, bullion6)
    )
    val royalGoldGradientL = Brush.linearGradient(
        colors = listOf(bullion1, bullion2, bullion3, bullion4, bullion5, bullion6)
    )


    val red1 = Color(0xFFEA101F)
    val red2 = Color(0xFFE83232)
    val red3 = Color(0xFFF32918)
    val red4 = Color(0xFFDC1A1A)
    val red5 = Color(0xFF810202)

    val alphaRedGradientV = Brush.verticalGradient(
        colors = listOf(red1, red2, red3, red4, red5)
    )
    val alphaRedGradientH = Brush.horizontalGradient(
        colors = listOf(red1, red2, red3, red4, red5)
    )
    val alphaRedGradientL = Brush.linearGradient(
        colors = listOf(red1, red2, red3, red4, red5)
    )



    val sber1 = Color(0xFF22CCB4)
    val sber2 = Color(0xFF7CEC84)
    val sber3 = Color(0xFF1ACBBD)
    val sber4 = Color(0xFF4CA45C)
    val sber5 = Color(0xFFACD4B4)
    val sber6 = Color(0xFF11877E)

    val sberGreenGradientV = Brush.verticalGradient(
        colors = listOf( sber1,
            sber2,
            sber3,
            sber4,
            sber5,
            sber6,)
    )
    val sberGreenGradientH = Brush.horizontalGradient(
        colors = listOf( sber1,
            sber2,
            sber3,
            sber4,
            sber5,
            sber6,)
    )
    val sberGreenGradientL = Brush.linearGradient(
        colors = listOf( sber1,
            sber2,
            sber3,
            sber4,
            sber5,
            sber6,)
    )




    val yellow1 = Color(0xFFEECA2C)
    val yellow2 = Color(0xFFC5BB07)
    val yellow3 = Color(0xFFC4A921)
    val yellow4 = Color(0xFFA49615)
    val yellow5 = Color(0xFFD3BA51)


    val text1 = Color(0xFFD3BB1E)
    val text2  = Color(0xFFF1DF31)
    val text3  = Color(0xFFC0A118)

    val tbankTextV = Brush.verticalGradient(
        colors = listOf(text1,text2,text3)
    )

    val tbankYellowGradientV = Brush.verticalGradient(
        colors = listOf(yellow1, yellow2, yellow3, yellow4, yellow5)
    )

    val tbankYellowGradientH = Brush.horizontalGradient(
        colors = listOf(yellow1, yellow2, yellow3, yellow4, yellow5)
    )

    val tbankYellowGradientL = Brush.linearGradient(
        colors = listOf(yellow1, yellow2, yellow3, yellow4, yellow5)
    )

    val forest1 = Color(0xFF28A228)
    val forest2 = Color(0xFF24B024)
    val forest3 = Color(0xFF26B426)
    val forest4 = Color(0xFF21CC21)
    val forest5 = Color(0xFF179817)
    val forest6 = Color(0xFF3BEA3B)

    val forestMetalGradientL = Brush.linearGradient(
        colors = listOf(forest1, forest2, forest3, forest4, forest5,forest6)
    )

    val green1 = Color(0xFF058105)
    val green2 = Color(0xFF09D209)
    val red11 = Color(0xFF860202)
    val red22 = Color(0xFFEC0111)
    val yellow11 = Color(0xFFA28800)
    val yellow22 = Color(0xFFE8DD05)

    val YRG = Brush.linearGradient(
        colors = listOf(green1, green2, red11, red22, yellow11, yellow22)
    )




}