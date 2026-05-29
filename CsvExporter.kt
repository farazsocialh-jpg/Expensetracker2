package com.expensetracker.utils
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import java.io.File
import java.io.FileWriter
import java.time.format.DateTimeFormatter
object CsvExporter{
    private val df=DateTimeFormatter.ofPattern("yyyy-MM-dd");private val tf=DateTimeFormatter.ofPattern("HH:mm")
    fun exportToFile(ctx:Context,transactions:List<Transaction>):File{val file=File(ctx.cacheDir,"expenses_${System.currentTimeMillis()}.csv");FileWriter(file).use{w->w.appendLine("Date,Time,Merchant,Type,Category,Amount,Currency,Balance,Card,Account,Bank,Recurring,Note,Source");transactions.forEach{t->w.appendLine(listOf(t.dateTime.format(df),t.dateTime.format(tf),esc(t.merchant),t.transactionType.name,t.category.displayName,t.amount.toString(),t.currency,t.balance?.toString()?:"",t.accountLast4,esc(t.accountLabel),esc(t.bankName),if(t.isRecurring)"Yes" else "No",esc(t.note),if(t.isManual)"Manual" else "SMS").joinToString(","))}};return file}
    private fun esc(v:String)="\"${v.replace("\"","\"\"")}\""
    fun shareFile(ctx:Context,file:File){val uri=FileProvider.getUriForFile(ctx,"${ctx.packageName}.provider",file);ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/csv";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Export"))}
}
