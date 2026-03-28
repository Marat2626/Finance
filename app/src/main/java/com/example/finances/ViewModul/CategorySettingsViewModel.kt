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


    // ←←← НОВЫЙ МЕТОД (главное исправление)
    fun saveSelectedCategories(selections: Map<String, ImportanceType>) {
        viewModelScope.launch {
            try {
                // Очищаем старую таблицу
                db.categorySettingsDao().deleteAll()


                // Сохраняем ТОЛЬКО то, что выбрал пользователь
                val toSave = selections.map { (category, importance) ->
                    CategorySettings(
                        category = category,
                        importance = importance,
                        isUserDefined = true
                    )
                }


                if (toSave.isNotEmpty()) {
                    db.categorySettingsDao().insertAll(toSave)
                    Log.d("CategoryVM", "✅ Сохранено ${toSave.size} настроек в БД")
                }


                printAllSavedSettings()
                loadCategories() // обновляем экран
            } catch (e: Exception) {
                Log.e("CategoryVM", "Ошибка сохранения", e)
            }
        }
    }


    fun printAllSavedSettings() {
        viewModelScope.launch {
            val list = db.categorySettingsDao().getAllSettings().first()
            Log.d("DEBUG_SETTINGS", "=== СОХРАНЁННЫЕ НАСТРОЙКИ В БД ===")
            if (list.isEmpty()) {
                Log.d("DEBUG_SETTINGS", "База пустая")
            } else {
                list.forEach {
                    Log.d("DEBUG_SETTINGS", "Категория: ${it.category} → ${it.importance}")
                }
            }
            Log.d("DEBUG_SETTINGS", "Всего записей: ${list.size}")
        }
    }
}




