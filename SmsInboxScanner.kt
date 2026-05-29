package com.expensetracker.service
import android.content.Context
import android.provider.Telephony
import com.expensetracker.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
data class RawSms(val body:String,val sender:String,val timestamp:LocalDateTime)
@Singleton class SmsInboxScanner @Inject constructor(@ApplicationContext private val ctx:Context){
    fun scanInbox(s:AppSettings,onProgress:(Int,Int)->Unit={_,_->}):List<RawSms>{
        val results=mutableListOf<RawSms>();val config=SmsParser.ParserConfig(s.currencySymbol,s.trustedSenders,s.debitKeywords,s.creditKeywords)
        val cursor=try{ctx.contentResolver.query(Telephony.Sms.Inbox.CONTENT_URI,arrayOf(Telephony.Sms.BODY,Telephony.Sms.ADDRESS,Telephony.Sms.DATE),null,null,"${Telephony.Sms.DATE} DESC")}catch(_:SecurityException){null}?:return results
        cursor.use{c->val total=c.count;var done=0;val bI=c.getColumnIndexOrThrow(Telephony.Sms.BODY);val aI=c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS);val dI=c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while(c.moveToNext()){done++;onProgress(done,total);val body=c.getString(bI)?:continue;val sender=c.getString(aI)?:"";val dt=LocalDateTime.ofInstant(Instant.ofEpochMilli(c.getLong(dI)),ZoneId.systemDefault());if(!SmsParser.isTrustedSender(sender,config))continue;if(!SmsParser.isDebitTransaction(body,config))continue;results.add(RawSms(body,sender,dt))}}
        return results
    }
}
