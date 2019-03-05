package com.tuenti.acalvo.meusimtron

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SubscriptionManager
import android.util.Log
import com.tuenti.acalvo.meusimtron.activities.getSims


class SmsReceiver: BroadcastReceiver() {
    companion object {
        const val LOG_TAG = "MEU-SMS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action?.equals("android.provider.Telephony.SMS_RECEIVED") == true) {
            val smsText = Telephony.Sms.Intents.getMessagesFromIntent(intent)[0]!!.messageBody
            val slot = intent.extras?.getInt("slot", -1) ?: -1
            Log.i(LOG_TAG, "Slot $slot: $smsText")

            val simDataText = if (slot != -1) {
                var simData = Directory.instance.getSimInfo(slot)
                if (simData == null) {
                    Log.i(LOG_TAG, "resync")
                    val sims = context.applicationContext.getSystemService(SubscriptionManager::class.java).getSims()
                    Directory.instance.sync(sims)
                    simData = Directory.instance.getSimInfo(slot)
                }
                simData?.toSlack() ?: "Unknown sim"
            } else {
                Log.w(LOG_TAG, "unknown slot")
                "??"
            }
            doAsync {
                val token = context.getString(R.string.token)
                val channel = context.getString(R.string.channel)
                SlackManager.INSTANCE.send(token, channel, simDataText, listOf(SlackAttachment(smsText)))
            }.execute()
        }
    }
}
