package com.example.finances

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters


@Database(entities = [DateExcel::class,
    Balance::class,
    PlanSettings::class,
    DataGoal::class,
    CategorySettings::class,
    ExpenseSettings::class ],
    version = 1,exportSchema = false)
@TypeConverters(Converters::class)
abstract class DataTransactions: RoomDatabase() {
    abstract fun dataExcelDao(): DataExellDao
    abstract fun balanceDao(): BalanceDao
//    abstract fun saveDao(): SaveDao
    abstract fun goalDao(): GoalDao
    abstract fun expenseSettingsDao(): ExpenseSettingsDao
    abstract fun categorySettingsDao(): CategorySettingsDao
    abstract fun planSettingsDao(): PlanSettingsDao
    companion object{


        @Volatile
        private var INSTANCE: DataTransactions? = null

        @Volatile
        private var TEST_INSTANCE: DataTransactions? = null




        fun getDatabase(context: Context): DataTransactions{
            return Room.databaseBuilder(
                context,
                DataTransactions::class.java,
                "data"
            ).build()
        }


        fun getTestDatabase(context: Context): DataTransactions {
            return TEST_INSTANCE ?: synchronized(this) {
                TEST_INSTANCE ?: Room.inMemoryDatabaseBuilder(
                    context,
                    DataTransactions::class.java
                )
                    .allowMainThreadQueries()
                    .build()
                    .also { TEST_INSTANCE = it }
            }
        }

        /**
         * Очистка тестовой БД между тестами
         */
        fun clearTestDatabase() {
            TEST_INSTANCE?.close()
            TEST_INSTANCE = null
        }

    }
}