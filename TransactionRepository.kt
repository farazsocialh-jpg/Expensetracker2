package com.expensetracker.data.repository
import com.expensetracker.data.db.*
import com.expensetracker.domain.model.*
import com.expensetracker.service.SmsParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
@Singleton class TransactionRepository @Inject constructor(private val txDao:TransactionDao,private val walletDao:WalletDao,private val budgetDao:BudgetDao,private val goalDao:SavingsGoalDao,private val ruleDao:MerchantRuleDao){
    private val fmt=DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private fun fmt(dt:LocalDateTime)=dt.format(fmt)
    private fun now()=LocalDateTime.now().format(fmt)
    fun getAll()=txDao.getAll().map{it.map{e->e.toDomain()}}
    fun getRecent(limit:Int=8)=txDao.getRecent(limit).map{it.map{e->e.toDomain()}}
    fun search(q:String)=txDao.search(q).map{it.map{e->e.toDomain()}}
    fun getByCategory(c:ExpenseCategory)=txDao.getByCategory(c.name).map{it.map{e->e.toDomain()}}
    fun getByCard(card:String)=txDao.getByCard(card).map{it.map{e->e.toDomain()}}
    fun getByWallet(id:Long)=txDao.getByWallet(id).map{it.map{e->e.toDomain()}}
    fun getRecurring()=txDao.getRecurring().map{it.map{e->e.toDomain()}}
    fun getDistinctCards()=txDao.getDistinctCards()
    fun getByDateRange(s:LocalDateTime,e:LocalDateTime)=txDao.getByDateRange(fmt(s),fmt(e)).map{it.map{x->x.toDomain()}}
    fun getWallets()=walletDao.getAll().map{it.map{e->e.toDomain()}}
    fun getGoals()=goalDao.getAll().map{it.map{e->e.toDomain()}}
    fun getMerchantRules()=ruleDao.getAll().map{it.map{e->e.toDomain()}}
    fun getBudgetsForMonth(month:Int,year:Int)=budgetDao.getForMonth(month,year).map{it.map{e->e.toDomain()}}
    suspend fun saveWallet(w:Wallet)=walletDao.insert(w.toEntity())
    suspend fun deleteWallet(id:Long)=walletDao.deleteById(id)
    suspend fun saveGoal(g:SavingsGoal)=goalDao.insert(g.toEntity())
    suspend fun deleteGoal(id:Long)=goalDao.deleteById(id)
    suspend fun deleteMerchantRule(r:MerchantRule)=ruleDao.delete(r.toEntity())
    suspend fun applyMerchantRule(merchantNormalized:String,displayName:String,category:ExpenseCategory,applyToPast:Boolean){ruleDao.insert(MerchantRuleEntity(pattern=merchantNormalized,displayName=displayName,category=category.name,applyToFuture=true,applyToPast=applyToPast,createdAt=now()));if(applyToPast)txDao.bulkUpdateCategory(merchantNormalized,category.name,now())}
    suspend fun saveBudget(b:Budget)=budgetDao.insert(b.toEntity())
    suspend fun deleteBudget(b:Budget)=budgetDao.delete(b.toEntity())
    suspend fun insert(t:Transaction)=txDao.insert(t.toEntity())
    suspend fun insertFromSms(parsed:SmsParser.ParsedTransaction):Long?{if(txDao.findBySmsHash(parsed.smsHash)!=null)return null;val entity=parsed.toEntity();val rule=ruleDao.findMatch(normalizeMerchant(parsed.merchant));val catStr:String=if(rule!=null&&rule.applyToFuture)rule.category else entity.category;return txDao.insert(entity.copy(category=catStr))}
    suspend fun update(t:Transaction)=txDao.update(t.toEntity())
    suspend fun delete(id:Long)=txDao.deleteById(id)
    suspend fun updateAccountLabel(card:String,label:String)=txDao.updateAccountLabel(card,label)
    suspend fun getDashboardStats(now:LocalDate=LocalDate.now(),monthStartDay:Int=1):DashboardStats{
        val sd=monthStartDay.coerceIn(1,28);val today=now
        val dayStart=today.atStartOfDay();val weekStart=today.minusDays(today.dayOfWeek.value.toLong()-1).atStartOfDay()
        val monthStart=if(today.dayOfMonth>=sd)today.withDayOfMonth(sd).atStartOfDay() else today.minusMonths(1).withDayOfMonth(sd).atStartOfDay()
        val end=today.plusDays(1).atStartOfDay()
        val daily=txDao.sumExpenses(fmt(dayStart),fmt(end))?:0.0;val weekly=txDao.sumExpenses(fmt(weekStart),fmt(end))?:0.0;val monthly=txDao.sumExpenses(fmt(monthStart),fmt(end))?:0.0;val income=txDao.sumIncome(fmt(monthStart),fmt(end))?:0.0
        val summaries=ExpenseCategory.values().mapNotNull{cat->val total=txDao.sumCategory(cat.name,fmt(monthStart),fmt(end))?:0.0;val count=txDao.countCategory(cat.name,fmt(monthStart),fmt(end));if(total<=0.0&&count==0)null else{val budget=budgetDao.getForCategory(cat.name,today.monthValue,today.year);CategorySummary(cat,total,count,budget?.monthlyLimit,(if(monthly>0)(total/monthly*100).toFloat() else 0f))}}.sortedByDescending{it.totalAmount}
        val daysElapsed=java.time.temporal.ChronoUnit.DAYS.between(monthStart.toLocalDate(),today).coerceAtLeast(1);val dailyAvg=monthly/daysElapsed;val projected=dailyAvg*30;val savingsRate=if(income>0)((income-monthly)/income*100).coerceAtLeast(0.0) else 0.0
        val sevenAgo=today.minusDays(6).atStartOfDay();val dailyRows=txDao.getDailyAmounts(fmt(sevenAgo),fmt(end));val weeklySpends=dailyRows.groupBy{it.dateTime.take(10)}.map{(date,rows)->DailySpend(date,rows.sumOf{it.amount})}
        val ba=summaries.filter{it.budgetLimit!=null}.map{if(it.totalAmount<=(it.budgetLimit?:1.0))1f else 0f}.average().let{if(it.isNaN())1.0 else it};val health=((savingsRate/30.0*50)+(ba*50)).toInt().coerceIn(0,100)
        val alerts=summaries.mapNotNull{s->val limit=s.budgetLimit?:return@mapNotNull null;when{s.totalAmount>=limit->SpendingAlert(AlertType.BUDGET_EXCEEDED,"${s.category.emoji} ${s.category.displayName} budget exceeded!",s.category);s.totalAmount>=limit*0.8->SpendingAlert(AlertType.BUDGET_WARNING,"${s.category.emoji} ${s.category.displayName} at ${((s.totalAmount/limit)*100).toInt()}%",s.category);else->null}}
        return DashboardStats(dailyTotal=daily,weeklyTotal=weekly,monthlyTotal=monthly,monthlyIncome=income,categorySummaries=summaries,dailyAverage=dailyAvg,projectedMonthly=projected,savingsRate=savingsRate,alerts=alerts,financialHealthScore=health,weeklySpends=weeklySpends)
    }
}
