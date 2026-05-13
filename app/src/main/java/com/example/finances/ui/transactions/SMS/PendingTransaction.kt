package com.example.finances

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime

// РЕПОЗИТОРИЙ для хранения неподтверждённых транзакций в памяти
class PendingRepository {

    private val _pendingTransactions = MutableStateFlow<List<PendingTransaction>>(emptyList())
    val pendingTransactions: StateFlow<List<PendingTransaction>> = _pendingTransactions.asStateFlow()

    fun add(transaction: PendingTransaction) {
        _pendingTransactions.value = _pendingTransactions.value + transaction
    }

    fun remove(transaction: PendingTransaction) {
        _pendingTransactions.value = _pendingTransactions.value - transaction
    }

    fun clearAll() {
        _pendingTransactions.value = emptyList()
    }
}

// ДАННЫЕ одной неподтверждённой транзакции
data class PendingTransaction(
    val id: Long = System.currentTimeMillis(),
    val amount: Double,
    val type: String,        // "Расходы" или "Зачисления"
    val description: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)