package com.example.finances

object PythonLogic {
    /**
     * Точная копия твоей Python функции
     * income: число (доход)
     * Возвращает: "ХОРОШО" или "ПЛОХО"
     */
    fun checkIncome(income: Double): String {
        if (income > 100000) {
            return "ХОРОШО"
        } else {
            return "ПЛОХО"
        }
    }
    
    /**
     * Более продвинутая версия (если захочешь)
     */
    fun analyzeBudget(income: Double, expenses: Double): String {
        val savings = income - expenses
        val savingsPercent = (savings / income) * 100
        
        return when {
            expenses > income -> "❌ Тратите больше чем зарабатываете"
            savingsPercent > 30 -> "✅ Отлично! Экономите ${savingsPercent.toInt()}%"
            savingsPercent > 10 -> "👍 Хорошо, продолжайте"
            else -> "⚠️ Можно экономить лучше"
        }
    }
}