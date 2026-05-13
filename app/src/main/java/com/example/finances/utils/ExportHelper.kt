package com.example.finances

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExportHelper(private val context: Context) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    private val dateFileFormatter = DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm_ss")

    suspend fun exportAllData(
        transactions: List<DateExcel>,
        goals: List<DataGoal>,
        categorySettings: List<CategorySettings>,
        expenseSettings: List<ExpenseSettings>,
        balance: Balance?
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val workbook = XSSFWorkbook()

            // Создаём стили
            val headerStyle = createHeaderStyle(workbook)
            val dateStyle = createDateStyle(workbook)
            val moneyStyle = createMoneyStyle(workbook)

            // 1. Лист с транзакциями
            createTransactionsSheet(workbook, transactions, headerStyle, dateStyle, moneyStyle)

            // 2. Лист с целями
            createGoalsSheet(workbook, goals, headerStyle, moneyStyle, dateStyle)

            // 3. Лист с настройками категорий
            createCategorySettingsSheet(workbook, categorySettings, headerStyle)

            // 4. Лист с детальными настройками трат
            createExpenseSettingsSheet(workbook, expenseSettings, headerStyle)

            // 5. Лист с балансом
            createBalanceSheet(workbook, balance, headerStyle, moneyStyle)

            // Сохраняем файл
            val fileName = "finance_export_${java.time.LocalDateTime.now().format(dateFileFormatter)}.xlsx"
            val file = saveWorkbook(workbook, fileName)

            // Делимся файлом
            shareFile(file)

            workbook.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun createHeaderStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        font.bold = true
        font.color = IndexedColors.WHITE.index.toShort()
        style.setFont(font)
        style.fillForegroundColor = IndexedColors.DARK_BLUE.index.toShort()
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        return style
    }

    private fun createDateStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val format = workbook.createDataFormat()
        style.dataFormat = format.getFormat("dd.MM.yyyy HH:mm")
        return style
    }

    private fun createMoneyStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val format = workbook.createDataFormat()
        style.dataFormat = format.getFormat("#,##0.00")
        return style
    }

    private fun createTransactionsSheet(
        workbook: Workbook,
        transactions: List<DateExcel>,
        headerStyle: CellStyle,
        dateStyle: CellStyle,
        moneyStyle: CellStyle
    ) {
        val sheet = workbook.createSheet("Транзакции")

        // Заголовки
        val headers = arrayOf("ID", "Дата", "Тип операции", "Категория", "Сумма", "Валюта", "Описание", "Статус", "Номер карты")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }

        // Данные
        transactions.forEachIndexed { index, transaction ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(transaction.id.toDouble())
            row.createCell(1).setCellValue(transaction.date.format(dateFormatter))
            row.createCell(2).setCellValue(transaction.typeOfOperation)
            row.createCell(3).setCellValue(transaction.category)

            val amountCell = row.createCell(4)
            amountCell.setCellValue(transaction.amount)
            amountCell.cellStyle = moneyStyle

            row.createCell(5).setCellValue(transaction.currency)
            row.createCell(6).setCellValue(transaction.description)
            row.createCell(7).setCellValue(transaction.status)
            row.createCell(8).setCellValue(transaction.debitCardNumber)
        }

        // Устанавливаем ширину колонок вручную (вместо autoSizeColumn)
        for (i in headers.indices) {
            sheet.setColumnWidth(i, 4000)  // фиксированная ширина
        }
    }

    private fun createGoalsSheet(
        workbook: Workbook,
        goals: List<DataGoal>,
        headerStyle: CellStyle,
        moneyStyle: CellStyle,
        dateStyle: CellStyle
    ) {
        val sheet = workbook.createSheet("Цели")

        val headers = arrayOf("ID", "Название", "Сумма цели", "Накоплено", "Осталось", "Срок (мес)", "Ежемесячный платёж", "Дата создания", "Прогресс %")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }

        goals.forEachIndexed { index, goal ->
            val row = sheet.createRow(index + 1)
            val targetAmount = goal.amount ?: 0.0
            val remaining = targetAmount - goal.savedAmount
            val progress = if (targetAmount > 0) (goal.savedAmount / targetAmount * 100).toInt() else 0

            row.createCell(0).setCellValue(goal.id.toDouble())
            row.createCell(1).setCellValue(goal.name)

            val amountCell = row.createCell(2)
            amountCell.setCellValue(targetAmount)
            amountCell.cellStyle = moneyStyle

            val savedCell = row.createCell(3)
            savedCell.setCellValue(goal.savedAmount)
            savedCell.cellStyle = moneyStyle

            val remainingCell = row.createCell(4)
            remainingCell.setCellValue(remaining)
            remainingCell.cellStyle = moneyStyle

            row.createCell(5).setCellValue(goal.monthsCount.toDouble())

            val paymentCell = row.createCell(6)
            paymentCell.setCellValue(goal.monthlyPayment)
            paymentCell.cellStyle = moneyStyle

            row.createCell(7).setCellValue(goal.createdDate.format(dateFormatter))
            row.createCell(8).setCellValue(progress.toDouble())
        }

        for (i in headers.indices) {
            sheet.setColumnWidth(i, 4000)
        }
    }

    private fun createCategorySettingsSheet(
        workbook: Workbook,
        settings: List<CategorySettings>,
        headerStyle: CellStyle
    ) {
        val sheet = workbook.createSheet("Настройки категорий")

        val headers = arrayOf("ID", "Категория", "Важность", "Выбрана пользователем")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }

        settings.forEachIndexed { index, setting ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(setting.id.toDouble())
            row.createCell(1).setCellValue(setting.category)
            row.createCell(2).setCellValue(setting.importance.name)
            row.createCell(3).setCellValue(if (setting.isUserDefined) "Да" else "Нет")
        }

        for (i in headers.indices) {
            sheet.setColumnWidth(i, 4000)
        }
    }

    private fun createExpenseSettingsSheet(
        workbook: Workbook,
        settings: List<ExpenseSettings>,
        headerStyle: CellStyle
    ) {
        val sheet = workbook.createSheet("Детальные настройки трат")

        val headers = arrayOf("ID", "Категория", "Описание", "Важность", "Выбрана пользователем")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }

        settings.forEachIndexed { index, setting ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(setting.id.toDouble())
            row.createCell(1).setCellValue(setting.category)
            row.createCell(2).setCellValue(setting.description)
            row.createCell(3).setCellValue(setting.importance.name)
            row.createCell(4).setCellValue(if (setting.isUserDefined) "Да" else "Нет")
        }

        for (i in headers.indices) {
            sheet.setColumnWidth(i, 4000)
        }
    }

    private fun createBalanceSheet(
        workbook: Workbook,
        balance: Balance?,
        headerStyle: CellStyle,
        moneyStyle: CellStyle
    ) {
        val sheet = workbook.createSheet("Баланс")

        val headers = arrayOf("Параметр", "Значение")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
        }

        if (balance != null) {
            val row1 = sheet.createRow(1)
            row1.createCell(0).setCellValue("Текущий баланс")
            val balanceCell = row1.createCell(1)
            balanceCell.setCellValue(balance.currentBalance)
            balanceCell.cellStyle = moneyStyle

            val row2 = sheet.createRow(2)
            row2.createCell(0).setCellValue("Заработная плата")
            val salaryCell = row2.createCell(1)
            salaryCell.setCellValue(balance.currentBalance)
            salaryCell.cellStyle = moneyStyle
        } else {
            sheet.createRow(1).createCell(0).setCellValue("Данные не найдены")
        }

        sheet.setColumnWidth(0, 4000)
        sheet.setColumnWidth(1, 4000)
    }

    private fun saveWorkbook(workbook: Workbook, fileName: String): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            resolver.openOutputStream(uri!!).use { outputStream ->
                workbook.write(outputStream)
            }
            File(context.getExternalFilesDir(null), fileName)
        } else {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
            }
            file
        }
    }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Экспорт данных"))
    }
}