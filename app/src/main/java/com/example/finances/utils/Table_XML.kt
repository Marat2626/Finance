package com.example.finances
import android.util.Log

import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

//
//fun twoReadExcelFiles(context: Context, fileName1: String, fileName2: String):  List<DateExcel>{
//
//    Log.d("Excel", "Начинаем создавать 2 файла  $fileName1 $fileName2 ")
//
//    val allTransactions = mutableListOf<DateExcel>()
//
//    allTransactions.addAll(readExcelFiles(context, fileName1))
//
//    allTransactions.addAll(readExcelFiles(context, fileName2))
//
//    return allTransactions
//}
//fun readExcelFiles(context: Context, fileName: String): List<DateExcel>{
//
//    Log.d("Excel", "Читаю файл: $fileName")
//
//    val inputStream = context.assets.open(fileName)
//    Log.d("Excel", "Файл открыт, размер: ${inputStream.available()}")
//    val transaction = mutableListOf<DateExcel>()
//    val workbook: Workbook = XSSFWorkbook(inputStream)
//
//
//    try {
//
//        Log.d("Excel", "Начинаем ПАрсить $fileName ")
//        val sheet = workbook.getSheetAt(0)
//        val formatter = DateTimeFormatter.ofPattern("dd MMM. yyyy, HH:mm", Locale.ENGLISH)
//        for (row in sheet){
//            if (row.rowNum > 0){
//
//                val dateCell = row.getCell(1).stringCellValue
//               val englishDate = dateCell
//                    .replace("янв.", "Jan")
//                    .replace("фев.", "Feb")
//                    .replace("мар.", "Mar")
//                    .replace("апр.", "Apr")
//                    .replace("мая", "May")
//                    .replace("июн.", "Jun")
//                    .replace("июл.", "Jul")
//                    .replace("авг.", "Aug")
//                    .replace("сен.", "Sep")
//                    .replace("окт.", "Oct")
//                   .replace("нояб", "Nov")
//                    .replace("дек.", "Dec")
//
//                Log.d("Excel", "=== Всего строк: ${englishDate} ===")
//                val dates = LocalDateTime.parse(englishDate, formatter)
//                val typeOfOperation = row.getCell(2).stringCellValue
//                val category = row.getCell(3).stringCellValue
//                val amount = row.getCell(4).numericCellValue
//                val currency = row.getCell(5).stringCellValue
//                val description = row.getCell(7).stringCellValue
//                val status= row.getCell(8).stringCellValue
//                val debitCardNumber= row.getCell(9).stringCellValue
//
//                val transactions = DateExcel(
//                    id = 0,
//                    dates, typeOfOperation, category, amount,
//                    currency, description, status, debitCardNumber
//                )
//                transaction.add(transactions)
//            }
//        }
//        Log.d("Excel", "=== Всего строк:${transaction.size}===")
//    }
//    finally {
//        workbook.close()
//    }
//    return transaction
//
//
//}


// Добавьте этот метод в Table_XML.kt
fun readExcelFromStream(inputStream: InputStream, fileName: String): List<DateExcel> {
    Log.d("Excel", "Читаю файл из потока: $fileName")

    val transaction = mutableListOf<DateExcel>()
    val workbook: Workbook = XSSFWorkbook(inputStream)

    try {
        Log.d("Excel", "Начинаем парсить $fileName")
        val sheet = workbook.getSheetAt(0)
        val formatter = DateTimeFormatter.ofPattern("dd MMM. yyyy, HH:mm", Locale.ENGLISH)

        for (row in sheet) {
            if (row.rowNum > 0) {
                val dateCell = row.getCell(1).stringCellValue
                val englishDate = dateCell
                    .replace("янв.", "Jan")
                    .replace("фев", "Feb")
                    .replace("мар", "Mar")
                    .replace("апр", "Apr")
                    .replace("мая", "May")
                    .replace("июн", "Jun")
                    .replace("июл", "Jul")
                    .replace("авг", "Aug")
                    .replace("сен", "Sep")
                    .replace("окт", "Oct")
                    .replace("нояб", "Nov")
                    .replace("дек", "Dec")

                Log.d("Excel", "Дата: $englishDate")
                val dates = LocalDateTime.parse(englishDate, formatter)
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