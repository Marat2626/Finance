package com.example.finances

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BalanseViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DataTransactions.getDatabase(application)
    private val balanceDao = database.balanceDao()



    val balanceFlow: StateFlow<Double> = balanceDao
        .getBalanceFlow()
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )


    init {
        // Инициализируем баланс если его нет
        viewModelScope.launch {
            val currentBalance = balanceDao.getBalance()
            if (currentBalance == null) {
                balanceDao.saveBalance(Balance(id = 1, currentBalance = 0.0))
                Log.d("BalanseVM", "Баланс инициализирован: 0.0")
            }
        }
    }

    // ✅ Метод для ИЗМЕНЕНИЯ баланса
    fun updateBalance(newBalance: Double) {
        viewModelScope.launch {
            try {
                // Сохраняем новый баланс
                balanceDao.saveBalance(Balance(id = 1, currentBalance = newBalance))
                // balanceFlow АВТОМАТИЧЕСКИ обновится!
                Log.d("BalanseViewModel", "Баланс обновлен: $newBalance")
            } catch (e: Exception) {
                Log.e("BalanseViewModel", "Ошибка обновления баланса", e)
            }
        }
    }

    // ✅ Метод для ДОБАВЛЕНИЯ к балансу
    fun addMultipleToBalance(incomes: Double, expenses: Double) {
        viewModelScope.launch {
            try {
                val currentBalance = balanceDao.getBalance() ?: 0.0
                val newBalance = currentBalance + incomes - expenses
                balanceDao.updateBalance(newBalance)
                Log.d("BALANCE", "Баланс обновлён: $currentBalance + $incomes - $expenses = $newBalance")
            } catch (e: Exception) {
                Log.e("BalanseViewModel", "Ошибка добавления к балансу", e)
            }
        }
    }

    // ✅ Метод для ВЫЧИТАНИЯ из баланса
    fun subtractFromBalance(amount: Double) {
        viewModelScope.launch {
            try {
                val currentBalance = balanceDao.getBalance() ?: 0.0
                val newBalance = currentBalance - amount
                if (newBalance >= 0) {
                    balanceDao.saveBalance(Balance(id = 1, currentBalance = newBalance))
                    // balanceFlow АВТОМАТИЧЕСКИ обновится!
                }
            } catch (e: Exception) {
                Log.e("BalanseViewModel", "Ошибка вычитания из баланса", e)
            }
        }
    }

    // Вспомогательный метод для форматирования
    fun formatBalance(balance: Double): String {
        return String.format("%.2f ₽", balance)
    }
}