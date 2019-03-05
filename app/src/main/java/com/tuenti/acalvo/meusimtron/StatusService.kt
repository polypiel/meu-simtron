package com.tuenti.acalvo.meusimtron

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.tuenti.acalvo.meusimtron.activities.MainActivity
import com.tuenti.acalvo.meusimtron.activities.MainActivity.Companion.NOTIFICATION_CHANNEL

// TODO update notification
class StatusService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    private var notificationBuilder: NotificationCompat.Builder? = null

    override fun onCreate() {
        super.onCreate()
        notificationBuilder = createNotification()
        startForeground(1, notificationBuilder!!.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("MEU-SLACK", "Status service started")
        start()
        return START_STICKY
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

    private fun createNotification(): NotificationCompat.Builder {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Meu Simtron"
            val descriptionText = "Meu Simtron persistent notification"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_CHANNEL, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val resultIntent = Intent(this, MainActivity::class.java)
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(resultIntent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }


        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_ms)
                .setContentTitle(getNotificationText())
                .setContentText("Touch to open")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(resultPendingIntent)
                .setOngoing(true)
    }

    private fun getNotificationText(): String  {
        val sims = Directory.instance.getAllSimInfo()
        return if(sims.isEmpty()) {
            "No sims"
        } else{
            sims.joinToString(", ") { it.getMsisdnOrIcc() }
        }
    }
}

fun Sim.getMsisdnOrIcc() =
        simInfo?.msisdn ?: icc
