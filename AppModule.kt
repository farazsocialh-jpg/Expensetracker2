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

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add new columns to transactions
            db.execSQL("ALTER TABLE transactions ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE transactions ADD COLUMN currency TEXT NOT NULL DEFAULT 'QAR'")
            db.execSQL("ALTER TABLE transactions ADD COLUMN normalizedMerchant TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE transactions ADD COLUMN merchantId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE transactions ADD COLUMN subcategory TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE transactions ADD COLUMN bankName TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE transactions ADD COLUMN accountLast4 TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE transactions ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'DEBIT_CARD'")
            db.execSQL("ALTER TABLE transactions ADD COLUMN transactionType TEXT NOT NULL DEFAULT 'DEBIT'")
            db.execSQL("ALTER TABLE transactions ADD COLUMN isExcluded INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE transactions ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE transactions ADD COLUMN recurringGroupId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE transactions ADD COLUMN isTransfer INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE transactions ADD COLUMN isRefund INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE transactions ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE transactions ADD COLUMN confidenceScore REAL NOT NULL DEFAULT 1.0")
            db.execSQL("ALTER TABLE transactions ADD COLUMN userEdited INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE transactions ADD COLUMN createdAt TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE transactions ADD COLUMN updatedAt TEXT NOT NULL DEFAULT ''")
            // Add budgets rollover column
            db.execSQL("ALTER TABLE budgets ADD COLUMN rollover INTEGER NOT NULL DEFAULT 0")
            // Create merchant_rules table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS merchant_rules (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    pattern TEXT NOT NULL,
                    displayName TEXT NOT NULL,
                    category TEXT NOT NULL,
                    subcategory TEXT NOT NULL DEFAULT '',
                    applyToFuture INTEGER NOT NULL DEFAULT 1,
                    applyToPast INTEGER NOT NULL DEFAULT 0,
                    confidenceScore REAL NOT NULL DEFAULT 1.0,
                    createdAt TEXT NOT NULL DEFAULT ''
                )
            """)
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_merchant_rules_pattern ON merchant_rules(pattern)")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): ExpenseDatabase =
        Room.databaseBuilder(ctx, ExpenseDatabase::class.java, "expense_db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides fun txDao(db: ExpenseDatabase): TransactionDao = db.transactionDao()
    @Provides fun budgetDao(db: ExpenseDatabase): BudgetDao = db.budgetDao()
    @Provides fun ruleDao(db: ExpenseDatabase): MerchantRuleDao = db.merchantRuleDao()
}
