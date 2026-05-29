package com.expensetracker.data.db
import androidx.room.*
@Database(entities=[TransactionEntity::class,WalletEntity::class,BudgetEntity::class,SavingsGoalEntity::class,MerchantRuleEntity::class],version=3,exportSchema=false)
abstract class ExpenseDatabase:RoomDatabase(){abstract fun transactionDao():TransactionDao;abstract fun walletDao():WalletDao;abstract fun budgetDao():BudgetDao;abstract fun savingsGoalDao():SavingsGoalDao;abstract fun merchantRuleDao():MerchantRuleDao}
