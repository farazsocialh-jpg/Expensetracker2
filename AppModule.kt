package com.expensetracker
import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expensetracker.data.db.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module @InstallIn(SingletonComponent::class)
object AppModule {
    private val M1_2=object:Migration(1,2){override fun migrate(db:SupportSQLiteDatabase){db.execSQL("ALTER TABLE transactions ADD COLUMN uuid TEXT NOT NULL DEFAULT ''");db.execSQL("ALTER TABLE transactions ADD COLUMN currency TEXT NOT NULL DEFAULT 'QAR'");db.execSQL("ALTER TABLE transactions ADD COLUMN normalizedMerchant TEXT NOT NULL DEFAULT ''");db.execSQL("ALTER TABLE transactions ADD COLUMN subcategory TEXT NOT NULL DEFAULT ''");db.execSQL("ALTER TABLE transactions ADD COLUMN transactionType TEXT NOT NULL DEFAULT 'EXPENSE'");db.execSQL("ALTER TABLE transactions ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'DEBIT_CARD'");db.execSQL("ALTER TABLE transactions ADD COLUMN walletId INTEGER NOT NULL DEFAULT 0");db.execSQL("ALTER TABLE transactions ADD COLUMN walletName TEXT NOT NULL DEFAULT ''");db.execSQL("ALTER TABLE transactions ADD COLUMN bankName TEXT NOT NULL DEFAULT ''");db.execSQL("ALTER TABLE transactions ADD COLUMN accountLast4 TEXT NOT NULL DEFAULT ''");db.execSQL("ALTER TABLE transactions ADD COLUMN isExcluded INTEGER NOT NULL DEFAULT 0");db.execSQL("ALTER TABLE transactions ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0");db.execSQL("ALTER TABLE transactions ADD COLUMN recurringGroupId TEXT NOT NULL DEFAULT ''");db.execSQL("ALTER TABLE transactions ADD COLUMN isTransfer INTEGER NOT NULL DEFAULT 0");db.execSQL("ALTER TABLE transactions ADD COLUMN isRefund INTEGER NOT NULL DEFAULT 0");db.execSQL("ALTER TABLE transactions ADD COLUMN tags TEXT NOT NULL DEFAULT ''");db.execSQL("ALTER TABLE transactions ADD COLUMN confidenceScore REAL NOT NULL DEFAULT 1.0");db.execSQL("ALTER TABLE transactions ADD COLUMN userEdited INTEGER NOT NULL DEFAULT 0");db.execSQL("ALTER TABLE transactions ADD COLUMN createdAt TEXT NOT NULL DEFAULT ''");db.execSQL("ALTER TABLE transactions ADD COLUMN updatedAt TEXT NOT NULL DEFAULT ''");db.execSQL("ALTER TABLE budgets ADD COLUMN name TEXT NOT NULL DEFAULT ''");db.execSQL("ALTER TABLE budgets ADD COLUMN period TEXT NOT NULL DEFAULT 'MONTHLY'");db.execSQL("ALTER TABLE budgets ADD COLUMN rollover INTEGER NOT NULL DEFAULT 0");db.execSQL("ALTER TABLE budgets ADD COLUMN alertAt REAL NOT NULL DEFAULT 0.8");db.execSQL("CREATE TABLE IF NOT EXISTS merchant_rules(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,pattern TEXT NOT NULL,displayName TEXT NOT NULL,category TEXT NOT NULL,applyToFuture INTEGER NOT NULL DEFAULT 1,applyToPast INTEGER NOT NULL DEFAULT 0,createdAt TEXT NOT NULL DEFAULT '')");db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_merchant_rules_pattern ON merchant_rules(pattern)")}}
    private val M2_3=object:Migration(2,3){override fun migrate(db:SupportSQLiteDatabase){db.execSQL("CREATE TABLE IF NOT EXISTS wallets(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,name TEXT NOT NULL,type TEXT NOT NULL DEFAULT 'BANK',currency TEXT NOT NULL DEFAULT 'QAR',balance REAL NOT NULL DEFAULT 0.0,color INTEGER NOT NULL DEFAULT 0,emoji TEXT NOT NULL DEFAULT '🏦',isHidden INTEGER NOT NULL DEFAULT 0,isArchived INTEGER NOT NULL DEFAULT 0,bankName TEXT NOT NULL DEFAULT '',accountLast4 TEXT NOT NULL DEFAULT '',createdAt TEXT NOT NULL DEFAULT '')");db.execSQL("CREATE TABLE IF NOT EXISTS savings_goals(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,name TEXT NOT NULL,targetAmount REAL NOT NULL,currentAmount REAL NOT NULL DEFAULT 0.0,emoji TEXT NOT NULL DEFAULT '🎯',color INTEGER NOT NULL DEFAULT 0,deadline TEXT,status TEXT NOT NULL DEFAULT 'ACTIVE',createdAt TEXT NOT NULL DEFAULT '')")}}
    @Provides @Singleton fun db(@ApplicationContext ctx:Context):ExpenseDatabase=Room.databaseBuilder(ctx,ExpenseDatabase::class.java,"expense_db").addMigrations(M1_2,M2_3).build()
    @Provides fun txDao(db:ExpenseDatabase)=db.transactionDao()
    @Provides fun walletDao(db:ExpenseDatabase)=db.walletDao()
    @Provides fun budgetDao(db:ExpenseDatabase)=db.budgetDao()
    @Provides fun goalDao(db:ExpenseDatabase)=db.savingsGoalDao()
    @Provides fun ruleDao(db:ExpenseDatabase)=db.merchantRuleDao()
}
