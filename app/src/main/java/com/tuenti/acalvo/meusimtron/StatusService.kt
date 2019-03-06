package com.tuenti.acalvo.meusimtron

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class StatusService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(AppManager.NOTIF_ID, AppManager.INSTANCE.getNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("MEU-SLACK", "Status service started")
        start()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("MEU-SLACK", "ondestroy!")
        val broadcastIntent = Intent(this, RestartReceiver::class.java)
        sendBroadcast(broadcastIntent)
        //stoptimertask()
    }

    private var listener: SlackListener? = null

    private fun start() {
        if (listener == null || !listener!!.isAlive()) {
            val token = getString(R.string.token)
            val channel = getString(R.string.channel)
            val debugChannel = getString(R.string.debugChannel)
            listener = SlackListener(SlackInfo(token, channel, debugChannel))

            doAsync {
                SlackManager.INSTANCE.rtm(token, listener!!)
            }.execute()
        }
    }
}
