package com.example.finances

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class TransactionNotificationService : NotificationListenerService() {

    companion object {
        val pendingRepo = MainActivity.PendingRepositoryHolder.repository
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        val packageName = sbn.packageName

        // Получаем текст уведомления
        val text = sbn.notification?.extras?.getString(android.app.Notification.EXTRA_TEXT) ?: ""
        val bigText = sbn.notification?.extras?.getString(android.app.Notification.EXTRA_BIG_TEXT) ?: ""
        val fullText = "$text $bigText".trim()

        if (fullText.isBlank()) {
            Log.d("NotifSMS", "Пустое уведомление от: $packageName")
            return
        }

        Log.d("NotifSMS", "=== НОВОЕ УВЕДОМЛЕНИЕ ===")
        Log.d("NotifSMS", "Пакет: $packageName")
        Log.d("NotifSMS", "Текст: $fullText")

        val (amount, type, description) = parseTransaction(fullText, packageName)

        if (amount == null) {
            Log.d("NotifSMS", "Сумма не найдена")
            return
        }

        val pending = PendingTransaction(
            amount = amount,
            type = type,
            description = description
        )

        pendingRepo.add(pending)
        Log.d("NotifSMS", "✅ Добавлено: ${amount}₽ | $type | $description")
    }

    private fun parseTransaction(text: String, packageName: String): Triple<Double?, String, String> {
        // Определяем тип операции
        return when {
            // ЗАЧИСЛЕНИЯ: есть + и ₽ (для push от Сбера)
            text.contains("+") && text.contains("₽") -> {
                Triple(extractAmount(text), "Зачисления", "Пополнение")
            }
            // ЗАЧИСЛЕНИЯ: Зачисление, Перевод по СБП с + (для СМС)
            text.contains("Зачисление") || (text.contains("Перевод по СБП") && text.contains("+")) -> {
                parseIncome(text)
            }
            // РАСХОДЫ: Покупка, Оплата, Перевод (без +)
            text.contains("Покупка") || text.contains("Оплата") || (text.contains("Перевод") && !text.contains("+")) -> {
                parseExpense(text)
            }
            // ПО УМОЛЧАНИЮ (для push без +, например "Списание")
            else -> {
                val defaultDesc = when {
                    packageName.contains("sber") -> "Сбер"
                    packageName.contains("tinkoff") -> "Т-Банк"
                    else -> "Банк"
                }
                Triple(extractAmount(text), "Расходы", defaultDesc)
            }
        }
    }

    private fun parseIncome(text: String): Triple<Double?, String, String> {
        val amount = extractAmount(text)

        // Ищем имя отправителя (после "от")
        val senderRegex = Regex("от\\s+([А-Я][А-Яа-я]+\\s+[А-Я]\\.?)")
        val sender = senderRegex.find(text)?.groupValues?.get(1)?.trim()

        val description = when {
            text.contains("Перевод по СБП") && sender != null -> "Перевод от $sender"
            text.contains("Зачисление выплаты самозанятым") -> "Выплата самозанятому"
            text.contains("Зачисление с брокерского счёта") -> "С брокерского счёта"
            text.contains("Зачисление") && text.contains("от") && sender != null -> "Зачисление от $sender"
            text.contains("Зачисление") -> {
                val descRegex = Regex("Зачисление\\s+([А-Яа-я\\s]+?)\\d")
                val customDesc = descRegex.find(text)?.groupValues?.get(1)?.trim()
                customDesc ?: "Зачисление"
            }
            else -> "Пополнение"
        }

        return Triple(amount, "Зачисления", description)
    }

    private fun parseExpense(text: String): Triple<Double?, String, String> {
        val amount = extractAmount(text)

        // Ищем магазин/получателя
        val merchantRegex = Regex("(?:Покупка|Оплата|Перевод)\\s+\\d+[\\s\\d]*[рР]\\s+([А-Яа-я0-9_\\-]+)")
        val merchant = merchantRegex.find(text)?.groupValues?.get(1)?.trim()

        // Ищем банк получателя для переводов
        val bankRegex = Regex("Перевод\\s+([А-Я]{3,})")
        val bank = bankRegex.find(text)?.groupValues?.get(1)?.trim()

        val description = when {
            merchant != null -> "Покупка в $merchant"
            bank != null -> "Перевод в $bank"
            text.contains("Покупка") -> "Покупка"
            text.contains("Оплата") -> "Оплата"
            text.contains("Перевод") -> "Перевод"
            else -> "Списание"
        }

        return Triple(amount, "Расходы", description)
    }

    private fun extractAmount(text: String): Double? {
        // Ищем сумму: 229.80р, 41 300р, 10р, 169.99р, 10 ₽
        val regex = Regex("(\\d+[\\s]?[\\d]*[\\.]?\\d*)\\s*[рР₽]")
        val match = regex.find(text)
        val amountStr = match?.groupValues?.get(1)?.trim()?.replace(" ", "")?.replace(",", ".")
        return amountStr?.toDoubleOrNull()
    }
}