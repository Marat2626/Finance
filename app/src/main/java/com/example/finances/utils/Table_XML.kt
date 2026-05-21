package com.example.finances
import android.util.Log

import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Добавьте этот метод в Table_XML.kt
fun readExcelFromStream(inputStream: InputStream, fileName: String): List<DateExcel> {
    Log.d("Excel", "Читаю файл из потока: $fileName")

    val transaction = mutableListOf<DateExcel>()
    val workbook: Workbook = XSSFWorkbook(inputStream)

    try {
        Log.d("Excel", "Начинаем парсить $fileName")
        val sheet = workbook.getSheetAt(0)

        // Поддерживаем оба формата даты
        val formatterWithDot = DateTimeFormatter.ofPattern("dd MMM. yyyy, HH:mm", Locale.ENGLISH)
        val formatterWithoutDot = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH)

        for (row in sheet) {
            if (row.rowNum > 0) {
                val dateCell = row.getCell(1).stringCellValue
                val englishDate = dateCell
                    .replace("янв.", "Jan")
                    .replace("фев.", "Feb")
                    .replace("мар.", "Mar")
                    .replace("апр.", "Apr")
                    .replace("мая", "May")
                    .replace("июн.", "Jun")
                    .replace("июл.", "Jul")
                    .replace("авг.", "Aug")
                    .replace("сен.", "Sep")
                    .replace("окт.", "Oct")
                    .replace("нояб", "Nov")
                    .replace("дек.", "Dec")
                    // Убираем точки из названий месяцев, если они остались
                    .replace("Jan.", "Jan")
                    .replace("Feb.", "Feb")
                    .replace("Mar.", "Mar")
                    .replace("Apr.", "Apr")
                    .replace("May.", "May")
                    .replace("Jun.", "Jun")
                    .replace("Jul.", "Jul")
                    .replace("Aug.", "Aug")
                    .replace("Sep.", "Sep")
                    .replace("Oct.", "Oct")
                    .replace("Nov.", "Nov")
                    .replace("Dec.", "Dec")

                Log.d("Excel", "Дата после замены: $englishDate")

                val dates = try {
                    // Сначала пробуем формат без точки
                    LocalDateTime.parse(englishDate, formatterWithoutDot)
                } catch (e: Exception) {
                    try {
                        // Если не получилось, пробуем формат с точкой
                        LocalDateTime.parse(englishDate, formatterWithDot)
                    } catch (e2: Exception) {
                        Log.e("Excel", "Не удалось распарсить дату: $englishDate", e2)
                        continue // Пропускаем строку с ошибкой
                    }
                }

                val typeOfOperation = row.getCell(2).stringCellValue
                val category = row.getCell(3).stringCellValue
                val amount = row.getCell(4).numericCellValue
                val currency = row.getCell(5).stringCellValue
                val description = row.getCell(7).stringCellValue
                val status = row.getCell(8).stringCellValue
                val debitCardNumber = row.getCell(9).stringCellValue

                val transactions = DateExcel(
                    id = 0,
                    dates, typeOfOperation, category, amount,
                    currency, description, status, debitCardNumber
                )
                transaction.add(transactions)
            }
        }
        Log.d("Excel", "Всего строк: ${transaction.size}")
    } finally {
        workbook.close()
    }
    return transaction
}