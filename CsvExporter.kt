package com.expensetracker.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.expensetracker.domain.model.Transaction
import java.io.File
import java.io.FileWriter
import java.time.format.DateTimeFormatter

object CsvExporter {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun exportToFile(context: Context, transactions: List<Transaction>): File {
        val file = File(context.cacheDir, "expenses_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { w ->
            w.appendLine("Date,Time,Merchant,Category,Amount (QAR),Balance,Card,Account,Note,Source,SMS")
            transactions.forEach { t ->
                w.appendLine(listOf(
                    t.dateTime.format(dateFormatter),
                    t.dateTime.format(timeFormatter),
                    csvEscape(t.merchant),
                    t.category.displayName,
                    t.amount.toString(),
                    t.balance?.toString() ?: "",
                    t.cardNumber,
                    csvEscape(t.accountLabel),
                    csvEscape(t.note),
                    if (t.isManual) "Manual" else "SMS",
                    csvEscape(t.rawSms ?: "")
                ).joinToString(","))
            }
        }
        return file
    }

    private fun csvEscape(value: String): String = "\"${value.replace("\"", "\"\"")}\""

    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Expenses"))
    }
}
