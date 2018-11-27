package com.tuenti.acalvo.meusimtron

import android.app.IntentService
import android.content.Intent

class StatusService : IntentService("StatusService") {
    private var listener: SlackListener? = null

    override fun onHandleIntent(intent: Intent?) {
        if (listener == null || !listener!!.isAlive()) {
            val token = getString(R.string.token)
            val channel = getString(R.string.channel)
            val debugChannel = getString(R.string.debugChannel)
            listener = SlackListener(SlackInfo(token, channel, debugChannel))

            SlackService.instance.rtm(token, listener!!)
        }

    }
}
