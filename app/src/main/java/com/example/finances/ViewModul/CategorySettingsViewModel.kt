package com.example.finances


import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class CategorySettingsViewModel(application: Application) : AndroidViewModel(application) {


    private val db = DataTransactions.getDatabase(application)


    data class CategoryWithImportance(
        val category: String,
        val importance: ImportanceType? = null,
        val isUserDefined: Boolean = false
    )


    private val _categories = MutableStateFlow<List<CategoryWithImportance>>(emptyList())
    val categories: StateFlow<List<CategoryWithImportance>> = _categories.asStateFlow()


    fun loadCategories() {
        viewModelScope.launch {
            val expenses = db.dataExcelDao().getAllExpenses().first()
            val uniqueCategories = expenses.map { it.category }.distinct().toSet()

            val savedList = db.categorySettingsDao().getAllSettings().first()

            val result = uniqueCategories.map { cat ->
                val saved = savedList.find { it.category == cat }
                CategoryWithImportance(
                    category = cat,
                    importance = saved?.importance,
                    isUserDefined = saved != null
                )
            }.sortedBy { it.category }

            _categories.value = result
            Log.d("CategoryVM", "Загружено ${result.size} категорий (с настройками: ${savedList.size})")
        }
    }

    /**
     * Сохраняет ТОЛЬКО чистые категории (без |||).
     * Полностью пересоздаёт таблицу при каждом вызове.
     * При сохранении категории удаляет ВСЕ подкатегории этой категории из expense_settings.
     */
    fun saveSelectedCategories(selections: Map<String, ImportanceType>) {
        viewModelScope.launch {
            try {
                // 1. Фильтруем — оставляем только чистые категории (без |||)
                val pureCategories = selections.filter { (key, _) ->
                    !key.contains("|||")
                }

                if (pureCategories.isEmpty()) {
                    Log.d("CategoryVM", "Нет чистых категорий для сохранения, очищаем таблицу")
                    db.categorySettingsDao().deleteAll()
                    return@launch
                }

                Log.d("CategoryVM", "Сохраняемые категории: ${pureCategories.keys}")

                // 2. Для каждой сохраняемой категории удаляем её подкатегории из expense_settings
                pureCategories.forEach { (category, _) ->
                    db.expenseSettingsDao().deleteByCategory(category)
                    Log.d("CategoryVM", "Удалены подкатегории для: $category")
                }

                // 3. Полностью очищаем таблицу category_settings
                db.categorySettingsDao().deleteAll()
                Log.d("CategoryVM", "Таблица category_settings очищена")

                // 4. Сохраняем только чистые категории
                val settingsList = pureCategories.map { (category, importance) ->
                    CategorySettings(
                        category = category,
                        importance = importance,
                        isUserDefined = true
                    )
                }

                db.categorySettingsDao().insertAll(settingsList)

                Log.d("CategoryVM", "Сохранено ${settingsList.size} категорий: ${settingsList.map { it.category }}")

            } catch (e: Exception) {
                Log.e("CategoryVM", "Ошибка сохранения категорий", e)
            }
        }
    }

    /**
     * Загружает все сохранённые настройки
     */
    fun getAllSettings() =   db.categorySettingsDao().getAllSettings()

    /**
     * Получает настройки по важности
     */
    fun getByImportance(importance: ImportanceType) =    db.categorySettingsDao().getByImportance(importance)

}



