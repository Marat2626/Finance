// File: app/src/test/java/com/example/finances/FilterViewModelTest.kt
package com.example.finances

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class FilterViewModelTest {

    private lateinit var viewModel: FilterViewModel

    @Before
    fun setUp() {
        viewModel = FilterViewModel()
    }

    // ================================================================
    // 1. Пустые транзакции → суммы = 0
    // ================================================================
    @Test
    fun setTransactions_emptyList_zeroSums(): Unit = runBlocking {
        viewModel.setTransactions(emptyList())

        assertEquals(0.0, viewModel.expensesSum.first(), 0.01)
        assertEquals(0.0, viewModel.incomesSum.first(), 0.01)
    }

    // ================================================================
    // 2. Транзакции текущего месяца → суммы считаются
    // ================================================================
    @Test
    fun setTransactions_currentMonth_calculatesSums(): Unit = runBlocking {
        val transactions = listOf(
            createTransaction("Расходы", "Еда", 500.0, LocalDateTime.now()),
            createTransaction("Расходы", "Кафе", 300.0, LocalDateTime.now()),
            createTransaction("Зачисления", "Зарплата", 10_000.0, LocalDateTime.now()),
            createTransaction("Накопление", "Вклад", 2_000.0, LocalDateTime.now())
        )

        viewModel.setTransactions(transactions)

        assertEquals(800.0, viewModel.expensesSum.first(), 0.01)
        assertEquals(10_000.0, viewModel.incomesSum.first(), 0.01)
    }

    // ================================================================
    // 5. Фильтр "Все категории" — возвращает все
    // ================================================================
    @Test
    fun filter_allCategories_returnsAll(): Unit = runBlocking {
        val transactions = listOf(
            createTransaction("Расходы", "Еда", 500.0),
            createTransaction("Расходы", "Кафе", 300.0)
        )

        viewModel.setTransactions(transactions)
        viewModel.updateCategory("Все категории")

        val filtered = viewModel.filteredTransactions.first()
        assertEquals(2, filtered.size)
    }

    // ================================================================
    // 6. Фильтр по цене (min/max)
    // ================================================================
    // Замени эти тесты целиком:

    @Test
    fun filter_byType_onlyExpenses(): Unit = runBlocking {
        val transactions = listOf(
            createTransaction("Расходы", "Еда", 500.0),
            createTransaction("Расходы", "Кафе", 300.0),
            createTransaction("Зачисления", "Зарплата", 10_000.0)
        )

        viewModel.setTransactions(transactions)
        viewModel.updatePeriod("Все время")  // ← чтобы не мешал фильтр по дате
        viewModel.updateType("Расходы")

        val filtered = viewModel.filteredTransactions.first()
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.typeOfOperation == "Расходы" })
    }

    @Test
    fun filter_byCategory_onlySelected(): Unit = runBlocking {
        val transactions = listOf(
            createTransaction("Расходы", "Еда", 500.0),
            createTransaction("Расходы", "Кафе", 300.0),
            createTransaction("Расходы", "Еда", 200.0)
        )

        viewModel.setTransactions(transactions)
        viewModel.updatePeriod("Все время")  // ← чтобы не мешал фильтр по дате
        viewModel.updateCategory("Еда")

        val filtered = viewModel.filteredTransactions.first()
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.category == "Еда" })
    }

    @Test
    fun filter_byPrice_inRange(): Unit = runBlocking {
        val transactions = listOf(
            createTransaction("Расходы", "Еда", 100.0),
            createTransaction("Расходы", "Кафе", 500.0),
            createTransaction("Расходы", "Ресторан", 2000.0)
        )

        viewModel.setTransactions(transactions)
        viewModel.updatePeriod("Все время")  // ← чтобы не мешал фильтр по дате
        viewModel.updateMinPrice("200")
        viewModel.updateMaxPrice("1000")

        val filtered = viewModel.filteredTransactions.first()
        assertEquals("Должна остаться 1 транзакция в диапазоне 200-1000", 1, filtered.size)
        assertEquals(500.0, filtered[0].amount, 0.01)
    }

    @Test
    fun resetFilters_returnsAll(): Unit = runBlocking {
        val transactions = listOf(
            createTransaction("Расходы", "Еда", 500.0),
            createTransaction("Зачисления", "Зарплата", 10_000.0)
        )

        viewModel.setTransactions(transactions)
        viewModel.updateType("Расходы")
        assertEquals(1, viewModel.filteredTransactions.first().size)

        viewModel.resetFilters()
        assertEquals(2, viewModel.filteredTransactions.first().size)
        assertEquals("Все", viewModel.selectedType.first())
        assertEquals("Все категории", viewModel.selectedCategory.first())
    }

    @Test
    fun togglePriorityMode_changesState(): Unit = runBlocking {
        assertFalse(viewModel.priorityMode.first())

        viewModel.togglePriorityMode()
        assertTrue(viewModel.priorityMode.first())
        assertTrue(viewModel.categoryGroupMode.first())

        viewModel.togglePriorityMode()
        assertFalse(viewModel.priorityMode.first())
    }

    // ================================================================
    // 9. Период "Сегодня"
    // ================================================================
    @Test
    fun filter_today_returnsOnlyToday(): Unit = runBlocking {
        val today = LocalDateTime.now()
        val yesterday = LocalDateTime.now().minusDays(1)

        val transactions = listOf(
            createTransaction("Расходы", "Еда", 500.0, today),
            createTransaction("Расходы", "Кафе", 300.0, yesterday)
        )

        viewModel.setTransactions(transactions)
        viewModel.updatePeriod("Сегодня")

        val filtered = viewModel.filteredTransactions.first()
        assertEquals(1, filtered.size)
    }

    // ================================================================
    // 10. Переключение панели фильтров
    // ================================================================
    @Test
    fun toggleFilterPanel_changesVisibility(): Unit = runBlocking {
        assertFalse(viewModel.showFilterPanel.first())

        viewModel.toggleFilterPanel()
        assertTrue(viewModel.showFilterPanel.first())

        viewModel.toggleFilterPanel()
        assertFalse(viewModel.showFilterPanel.first())
    }

    // ================================================================
    // 11. Фильтр "Все время" — все транзакции
    // ================================================================
    @Test
    fun filter_allTime_returnsAllTransactions(): Unit = runBlocking {
        val transactions = listOf(
            createTransaction("Расходы", "Еда", 500.0, LocalDateTime.now().minusMonths(3)),
            createTransaction("Расходы", "Кафе", 300.0, LocalDateTime.now().minusYears(1))
        )

        viewModel.setTransactions(transactions)
        viewModel.updatePeriod("Все время")

        val filtered = viewModel.filteredTransactions.first()
        assertEquals(2, filtered.size)
    }

    // ================================================================
    // Вспомогательный метод
    // ================================================================
    private fun createTransaction(
        type: String,
        category: String,
        amount: Double,
        date: LocalDateTime = LocalDateTime.now()
    ): DateExcel {
        return DateExcel(
            date = date,
            typeOfOperation = type,
            category = category,
            amount = amount,
            currency = "₽",
            description = "",
            status = "Выполнено",
            debitCardNumber = ""
        )
    }
}