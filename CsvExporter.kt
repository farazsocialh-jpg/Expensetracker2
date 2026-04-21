package com.expensetracker.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.expensetracker.domain.model.Transaction
import java.io.File
import java.io.FileWriter
import java.time.format.DateTimeFormatter

object CsvExporter {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun exportToFile(context: Context, transactions: List<Transaction>): File {
        val file = File(context.cacheDir, "expenses_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            // Header
            writer.appendLine("Date,Time,Merchant,Category,Amount,Balance,Note,Source")
            // Rows
            transactions.forEach { t ->
                writer.appendLine(
                    listOf(
                        t.dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        t.dateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                        "\"${t.merchant.replace("\"", "\"\"")}\"",
                        t.category.displayName,
                        t.amount.toString(),
                        t.balance?.toString() ?: "",
                        "\"${t.note.replace("\"", "\"\"")}\"",
                        if (t.isManual) "Manual" else "SMS"
                    ).joinToString(",")
                )
            }
        }
        return file
    }

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
