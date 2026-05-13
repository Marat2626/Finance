package com.example.finances.utils

import androidx.compose.material3.CheckboxDefaults.colors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color


// Все градиенты для разных тем
data class ThemeGradients(

    // Фоновые градиенты
    val screenBG: Color,
    val borderLiner: Brush,
    val cardBox: Color,
    val cardBoxGreen: Color,
    val cardBoxRed: Color,
    val text: Color,
    val textAlfa: Color,
    val icones: Color,
    val iconsWhite: Color,

    val linerChart: Brush,

    )
    enum class AppTheme {
        DARK, LIGHT, FAVOURITE
    }


    object AppColors {

        private val _currentTheme = mutableStateOf(AppTheme.LIGHT)
        val currentTheme: AppTheme get() = _currentTheme.value

        // Флаг для фирменной темы (вкл/выкл)
        private var _isFavouriteTheme = mutableStateOf(false)
        val isFavouriteTheme: Boolean get() = _isFavouriteTheme.value
        // Переключение между темной и светлой (игнорируя фирменную)
        fun toggleDarkLight() {
            _isFavouriteTheme.value = false  // выключаем фирменную тему
            _currentTheme.value = if (_currentTheme.value == AppTheme.DARK)
                AppTheme.LIGHT
            else
                AppTheme.DARK
        }

        fun toggleFavourite() {
            _isFavouriteTheme.value = !_isFavouriteTheme.value
            if (_isFavouriteTheme.value) {
                _currentTheme.value = AppTheme.FAVOURITE
            } else {
                // Если выключили фирменную, возвращаемся к темной (или сохраняем последнюю)
                _currentTheme.value = AppTheme.DARK
            }
        }

        // Текущие градиенты (зависят от темы)
        val current: ThemeGradients
            @Composable
            get() = when (_currentTheme.value) {
                AppTheme.DARK -> darkGradients
                AppTheme.LIGHT -> lightGradients
                AppTheme.FAVOURITE -> favouriteGradients
            }

        val bullion1 = Color(0xFFD4AF37)
        val bullion2 = Color(0xFFD4AF37)
        val bullion3 = Color(0xFFE7CF8C)
        val bullion4 = Color(0xFFB8860B)
        val bullion5 = Color(0xFFCFB53B)
        val bullion6 = Color(0xFFF4C430)


        val royalGoldGradientV = Brush.Companion.verticalGradient(
            colors = listOf(bullion1, bullion2, bullion3, bullion4, bullion5, bullion6)
        )

        val royalGoldGradientH = Brush.Companion.horizontalGradient(
            colors = listOf(bullion1, bullion2, bullion3, bullion4, bullion5, bullion6)
        )
        val royalGoldGradientL = Brush.Companion.linearGradient(
            colors = listOf(bullion1, bullion2, bullion3, bullion4, bullion5, bullion6)
        )




        val red1 = Color(0xFFEA101F)
        val red2 = Color(0xFFE83232)
        val red3 = Color(0xFFF32918)
        val red4 = Color(0xFFDC1A1A)
        val red5 = Color(0xFF810202)

        val alphaRedGradientL = Brush.Companion.linearGradient(
            colors = listOf(red1, red2, red3, red4, red5)
        )


        val sber1 = Color(0xFF22CCB4)
        val sber2 = Color(0xFF7CEC84)
        val sber3 = Color(0xFF1ACBBD)
        val sber4 = Color(0xFF4CA45C)
        val sber5 = Color(0xFFACD4B4)
        val sber6 = Color(0xFF11877E)


        val sberGreenGradientL = Brush.Companion.linearGradient(
            colors = listOf(sber1, sber2, sber3, sber4, sber5, sber6,)
        )


        val yellow1 = Color(0xFFEECA2C)
        val yellow2 = Color(0xFFC5BB07)
        val yellow3 = Color(0xFFC4A921)
        val yellow4 = Color(0xFFA49615)
        val yellow5 = Color(0xFFD3BA51)


        val yellowGradientL = Brush.Companion.linearGradient(
            colors = listOf(yellow1, yellow2, yellow3, yellow4, yellow5)
        )

        val forest1 = Color(0xFF28A228)
        val forest2 = Color(0xFF24B024)
        val forest3 = Color(0xFF26B426)
        val forest4 = Color(0xFF21CC21)
        val forest5 = Color(0xFF179817)
        val forest6 = Color(0xFF3BEA3B)

        val forestMetalGradientL = Brush.Companion.linearGradient(
            colors = listOf(forest1, forest2, forest3, forest4, forest5, forest6)
        )





        val box = Brush.Companion.verticalGradient(
            colors = listOf(
                Color(0xFF335A81),
                Color(0xFF2D447E),
                Color(0xFF2F4264),
            )
        )










        val screenFavorite =  Color(0xFF365B83)

        val screenLight =  Color(0xFFFCF6EE)
        val screenDark = Color(0xFF0A0C10)



        val bottomBarFavorite = Brush.Companion.verticalGradient(
            colors = listOf(
                Color(0xFF092A52),
                Color(0xFF153A68),
                Color(0xFF163969),
            )
        )
        val bottomBarLight = Brush.Companion.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFFFFFF),
            )
        )

        val bottomBarDark = Brush.Companion.linearGradient(
            colors = listOf(
                Color(0xFF1A1E25),
                Color(0xFF1A1E25),

            )
        )


        val cardBoxLight = Color(0xFFFFFFFF)
        val cardBoxDart = Color(0xFF1A1E25)
        val cardBoxFavorite = Color(0xFF2F4264)

        val textLight = Color(0xDA3D3D3D)
        val textDark = Color(0xFFFFFFFF)

        val textAlfaLight = Color(0x743D3D3D)
        val textAlfaDark = Color(0x80FFFFFF)


        val textFavorite =  Color(0xFFE8EDF5)

        val textAlfaFavorite =   Color(0xB23A6CA0)

        val borderLinerFavorite = Brush.Companion.linearGradient(
            colors = listOf(
                Color(0xFF1A2A4A),
                Color(0xFF2A4A7A),
                Color(0xFF1A2A4A)
            )
        )
        val casesRed = Color(0xFF75152E)
        val casesGreen = Color(0xFF266717)
        val casesYellow = Color(0xFFA28909)
        val casesBlue = Color(0xFF1A2A4A)


        val casesRedLight = Color(0xFFEF8C8C)
        val casesGreenLight = Color(0xFFC8FFBE)
        val casesYellowLight = Color(0xFFFFD30B)
        val casesBlueLight = Color(0xFF0544C9)





        val iconsBlack= Color(0xFF1E1F1F)

        val iconsWhite =   Color(0xFFFCF6EE)

        val iconsBlue = Color(0xFF173F64)


        val linerChartDarkNew = Brush.Companion.linearGradient(
            colors = listOf(
            Color(0xFF0B3860),
            Color(0xFF052A64),
        )
        )
        val linerChart =Brush.Companion.linearGradient(
             colors = listOf(
                Color(0xFF0B3860),
                Color(0xFF052A64),
             )
        )

        val iconesLight = Color(0xFF1C4E81)
        val linerChartText = Color(0xFF262626)


        private val favouriteGradients = ThemeGradients(
            screenBG = screenFavorite,
            borderLiner = royalGoldGradientL,
            cardBox = cardBoxFavorite,
            cardBoxGreen = casesGreen,
            cardBoxRed = casesRed,
            text = textFavorite,
            textAlfa = textAlfaFavorite,
            icones = Color.Unspecified,
            iconsWhite = Color.Unspecified,
            linerChart = linerChart

        )


        private val darkGradients = ThemeGradients(
            screenBG = screenDark,
            borderLiner = borderLinerFavorite,
            cardBox = cardBoxDart,
            cardBoxGreen = casesGreen,
            cardBoxRed = casesRed,
            text = textDark,
            textAlfa = textAlfaDark,
            icones = iconsWhite,
            iconsWhite = iconsWhite,
            linerChart = linerChartDarkNew
        )


        private val lightGradients = ThemeGradients(
            screenBG = screenLight ,
            borderLiner = bottomBarLight,
            cardBox = cardBoxLight,
            cardBoxGreen = casesGreenLight,
            cardBoxRed = casesRedLight,
            text = textLight,
            textAlfa = textAlfaLight,
            icones = iconsBlue,
            iconsWhite = iconsWhite,
            linerChart = linerChart
        )

}