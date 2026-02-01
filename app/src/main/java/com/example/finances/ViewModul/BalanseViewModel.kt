package com.example.finances

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BalanseViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DataTransactions.Companion.getDatabase(application)
    private val _balance = MutableStateFlow<Double?>(null)
    val balance: StateFlow<Double?> = _balance.asStateFlow()

    init {
        loadBalanceFromDb()
    }

    fun loadBalanceFromDb() {
        viewModelScope.launch {
            val balanceEntity = db.balanceDao().getBalance()
            _balance.value = balanceEntity?.amount
            Log.d("Balance", "💰 Загружен баланс из БД: ${balanceEntity?.amount}")
        }
    }
    fun saveBalance(amount: Double?, salary: Double?) {
        if (amount != null) {
            viewModelScope.launch {
                val balance = Balance(
                    id = 1,
                    amount = amount,
                    salary = salary
                )
                db.balanceDao().insertOrUpdate(balance)
                Log.d("Balance", "💾 Сохранен баланс: $amount ₽")
                _balance.value = amount
                val prefs = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("balance_set", true).apply()
            }
        }
    }
}