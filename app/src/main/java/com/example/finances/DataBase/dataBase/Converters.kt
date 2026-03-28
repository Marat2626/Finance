package com.example.finances

import androidx.room.TypeConverter
import java.time.LocalDateTime

class Converters {

    @TypeConverter
    fun localDateTimeToString(date: LocalDateTime): String?{

        return date.toString()

    }

    @TypeConverter
    fun stringToLocalDateTime(dateStr: String?): LocalDateTime?{

        return dateStr?.let { LocalDateTime.parse(it) }
    }
}