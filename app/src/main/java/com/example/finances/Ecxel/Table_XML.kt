package com.example.finances
import android.content.Context
import android.util.Log

import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook





fun twoReadExcelFiles(context: Context, fileName1: String, fileName2: String):  List<DateExcel>{
    val allTransactions = mutableListOf<DateExcel>()

    allTransactions.addAll(readExcelFiles(context, fileName1))

    allTransactions.addAll(readExcelFiles(context, fileName2))

    return allTransactions
}
fun readExcelFiles(context: Context, fileName: String): List<DateExcel>{
    val inputStream = context.assets.open(fileName)
    val transaction = mutableListOf<DateExcel>()
    val workbook: Workbook = XSSFWorkbook(inputStream)

    try {
        val sheet = workbook.getSheetAt(0)

        for (row in sheet){
            if (row.rowNum > 0){

                val date = row.getCell(1)
                val dates = date?.toString() ?: ""
                val typeOfOperation = row.getCell(2).stringCellValue
                val category = row.getCell(3).stringCellValue
                val amount = row.getCell(4).numericCellValue
                val currency = row.getCell(5).stringCellValue
                val description = row.getCell(7).stringCellValue
                val status= row.getCell(8).stringCellValue
                val debitCardNumber= row.getCell(9).stringCellValue

                val transactions = DateExcel(
                    id = 0, dates, typeOfOperation, category, amount,
                    currency, description, status, debitCardNumber
                )
                transaction.add(transactions)
            }
        }
        Log.d("Excel", "=== Всего строк: ${transaction.size} ===")
    }
    finally {
        workbook.close()
    }
    return transaction
}