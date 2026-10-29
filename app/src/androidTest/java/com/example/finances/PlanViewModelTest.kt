// File: app/src/androidTest/java/com/example/finances/CategorySettingsTest.kt
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
class CategorySettingsTest {

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
    // 1. Сохранение настроек категории
    // ================================================================
    @Test
    fun insertCategorySettings_savesCorrectly(): Unit = runBlocking {
        val dao = db.categorySettingsDao()

        dao.insertAll(
            listOf(
                CategorySettings(category = "Еда", importance = ImportanceType.ESSENTIAL),
                CategorySettings(category = "Кафе", importance = ImportanceType.NON_ESSENTIAL)
            )
        )

        val all = dao.getAllSettings().first()
        assertEquals(2, all.size)
    }

    // ================================================================
    // 2. Поиск по категории
    // ================================================================
    @Test
    fun getByCategory_returnsCorrectImportance(): Unit = runBlocking {
        val dao = db.categorySettingsDao()

        dao.insertAll(
            listOf(
                CategorySettings(category = "Транспорт", importance = ImportanceType.NEUTRAL)
            )
        )

        val all = dao.getAllSettings().first()
        val transport = all.find { it.category == "Транспорт" }
        assertNotNull("Категория должна быть найдена", transport)
        assertEquals(ImportanceType.NEUTRAL, transport?.importance)
    }

    // ================================================================
    // 3. Удаление категории
    // ================================================================
    @Test
    fun deleteByCategory_removesCorrectly(): Unit = runBlocking {
        val dao = db.categorySettingsDao()

        dao.insertAll(
            listOf(
                CategorySettings(category = "Еда", importance = ImportanceType.ESSENTIAL),
                CategorySettings(category = "Кафе", importance = ImportanceType.NON_ESSENTIAL)
            )
        )

        dao.deleteByCategory("Кафе")

        val remaining = dao.getAllSettings().first()
        assertEquals(1, remaining.size)
        assertEquals("Еда", remaining[0].category)
    }

    // ================================================================
    // 4. Обновление важности
    // ================================================================
    @Test
    fun updateImportance_changesCorrectly(): Unit = runBlocking {
        val dao = db.categorySettingsDao()

        dao.insertAll(
            listOf(CategorySettings(category = "Развлечения", importance = ImportanceType.NEUTRAL))
        )

        // Удаляем старую и вставляем новую
        dao.deleteByCategory("Развлечения")
        dao.insertAll(
            listOf(CategorySettings(category = "Развлечения", importance = ImportanceType.NON_ESSENTIAL))
        )

        val updated = dao.getAllSettings().first()
        assertEquals(ImportanceType.NON_ESSENTIAL, updated[0].importance)
    }

    // ================================================================
    // 5. getAllSync — синхронный метод
    // ================================================================
    @Test
    fun getAllSync_returnsList(): Unit = runBlocking {
        val dao = db.categorySettingsDao()

        dao.insertAll(
            listOf(CategorySettings(category = "Здоровье", importance = ImportanceType.ESSENTIAL))
        )

        val sync = dao.getAllSync()
        assertEquals(1, sync.size)
        assertEquals("Здоровье", sync[0].category)
    }
}