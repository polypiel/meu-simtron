package com.tuenti.acalvo.meusimtron

import android.app.IntentService
import android.content.Intent

class StatusService : IntentService("StatusService") {
    override fun onHandleIntent(intent: Intent?) {
        val token = getString(R.string.token)
        val channel = getString(R.string.channel)
        SlackService.instance.rtm(SlackInfo(token, channel))
    }
}
