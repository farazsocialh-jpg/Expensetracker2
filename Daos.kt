package com.expensetracker.data.db
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isExcluded=0 ORDER BY dateTime DESC") fun getAll():Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE isExcluded=0 AND dateTime>=:s AND dateTime<=:e ORDER BY dateTime DESC") fun getByDateRange(s:String,e:String):Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE isExcluded=0 AND category=:cat ORDER BY dateTime DESC") fun getByCategory(cat:String):Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE isExcluded=0 AND accountLast4=:card ORDER BY dateTime DESC") fun getByCard(card:String):Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE isExcluded=0 AND walletId=:wid ORDER BY dateTime DESC") fun getByWallet(wid:Long):Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE isExcluded=0 AND isRecurring=1 ORDER BY dateTime DESC") fun getRecurring():Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE isExcluded=0 AND (merchant LIKE '%'||:q||'%' OR normalizedMerchant LIKE '%'||:q||'%' OR note LIKE '%'||:q||'%' OR tags LIKE '%'||:q||'%') ORDER BY dateTime DESC") fun search(q:String):Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE isExcluded=0 ORDER BY dateTime DESC LIMIT :limit") fun getRecent(limit:Int):Flow<List<TransactionEntity>>
    @Query("SELECT DISTINCT accountLast4 FROM transactions WHERE accountLast4!='' ORDER BY accountLast4") fun getDistinctCards():Flow<List<String>>
    @Query("SELECT SUM(amount) FROM transactions WHERE isExcluded=0 AND transactionType NOT IN ('INCOME','TRANSFER') AND dateTime>=:s AND dateTime<=:e") suspend fun sumExpenses(s:String,e:String):Double?
    @Query("SELECT SUM(amount) FROM transactions WHERE isExcluded=0 AND transactionType='INCOME' AND dateTime>=:s AND dateTime<=:e") suspend fun sumIncome(s:String,e:String):Double?
    @Query("SELECT SUM(amount) FROM transactions WHERE isExcluded=0 AND transactionType NOT IN ('INCOME','TRANSFER') AND category=:cat AND dateTime>=:s AND dateTime<=:e") suspend fun sumCategory(cat:String,s:String,e:String):Double?
    @Query("SELECT COUNT(*) FROM transactions WHERE isExcluded=0 AND category=:cat AND dateTime>=:s AND dateTime<=:e") suspend fun countCategory(cat:String,s:String,e:String):Int
    @Query("SELECT dateTime, amount FROM transactions WHERE isExcluded=0 AND transactionType NOT IN ('INCOME','TRANSFER') AND dateTime>=:s AND dateTime<=:e ORDER BY dateTime ASC") suspend fun getDailyAmounts(s:String,e:String):List<DateAmountRow>
    @Query("SELECT * FROM transactions WHERE smsHash=:hash LIMIT 1") suspend fun findBySmsHash(hash:String):TransactionEntity?
    @Query("UPDATE transactions SET accountLabel=:label WHERE accountLast4=:card") suspend fun updateAccountLabel(card:String,label:String)
    @Query("UPDATE transactions SET category=:cat,userEdited=1,updatedAt=:now WHERE normalizedMerchant=:merchant AND userEdited=0") suspend fun bulkUpdateCategory(merchant:String,cat:String,now:String)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insert(t:TransactionEntity):Long
    @Update suspend fun update(t:TransactionEntity)
    @Query("DELETE FROM transactions WHERE id=:id") suspend fun deleteById(id:Long)
}
data class DateAmountRow(@ColumnInfo(name="dateTime") val dateTime:String, @ColumnInfo(name="amount") val amount:Double)

@Dao interface WalletDao {
    @Query("SELECT * FROM wallets WHERE isArchived=0 ORDER BY id ASC") fun getAll():Flow<List<WalletEntity>>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insert(w:WalletEntity):Long
    @Update suspend fun update(w:WalletEntity)
    @Query("DELETE FROM wallets WHERE id=:id") suspend fun deleteById(id:Long)
}
@Dao interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month=:month AND year=:year") fun getForMonth(month:Int,year:Int):Flow<List<BudgetEntity>>
    @Query("SELECT * FROM budgets WHERE category=:cat AND month=:month AND year=:year LIMIT 1") suspend fun getForCategory(cat:String,month:Int,year:Int):BudgetEntity?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insert(b:BudgetEntity):Long
    @Update suspend fun update(b:BudgetEntity)
    @Delete suspend fun delete(b:BudgetEntity)
}
@Dao interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals WHERE status!='ARCHIVED' ORDER BY createdAt DESC") fun getAll():Flow<List<SavingsGoalEntity>>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insert(g:SavingsGoalEntity):Long
    @Update suspend fun update(g:SavingsGoalEntity)
    @Query("DELETE FROM savings_goals WHERE id=:id") suspend fun deleteById(id:Long)
}
@Dao interface MerchantRuleDao {
    @Query("SELECT * FROM merchant_rules ORDER BY id DESC") fun getAll():Flow<List<MerchantRuleEntity>>
    @Query("SELECT * FROM merchant_rules WHERE :merchant LIKE '%'||pattern||'%' OR pattern LIKE '%'||:merchant||'%' LIMIT 1") suspend fun findMatch(merchant:String):MerchantRuleEntity?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insert(r:MerchantRuleEntity):Long
    @Delete suspend fun delete(r:MerchantRuleEntity)
}
