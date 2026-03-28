package com.example.finances

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters



@Database(entities = [DateExcel::class,
    Balance::class,
    Save::class,
    DataGoal::class,
    CategorySettings::class,
    ExpenseSettings::class ],
    version = 1,exportSchema = false)
@TypeConverters(Converters::class)
abstract class DataTransactions: RoomDatabase() {
    abstract fun dataExcelDao(): DataExellDao
    abstract fun balanceDao(): BalanceDao
    abstract fun saveDao(): SaveDao
    abstract fun goalDao(): GoalDao
    abstract fun expenseSettingsDao(): ExpenseSettingsDao
    abstract fun categorySettingsDao(): CategorySettingsDao
    companion object{
        fun getDatabase(context: Context): DataTransactions{
            return Room.databaseBuilder(
                context,
                DataTransactions::class.java,
                "data"
            ).build()
        }
    }
}