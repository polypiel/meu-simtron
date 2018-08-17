package com.tuenti.acalvo.meusimtron

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SubscriptionManager
import android.util.Log


class SmsReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action?.equals("android.provider.Telephony.SMS_RECEIVED") ?: false) {
            val smsText = Telephony.Sms.Intents.getMessagesFromIntent(intent)[0]!!.messageBody
            val slot = intent.extras?.getInt("slot", -1) ?: -1
            Log.i("SMS", "Slot $slot: $smsText")

            val simDataText = if (slot != -1) {
                var simData = Directory.instance.getSimInfo(slot)
                if (simData == null) {
                    Log.i("SMS", "resync")
                    Directory.instance.sync(context.applicationContext.getSystemService(SubscriptionManager::class.java))
                    simData = Directory.instance.getSimInfo(slot)
                }
                simData?.toSlackInfo() ?: "?"
            } else {
                Log.w("SMS", "unknown slot")
                "??"
            }
            doAsync {
                val token = context.getString(R.string.token)
                val channel = context.getString(R.string.channel)
                SlackService.instance.send(SlackInfo(token, channel), simDataText, listOf(SlackAttachment(smsText)))
            }.execute()
        }
    }
}
