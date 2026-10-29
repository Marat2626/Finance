// File: app/src/androidTest/java/com/example/finances/EISviewModelTest.kt
package com.example.finances

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class EISviewModelTest {

    private lateinit var db: DataTransactions

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = DataTransactions.getTestDatabase(context)
    }

    @After
    fun tearDown() {
        db.close()
        DataTransactions.clearTestDatabase()
    }

    // ================================================================
    // 1. Добавление расхода
    // ================================================================
    @Test
    fun addExpense_savesCorrectly(): Unit = runBlocking {
        val dao = db.dataExcelDao()

        val expense = DateExcel(
            date = LocalDateTime.now(),
            typeOfOperation = "Расходы",
            category = "Кафе",
            amount = 500.0,
            currency = "₽",
            description = "Обед",
            status = "Выполнено",
            debitCardNumber = ""
        )

        dao.insert(expense)
        val allExpenses = dao.getAllExpenses().first()

        assertEquals(1, allExpenses.size)
        assertEquals("Кафе", allExpenses[0].category)
        assertEquals(500.0, allExpenses[0].amount, 0.01)
    }

    // ================================================================
    // 2. Добавление дохода
    // ================================================================
    @Test
    fun addIncome_savesCorrectly(): Unit = runBlocking {
        val dao = db.dataExcelDao()

        val income = DateExcel(
            date = LocalDateTime.now(),
            typeOfOperation = "Зачисления",
            category = "Зарплата",
            amount = 100_000.0,
            currency = "₽",
            description = "Аванс",
            status = "Выполнено",
            debitCardNumber = ""
        )

        dao.insert(income)
        val allIncome = dao.getAllIncome().first()

        assertEquals(1, allIncome.size)
        assertEquals(100_000.0, allIncome[0].amount, 0.01)
    }

    // ================================================================
    // 3. Добавление накопления
    // ================================================================
    @Test
    fun addSaving_savesCorrectly(): Unit = runBlocking {
        val dao = db.dataExcelDao()

        val saving = DateExcel(
            date = LocalDateTime.now(),
            typeOfOperation = "Накопление",
            category = "Вклад",
            amount = 10_000.0,
            currency = "₽",
            description = "На машину",
            status = "Выполнено",
            debitCardNumber = ""
        )

        dao.insert(saving)
        val allSavings = dao.getAllSave().first()

        assertEquals(1, allSavings.size)
        assertEquals("На машину", allSavings[0].description)
    }

    // ================================================================
    // 4. Фильтр по типу операции
    // ================================================================
    @Test
    fun getAllOperations_mixed_filterByType(): Unit = runBlocking {
        val dao = db.dataExcelDao()

        dao.insert(createTransaction("Расходы", "Еда", 500.0))
        dao.insert(createTransaction("Расходы", "Кафе", 300.0))
        dao.insert(createTransaction("Зачисления", "Зарплата", 10_000.0))
        dao.insert(createTransaction("Накопление", "Вклад", 2_000.0))

        val expenses = dao.getAllExpenses().first()
        val incomes = dao.getAllIncome().first()
        val savings = dao.getAllSave().first()

        assertEquals(2, expenses.size)
        assertEquals(1, incomes.size)
        assertEquals(1, savings.size)
    }

    // ================================================================
    // 5. Все транзакции
    // ================================================================
    @Test
    fun getAll_returnsAllTransactions(): Unit = runBlocking {
        val dao = db.dataExcelDao()

        repeat(5) { i ->
            dao.insert(createTransaction("Расходы", "Категория $i", 100.0 * (i + 1)))
        }

        val all = dao.GetAll().first()
        assertEquals(5, all.size)
    }

    // ================================================================
    // 6. Транзакции с разными датами сохраняются
    // ================================================================
    @Test
    fun transactions_bothDates_savedCorrectly(): Unit = runBlocking {
        val dao = db.dataExcelDao()

        val oldTransaction = createTransaction("Расходы", "Еда", 500.0, LocalDateTime.now().minusDays(2))
        val newTransaction = createTransaction("Расходы", "Кафе", 300.0, LocalDateTime.now())

        dao.insert(oldTransaction)
        dao.insert(newTransaction)

        val all = dao.getAllExpenses().first()
        assertEquals(2, all.size)

        val dates = all.map { it.date.toLocalDate() }
        assertTrue("Должна быть старая дата", dates.any { it == LocalDateTime.now().minusDays(2).toLocalDate() })
        assertTrue("Должна быть новая дата", dates.any { it == LocalDateTime.now().toLocalDate() })
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