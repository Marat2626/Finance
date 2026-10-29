// File: app/src/androidTest/java/com/example/finances/GoalDaoTest.kt
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
class GoalDaoTest {

    private lateinit var db: DataTransactions
    private lateinit var dao: GoalDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = DataTransactions.getTestDatabase(context)
        dao = db.goalDao()
    }

    @After
    fun tearDown() {
        db.close()
        DataTransactions.clearTestDatabase()
    }

    // ================================================================
    // 1. insert + getGoalByName → возвращает сохранённую цель
    // ================================================================
    @Test
    fun insertAndGetByName_returnsSavedGoal(): Unit = runBlocking {
        val goal = DataGoal(
            name = "Машина",
            amount = 1_500_000.0,
            savedAmount = 200_000.0,
            monthsCount = 24,
            monthlyPayment = 62_500.0,
            createdDate = LocalDateTime.now(),
            category = "Авто"
        )

        val id = dao.insert(goal)
        assertTrue("ID должен быть > 0", id > 0)

        val saved = dao.getNameGoal("Машина")
        assertNotNull("Цель должна быть найдена", saved)
        assertEquals("Машина", saved?.name)
        assertEquals(1_500_000.0, saved?.amount ?: 0.0, 0.01)
        assertEquals(200_000.0, saved?.savedAmount ?: 0.0, 0.01)
        assertEquals(24, saved?.monthsCount)
        assertEquals("Авто", saved?.category)
    }

    // ================================================================
    // 2. deleteGoal → цель удаляется
    // ================================================================
    @Test
    fun deleteGoal_removesGoal(): Unit = runBlocking {
        val goal = DataGoal(
            name = "Отпуск",
            amount = 200_000.0,
            savedAmount = 50_000.0,
            monthsCount = 12,
            monthlyPayment = 16_666.0,
            createdDate = LocalDateTime.now(),
            category = "Путешествия"
        )

        dao.insert(goal)
        val saved = dao.getNameGoal("Отпуск")
        assertNotNull("Цель должна существовать до удаления", saved)

        dao.deleteGoal(saved!!)
        val afterDelete = dao.getNameGoal("Отпуск")
        assertNull("Цель должна быть удалена", afterDelete)
    }

    // ================================================================
    // 3. getAllGoals → возвращает Flow со всеми целями
    // ================================================================
    @Test
    fun getAllGoals_returnsAllGoals(): Unit = runBlocking {
        val goal1 = DataGoal(
            name = "Квартира", amount = 5_000_000.0, monthsCount = 60,
            monthlyPayment = 83_333.0, createdDate = LocalDateTime.now(), category = "Недвижимость"
        )
        val goal2 = DataGoal(
            name = "Телефон", amount = 100_000.0, monthsCount = 6,
            monthlyPayment = 16_666.0, createdDate = LocalDateTime.now(), category = "Электроника"
        )
        val goal3 = DataGoal(
            name = "Образование", amount = 300_000.0, monthsCount = 10,
            monthlyPayment = 30_000.0, createdDate = LocalDateTime.now(), category = "Учёба"
        )

        dao.insert(goal1)
        dao.insert(goal2)
        dao.insert(goal3)

        val allGoals = dao.getAllGoals().first()
        assertEquals("Должно быть 3 цели", 3, allGoals.size)

        val names = allGoals.map { it.name }
        assertTrue(names.contains("Квартира"))
        assertTrue(names.contains("Телефон"))
        assertTrue(names.contains("Образование"))
    }

    // ================================================================
    // 4. insertOrUpdate → обновляет существующую цель
    // ================================================================
    @Test
    fun insertOrUpdate_updatesExistingGoal(): Unit = runBlocking {
        val goal = DataGoal(
            name = "Ремонт",
            amount = 500_000.0,
            savedAmount = 100_000.0,
            monthsCount = 12,
            monthlyPayment = 41_666.0,
            createdDate = LocalDateTime.now(),
            category = "Дом"
        )

        val id = dao.insert(goal)
        val saved = dao.getNameGoal("Ремонт")

        // Обновляем savedAmount
        val updated = saved!!.copy(savedAmount = 250_000.0)
        dao.insertOrUpdate(updated)

        val afterUpdate = dao.getNameGoal("Ремонт")
        assertEquals(250_000.0, afterUpdate?.savedAmount ?: 0.0, 0.01)
    }

    // ================================================================
    // 5. Поиск несуществующей цели → null
    // ================================================================
    @Test
    fun getByName_notFound_returnsNull(): Unit = runBlocking {
        val result = dao.getNameGoal("НесуществующаяЦель")
        assertNull("Ненайденная цель должна вернуть null", result)
    }

    // ================================================================
    // 6. Пустая база → getAllGoals возвращает пустой список
    // ================================================================
    @Test
    fun getAllGoals_emptyDatabase_returnsEmptyList(): Unit = runBlocking {
        val allGoals = dao.getAllGoals().first()
        assertTrue("Список целей должен быть пустым", allGoals.isEmpty())
    }

    // ================================================================
    // 7. Цель с null amount
    // ================================================================
    @Test
    fun insertGoal_withNullAmount_works(): Unit = runBlocking {
        val goal = DataGoal(
            name = "Подушка",
            amount = null,
            savedAmount = 0.0,
            monthsCount = 6,
            monthlyPayment = 0.0,
            createdDate = LocalDateTime.now(),
            category = ""
        )

        val id = dao.insert(goal)
        assertTrue("ID > 0", id > 0)

        val saved = dao.getNameGoal("Подушка")
        assertNotNull("Цель должна сохраниться", saved)
        assertNull("amount должен быть null", saved?.amount)
    }

    // ================================================================
    // 8. Удаление несуществующей цели — не ломается
    // ================================================================
    @Test
    fun deleteGoal_nonExisting_doesNotCrash(): Unit = runBlocking {
        val fakeGoal = DataGoal(
            id = 9999,
            name = "Фейк",
            amount = 0.0,
            monthsCount = 1,
            monthlyPayment = 0.0,
            createdDate = LocalDateTime.now()
        )

        // Не должно упасть
        dao.deleteGoal(fakeGoal)

        val allGoals = dao.getAllGoals().first()
        assertTrue("База должна быть пустой", allGoals.isEmpty())
    }
}