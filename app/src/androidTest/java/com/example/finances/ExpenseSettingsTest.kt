// File: app/src/androidTest/java/com/example/finances/ExpenseSettingsTest.kt
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

@RunWith(AndroidJUnit4::class)
class ExpenseSettingsTest {

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
    // 1. Вставка настроек
    // ================================================================
    @Test
    fun insertExpenseSettings_savesCorrectly(): Unit = runBlocking {
        val dao = db.expenseSettingsDao()

        dao.insertAll(
            listOf(
                ExpenseSettings(
                    category = "Кафе",
                    description = "Кофе утром",
                    importance = ImportanceType.NON_ESSENTIAL,
                    isUserDefined = true
                )
            )
        )

        val all = dao.getAllSettings().first()
        assertEquals(1, all.size)
        assertEquals("Кафе", all[0].category)
        assertEquals("Кофе утром", all[0].description)
        assertEquals(ImportanceType.NON_ESSENTIAL, all[0].importance)
    }

    // ================================================================
    // 2. Поиск по категории и описанию
    // ================================================================
    @Test
    fun getByCategoryAndDescription_returnsCorrect(): Unit = runBlocking {
        val dao = db.expenseSettingsDao()

        dao.insertAll(
            listOf(
                ExpenseSettings(category ="Транспорт", description ="Метро", importance = ImportanceType.ESSENTIAL),
                ExpenseSettings(category ="Транспорт", description ="Такси", importance = ImportanceType.NON_ESSENTIAL)
            )
        )

        val metro = dao.getByCategoryAndDescription("Транспорт", "Метро")
        val taxi = dao.getByCategoryAndDescription("Транспорт", "Такси")

        assertEquals(ImportanceType.ESSENTIAL, metro?.importance)
        assertEquals(ImportanceType.NON_ESSENTIAL, taxi?.importance)
    }

    // ================================================================
    // 3. Поиск несуществующей → null
    // ================================================================
    @Test
    fun getByCategoryAndDescription_notFound_returnsNull(): Unit = runBlocking {
        val dao = db.expenseSettingsDao()

        val result = dao.getByCategoryAndDescription("Несуществующее", "Нет такого")
        assertNull("Должен вернуть null", result)
    }

    // ================================================================
    // 4. Удаление по категории
    // ================================================================
    @Test
    fun deleteByCategory_removesAllInCategory(): Unit = runBlocking {
        val dao = db.expenseSettingsDao()

        dao.insertAll(
            listOf(
                ExpenseSettings(category = "Кафе", description = "Кофе", importance = ImportanceType.NON_ESSENTIAL),
                ExpenseSettings(category = "Кафе",description =  "Десерт",importance = ImportanceType.NON_ESSENTIAL),
                ExpenseSettings(category = "Еда", description = "Продукты", importance = ImportanceType.ESSENTIAL)
            )
        )

        assertEquals(3, dao.getAllSettings().first().size)

        dao.deleteByCategory("Кафе")

        val remaining = dao.getAllSettings().first()
        assertEquals(1, remaining.size)
        assertEquals("Еда", remaining[0].category)
    }

    // ================================================================
    // 5. Множественная вставка + getAllSync
    // ================================================================
    @Test
    fun getAllSync_returnsAll(): Unit = runBlocking {
        val dao = db.expenseSettingsDao()

        val settings = listOf(
            ExpenseSettings(category = "Еда",description =  "Фастфуд", importance = ImportanceType.NON_ESSENTIAL),
            ExpenseSettings(category = "Еда", description = "Ресторан", importance = ImportanceType.NEUTRAL),
            ExpenseSettings(category = "Здоровье", description = "Аптека", importance = ImportanceType.ESSENTIAL),
            ExpenseSettings(category = "Развлечения", description = "Кино", importance = ImportanceType.NON_ESSENTIAL)
        )

        dao.insertAll(settings)
        val all = dao.getAllSync()

        assertEquals(4, all.size)
        val categories = all.map { it.category }.toSet()
        assertTrue(categories.contains("Еда"))
        assertTrue(categories.contains("Здоровье"))
        assertTrue(categories.contains("Развлечения"))
    }

    // ================================================================
    // 6. Обновление важности (перезапись)
    // ================================================================
    @Test
    fun updateImportance_changesCorrectly(): Unit = runBlocking {
        val dao = db.expenseSettingsDao()

        dao.insertAll(
            listOf(ExpenseSettings(category = "Кафе", description = "Латте",importance = ImportanceType.NEUTRAL))
        )

        // Удаляем и вставляем новое
        dao.deleteByCategory("Кафе")
        dao.insertAll(
            listOf(ExpenseSettings(category = "Кафе", description = "Латте", importance = ImportanceType.NON_ESSENTIAL))
        )

        val updated = dao.getByCategoryAndDescription("Кафе", "Латте")
        assertEquals(ImportanceType.NON_ESSENTIAL, updated?.importance)
    }
}