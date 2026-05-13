package com.example.finances

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LastGoalViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _lastGoalId = MutableStateFlow(prefs.getInt("last_goal_id", -1))
    val lastGoalId: StateFlow<Int> = _lastGoalId.asStateFlow()

    fun saveLastGoalId(goalId: Int) {
        if (goalId != -1) {
            _lastGoalId.value = goalId
            prefs.edit().putInt("last_goal_id", goalId).apply()
        }
    }
}