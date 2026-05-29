package com.expensetracker.data.db
import androidx.room.*

@Entity(tableName="transactions",indices=[Index("dateTime"),Index("category"),Index("accountLast4"),Index("normalizedMerchant"),Index("isRecurring"),Index("smsHash"),Index("transactionType"),Index("walletId")])
data class TransactionEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val uuid:String="",val amount:Double,val currency:String="QAR",val merchant:String,val normalizedMerchant:String="",val category:String,val subcategory:String="",val transactionType:String="EXPENSE",val paymentMethod:String="DEBIT_CARD",val walletId:Long=0,val walletName:String="",val dateTime:String,val bankName:String="",val accountLast4:String="",val accountLabel:String="",val balance:Double?=null,val rawSms:String?=null,val isManual:Boolean=false,val isExcluded:Boolean=false,val isRecurring:Boolean=false,val recurringGroupId:String="",val isTransfer:Boolean=false,val isRefund:Boolean=false,val note:String="",val tags:String="",val confidenceScore:Float=1f,val userEdited:Boolean=false,val sender:String="",val smsHash:String?=null,val createdAt:String="",val updatedAt:String="")

@Entity(tableName="wallets")
data class WalletEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val name:String,val type:String="BANK",val currency:String="QAR",val balance:Double=0.0,val color:Long=0xFF1565C0,val emoji:String="🏦",val isHidden:Boolean=false,val isArchived:Boolean=false,val bankName:String="",val accountLast4:String="",val createdAt:String="")

@Entity(tableName="budgets")
data class BudgetEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val name:String="",val category:String,val monthlyLimit:Double,val period:String="MONTHLY",val month:Int,val year:Int,val rollover:Boolean=false,val alertAt:Float=0.8f)

@Entity(tableName="savings_goals")
data class SavingsGoalEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val name:String,val targetAmount:Double,val currentAmount:Double=0.0,val emoji:String="🎯",val color:Long=0xFF1565C0,val deadline:String?=null,val status:String="ACTIVE",val createdAt:String="")

@Entity(tableName="merchant_rules",indices=[Index("pattern",unique=true)])
data class MerchantRuleEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val pattern:String,val displayName:String,val category:String,val applyToFuture:Boolean=true,val applyToPast:Boolean=false,val createdAt:String="")
