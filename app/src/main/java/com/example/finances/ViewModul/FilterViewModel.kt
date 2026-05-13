package com.example.finances

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class FilterViewModel : ViewModel() {

    // Основные фильтры
    private val _selectedPeriod = MutableStateFlow("Текущий месяц")  // ← изменено! теперь "Текущий месяц" по умолчанию
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _selectedType = MutableStateFlow("Все")
    val selectedType: StateFlow<String> = _selectedType.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Все категории")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _minPrice = MutableStateFlow("")
    val minPrice: StateFlow<String> = _minPrice.asStateFlow()

    private val _maxPrice = MutableStateFlow("")
    val maxPrice: StateFlow<String> = _maxPrice.asStateFlow()

    private val _showFilterPanel = MutableStateFlow(false)
    val showFilterPanel: StateFlow<Boolean> = _showFilterPanel.asStateFlow()

    // Исходные данные
    private val _allTransactions = MutableStateFlow<List<DateExcel>>(emptyList())

    // Отфильтрованные
    private val _filteredTransactions = MutableStateFlow<List<DateExcel>>(emptyList())
    val filteredTransactions: StateFlow<List<DateExcel>> = _filteredTransactions.asStateFlow()

    // Суммы (для дашборда)
    private val _expensesSum = MutableStateFlow(0.0)
    val expensesSum: StateFlow<Double> = _expensesSum.asStateFlow()

    private val _incomesSum = MutableStateFlow(0.0)
    val incomesSum: StateFlow<Double> = _incomesSum.asStateFlow()

    private val _savingsSum = MutableStateFlow(0.0)
    val savingsSum: StateFlow<Double> = _savingsSum.asStateFlow()

    private val _expensesChangePercent = MutableStateFlow(0)
    val expensesChangePercent: StateFlow<Int> = _expensesChangePercent.asStateFlow()

    private val _incomesChangePercent = MutableStateFlow(0)
    val incomesChangePercent: StateFlow<Int> = _incomesChangePercent.asStateFlow()

    private var currentDisplayYearMonth: YearMonth? = null

    // ========== МЕТОДЫ ==========

    fun setTransactions(transactions: List<DateExcel>) {
        _allTransactions.value = transactions
        applyFilters()
        updateDisplayMonthData()  // ← используем новую логику
    }

    fun toggleFilterPanel() {
        _showFilterPanel.value = !_showFilterPanel.value
    }

    fun updatePeriod(period: String) {
        _selectedPeriod.value = period
        applyFilters()
        // Если выбран не "Текущий месяц", обновляем данные для отображаемого периода
        if (period != "Текущий месяц") {
            updateDataForPeriod(period)
        } else {
            updateDisplayMonthData()
        }
    }

    fun updateType(type: String) {
        _selectedType.value = type
        applyFilters()
    }

    fun updateCategory(category: String) {
        _selectedCategory.value = category
        applyFilters()
    }

    fun updateMinPrice(price: String) {
        _minPrice.value = price
        applyFilters()
    }

    fun updateMaxPrice(price: String) {
        _maxPrice.value = price
        applyFilters()
    }

    fun resetFilters() {
        _selectedPeriod.value = "Текущий месяц"  // ← сброс на "Текущий месяц"
        _selectedType.value = "Все"
        _selectedCategory.value = "Все категории"
        _minPrice.value = ""
        _priorityMode.value = false      // ← добавь
        _categoryGroupMode.value = false // ← добавь
        _maxPrice.value = ""
        applyFilters()
        updateDisplayMonthData()
    }

    private fun applyFilters() {
        val transactions = _allTransactions.value
        val period = _selectedPeriod.value
        val type = _selectedType.value
        val category = _selectedCategory.value
        val min = _minPrice.value.toDoubleOrNull()
        val max = _maxPrice.value.toDoubleOrNull()

        var filtered = transactions

        // Фильтр по периоду (для отображения в списке/графиках)
        filtered = when (period) {
            "Сегодня" -> filtered.filter { it.date.toLocalDate() == LocalDate.now() }
            "Неделя" -> filtered.filter { it.date.toLocalDate() >= LocalDate.now().minusWeeks(1) }
            "Месяц" -> filtered.filter {
                val now = LocalDate.now()
                val previousMonth = now.minusMonths(1)

                it.date.toLocalDate().month == previousMonth.month &&
                        it.date.toLocalDate().year == previousMonth.year
            }
            "6 месяцев" -> filtered.filter { it.date.toLocalDate() >= LocalDate.now().minusMonths(6) }
            "Год" -> filtered.filter { it.date.toLocalDate() >= LocalDate.now().minusYears(1) }
            "Текущий месяц" -> {
                val targetMonth = getTargetMonth()
                if (targetMonth != null) {
                    filtered.filter {
                        it.date.year == targetMonth.year &&
                                it.date.monthValue == targetMonth.monthValue
                    }
                } else {
                    emptyList()
                }
            }
            else -> filtered
        }

        // Фильтр по типу
        if (type != "Все") {
            filtered = filtered.filter { it.typeOfOperation == type }
        }

        // Фильтр по категории
        if (category != "Все категории") {
            filtered = filtered.filter { it.category == category }
        }

        // Фильтр по цене
        if (min != null) {
            filtered = filtered.filter { it.amount >= min }
        }
        if (max != null) {
            filtered = filtered.filter { it.amount <= max }
        }

        _filteredTransactions.value = filtered

        // Обновляем суммы (для отфильтрованного списка)
        _expensesSum.value = filtered.filter { it.typeOfOperation == "Расходы" }.sumOf { it.amount }
        _incomesSum.value = filtered.filter { it.typeOfOperation == "Зачисления" }.sumOf { it.amount }
        _savingsSum.value = filtered.filter { it.typeOfOperation == "Накопление" }.sumOf { it.amount }
    }

    /**
     * Определяет, какой месяц показывать:
     * - Если есть транзакции за текущий месяц → показываем текущий месяц
     * - Если нет → показываем последний месяц, в котором есть транзакции
     * - Если транзакций вообще нет → null
     */
    private fun getTargetMonth(): YearMonth? {
        val transactions = _allTransactions.value
        if (transactions.isEmpty()) return null

        val now = LocalDate.now()
        val currentMonth = YearMonth.now()

        // Проверяем, есть ли транзакции в текущем месяце
        val hasCurrentMonthTransactions = transactions.any { tx ->
            tx.date.year == currentMonth.year && tx.date.monthValue == currentMonth.monthValue
        }

        return if (hasCurrentMonthTransactions) {
            currentMonth
        } else {
            // Находим последнюю дату с транзакцией
            val lastTransactionDate = transactions.maxByOrNull { it.date }?.date
            lastTransactionDate?.let { YearMonth.of(it.year, it.monthValue) }
        }
    }

    /**
     * Обновляет суммы и проценты для отображаемого месяца
     */
    private fun updateDisplayMonthData() {
        val transactions = _allTransactions.value
        if (transactions.isEmpty()) {
            _expensesSum.value = 0.0
            _incomesSum.value = 0.0
            _expensesChangePercent.value = 0
            _incomesChangePercent.value = 0
            currentDisplayYearMonth = null
            return
        }

        val targetMonth = getTargetMonth()
        if (targetMonth == null) {
            _expensesSum.value = 0.0
            _incomesSum.value = 0.0
            _expensesChangePercent.value = 0
            _incomesChangePercent.value = 0
            return
        }

        currentDisplayYearMonth = targetMonth

        // Транзакции за целевой месяц
        val monthTransactions = transactions.filter { tx ->
            tx.date.year == targetMonth.year && tx.date.monthValue == targetMonth.monthValue
        }

        val currentExpenses = monthTransactions
            .filter { it.typeOfOperation == "Расходы" }
            .sumOf { it.amount }

        val currentIncomes = monthTransactions
            .filter { it.typeOfOperation == "Зачисления" }
            .sumOf { it.amount }

        // Ищем ПРЕДЫДУЩИЙ месяц с транзакциями (не обязательно календарный предыдущий)
        val previousMonth = findPreviousMonthWithTransactions(targetMonth, transactions)

        val previousExpenses = if (previousMonth != null) {
            transactions
                .filter {
                    it.typeOfOperation == "Расходы" &&
                            it.date.year == previousMonth.year &&
                            it.date.monthValue == previousMonth.monthValue
                }
                .sumOf { it.amount }
        } else 0.0

        val previousIncomes = if (previousMonth != null) {
            transactions
                .filter {
                    it.typeOfOperation == "Зачисления" &&
                            it.date.year == previousMonth.year &&
                            it.date.monthValue == previousMonth.monthValue
                }
                .sumOf { it.amount }
        } else 0.0

        // Считаем проценты
        val expPercent = if (previousExpenses > 0) {
            ((currentExpenses - previousExpenses) / previousExpenses * 100).toInt()
        } else if (currentExpenses > 0 && previousExpenses == 0.0) {
            100
        } else {
            0
        }

        val incPercent = if (previousIncomes > 0) {
            ((currentIncomes - previousIncomes) / previousIncomes * 100).toInt()
        } else if (currentIncomes > 0 && previousIncomes == 0.0) {
            100
        } else {
            0
        }

        _expensesSum.value = currentExpenses
        _incomesSum.value = currentIncomes
        _expensesChangePercent.value = expPercent
        _incomesChangePercent.value = incPercent
    }

    /**
     * Находит предыдущий месяц (относительно targetMonth), в котором есть транзакции
     */
    private fun findPreviousMonthWithTransactions(targetMonth: YearMonth, transactions: List<DateExcel>): YearMonth? {
        // Получаем все месяцы, в которых есть транзакции, сортируем по убыванию
        val monthsWithTransactions = transactions
            .map { YearMonth.of(it.date.year, it.date.monthValue) }
            .distinct()
            .sortedDescending()

        // Находим индекс целевого месяца в списке
        val targetIndex = monthsWithTransactions.indexOf(targetMonth)

        // Возвращаем следующий месяц (предыдущий по времени)
        return if (targetIndex != -1 && targetIndex + 1 < monthsWithTransactions.size) {
            monthsWithTransactions[targetIndex + 1]
        } else {
            null
        }
    }

    /**
     * Обновляет данные для выбранного периода (для фильтров типа "Месяц", "Неделя" и т.д.)
     */
    private fun updateDataForPeriod(period: String) {
        val transactions = _allTransactions.value
        if (transactions.isEmpty()) return

        val now = LocalDate.now()
        val filtered = when (period) {
            "Сегодня" -> transactions.filter { it.date.toLocalDate() == now }
            "Неделя" -> transactions.filter { it.date.toLocalDate() >= now.minusWeeks(1) }
            "Месяц" -> transactions.filter { it.date.toLocalDate() >= now.minusMonths(1) }
            "6 месяцев" -> transactions.filter { it.date.toLocalDate() >= now.minusMonths(6) }
            "Год" -> transactions.filter { it.date.toLocalDate() >= now.minusYears(1) }
            "Все время" -> transactions
            else -> transactions
        }

        _expensesSum.value = filtered.filter { it.typeOfOperation == "Расходы" }.sumOf { it.amount }
        _incomesSum.value = filtered.filter { it.typeOfOperation == "Зачисления" }.sumOf { it.amount }

        // Для периодов, отличных от месяца, процент не показываем или считаем по-другому
        // Здесь можно доработать под свои нужды
        _expensesChangePercent.value = 0
        _incomesChangePercent.value = 0
    }

    private val _priorityMode = MutableStateFlow(false)
    val priorityMode: StateFlow<Boolean> = _priorityMode.asStateFlow()

    private val _categoryGroupMode = MutableStateFlow(false)
    val categoryGroupMode: StateFlow<Boolean> = _categoryGroupMode.asStateFlow()

    // ====================== НОВЫЕ МЕТОДЫ ======================

    fun togglePriorityMode() {
        val newValue = !_priorityMode.value
        _priorityMode.value = newValue
        _categoryGroupMode.value = newValue

        // Автоматически меняем период при включении/выключении
        if (newValue) {
            updatePeriod("Текущий месяц")
        } else {
            updatePeriod("Все время")
        }
    }

    fun setPriorityMode(enabled: Boolean) {
        _priorityMode.value = enabled
        _categoryGroupMode.value = enabled
    }

    // Опционально: сброс при сбросе всех фильтров


}