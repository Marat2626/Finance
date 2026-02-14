package com.example.finances

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.Flow


class EISViewModelFactory(
    private val application: Application,
    private val daoMethod: suspend (DataExellDao) -> Flow<List<DateExcel>>,
    private val operationType: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EISviewModel(
            application = application,
            daoMethod = daoMethod,
            operationType = operationType
        ) as T
    }
}