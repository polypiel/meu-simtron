package com.tuenti.acalvo.meusimtron

import android.app.IntentService
import android.content.Intent

class StatusService : IntentService("StatusService") {
    override fun onHandleIntent(intent: Intent?) {
        SlackService.instance.rtm()
    }
}
