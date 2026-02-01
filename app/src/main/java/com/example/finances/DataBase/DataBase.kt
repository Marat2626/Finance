package com.example.finances

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.finances.DataBase.Save.Save
import com.example.finances.DataBase.Save.SaveDao

@Database(entities = [DateExcel::class, Balance::class, Save::class, DataGoal::class], version = 1,exportSchema = false)
abstract class DataTransactions: RoomDatabase() {
    abstract fun dataExcelDao(): DataExellDao
    abstract fun balanceDao(): BalanceDao
    abstract fun saveDao(): SaveDao
    abstract fun goalDao(): GoalDao
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