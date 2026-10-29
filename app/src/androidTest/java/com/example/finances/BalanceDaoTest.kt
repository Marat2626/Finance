// File: app/src/androidTest/java/com/example/finances/BalanceDaoTest.kt
package com.example.finances

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BalanceDaoTest {

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
    // 1. Начальный баланс — null (нет записи)
    // ================================================================
    @Test
    fun getBalance_empty_returnsNull(): Unit = runBlocking {
        val balance = db.balanceDao().getBalance()
        assertNull("Начальный баланс должен быть null", balance)
    }

    // ================================================================
    // 2. Сохранение баланса
    // ================================================================
    @Test
    fun saveBalance_savesCorrectly(): Unit = runBlocking {
        val dao = db.balanceDao()

        dao.saveBalance(Balance(id = 1, currentBalance = 50_000.0))
        val saved = dao.getBalance()

        assertEquals(50_000.0, saved ?: 0.0, 0.01)
    }

    // ================================================================
    // 3. Обновление баланса
    // ================================================================
    @Test
    fun saveBalance_update_overwrites(): Unit = runBlocking {
        val dao = db.balanceDao()

        dao.saveBalance(Balance(id = 1, currentBalance = 50_000.0))
        dao.saveBalance(Balance(id = 1, currentBalance = 75_000.0))

        val updated = dao.getBalance()
        assertEquals(75_000.0, updated ?: 0.0, 0.01)
    }

    // ================================================================
    // 4. Отрицательный баланс
    // ================================================================
    @Test
    fun saveBalance_negative_works(): Unit = runBlocking {
        val dao = db.balanceDao()

        dao.saveBalance(Balance(id = 1, currentBalance = -10_000.0))
        val saved = dao.getBalance()

        assertEquals(-10_000.0, saved ?: 0.0, 0.01)
    }

    // ================================================================
    // 5. Нулевой баланс
    // ================================================================
    @Test
    fun saveBalance_zero_works(): Unit = runBlocking {
        val dao = db.balanceDao()

        dao.saveBalance(Balance(id = 1, currentBalance = 0.0))
        val saved = dao.getBalance()

        assertEquals(0.0, saved ?: 0.0, 0.01)
    }

    // ================================================================
    // 6. Операции с балансом: доход + расход
    // ================================================================
    @Test
    fun balanceOperations_incomeAndExpense(): Unit = runBlocking {
        val dao = db.balanceDao()

        // Начальный баланс
        dao.saveBalance(Balance(id = 1, currentBalance = 100_000.0))

        // Доход
        val current1 = dao.getBalance() ?: 0.0
        val afterIncome = current1 + 50_000.0
        dao.saveBalance(Balance(id = 1, currentBalance = afterIncome))
        assertEquals(150_000.0, dao.getBalance() ?: 0.0, 0.01)

        // Расход
        val current2 = dao.getBalance() ?: 0.0
        val afterExpense = current2 - 30_000.0
        dao.saveBalance(Balance(id = 1, currentBalance = afterExpense))
        assertEquals(120_000.0, dao.getBalance() ?: 0.0, 0.01)
    }
}