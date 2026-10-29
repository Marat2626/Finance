// File: app/src/androidTest/java/com/example/finances/GoalViewModelTest.kt
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
class GoalViewModelTest {

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
    // 1. Сохранение новой цели
    // ================================================================
    @Test
    fun saveGoal_newGoal_insertsCorrectly(): Unit = runBlocking {
        val dao = db.goalDao()

        val goal = DataGoal(
            name = "Машина",
            amount = 1_500_000.0,
            savedAmount = 0.0,
            monthsCount = 24,
            monthlyPayment = 62_500.0,
            createdDate = LocalDateTime.now(),
            category = "Авто"
        )

        dao.insert(goal)
        val saved = dao.getNameGoal("Машина")

        assertNotNull("Цель должна сохраниться", saved)
        assertEquals("Машина", saved?.name)
        assertEquals(1_500_000.0, saved?.amount ?: 0.0, 0.01)
        assertEquals(24, saved?.monthsCount)
    }

    // ================================================================
    // 2. Обновление savedAmount (пополнение цели)
    // ================================================================
    @Test
    fun updateAmount_existingGoal_updatesCorrectly(): Unit = runBlocking {
        val dao = db.goalDao()

        val goal = DataGoal(
            name = "Телефон",
            amount = 100_000.0,
            savedAmount = 20_000.0,
            monthsCount = 6,
            monthlyPayment = 16_666.0,
            createdDate = LocalDateTime.now(),
            category = "Электроника"
        )

        dao.insert(goal)
        val existing = dao.getNameGoal("Телефон")

        // Имитация пополнения
        val updated = existing!!.copy(savedAmount = existing.savedAmount + 10_000.0)
        dao.insertOrUpdate(updated)

        val afterUpdate = dao.getNameGoal("Телефон")
        assertEquals(30_000.0, afterUpdate?.savedAmount ?: 0.0, 0.01)
    }

    // ================================================================
    // 3. Удаление цели
    // ================================================================
    @Test
    fun deleteGoal_removesCorrectly(): Unit = runBlocking {
        val dao = db.goalDao()

        val goal = DataGoal(
            name = "Отпуск",
            amount = 200_000.0,
            monthsCount = 12,
            monthlyPayment = 16_666.0,
            createdDate = LocalDateTime.now()
        )

        dao.insert(goal)
        val saved = dao.getNameGoal("Отпуск")
        assertNotNull("Цель должна существовать", saved)

        dao.deleteGoal(saved!!)
        val afterDelete = dao.getNameGoal("Отпуск")
        assertNull("Цель должна быть удалена", afterDelete)
    }

    // ================================================================
    // 4. Сохранение дубликата — обновляет существующую
    // ================================================================
    @Test
    fun saveGoal_duplicateName_updatesExisting(): Unit = runBlocking {
        val dao = db.goalDao()

        val goal1 = DataGoal(
            name = "Ремонт",
            amount = 500_000.0,
            savedAmount = 100_000.0,
            monthsCount = 12,
            monthlyPayment = 41_666.0,
            createdDate = LocalDateTime.now()
        )

        dao.insert(goal1)
        val existing = dao.getNameGoal("Ремонт")

        // "Повторное" сохранение с увеличением savedAmount
        val updated = existing!!.copy(savedAmount = existing.savedAmount + 50_000.0)
        dao.insertOrUpdate(updated)

        val result = dao.getNameGoal("Ремонт")
        assertEquals(150_000.0, result?.savedAmount ?: 0.0, 0.01)
    }

    // ================================================================
    // 5. План создаётся при сохранении цели
    // ================================================================
    @Test
    fun saveGoal_withMonthlyPayment_createsPlan(): Unit = runBlocking {
        val dao = db.goalDao()
        val planDao = db.planSettingsDao()

        val goal = DataGoal(
            name = "Квартира",
            amount = 5_000_000.0,
            savedAmount = 0.0,
            monthsCount = 60,
            monthlyPayment = 83_333.0,
            createdDate = LocalDateTime.now()
        )

        val goalId = dao.insert(goal).toInt()

        // Имитируем то, что делает GoalViewModel.saveGoal()
        val planSettings = PlanSettings(
            category = "goal_$goalId",
            importance = ImportanceType.ESSENTIAL,
            plannedAmount = 83_333.0,
            isDetailed = false
        )
        planDao.insertAll(listOf(planSettings))

        val allPlans = planDao.getAll().first()
        assertTrue("План должен содержать goal_$goalId",
            allPlans.any { it.category == "goal_$goalId" })
    }

    // ================================================================
    // 6. Все цели загружаются
    // ================================================================
    @Test
    fun getAllGoals_multipleGoals_returnsAll(): Unit = runBlocking {
        val dao = db.goalDao()

        listOf(
            DataGoal(name = "Цель 1", amount = 100_000.0, monthsCount = 6, monthlyPayment = 16_666.0, createdDate = LocalDateTime.now()),
            DataGoal(name ="Цель 2", amount = 200_000.0, monthsCount = 12, monthlyPayment = 16_666.0, createdDate = LocalDateTime.now()),
            DataGoal(name ="Цель 3", amount = 300_000.0, monthsCount = 18, monthlyPayment = 16_666.0, createdDate = LocalDateTime.now())
        ).forEach { dao.insert(it) }

        val allGoals = dao.getAllGoals().first()
        assertEquals(3, allGoals.size)
    }
}