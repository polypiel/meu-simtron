package com.tuenti.acalvo.meusimtron

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import com.tuenti.acalvo.meusimtron.activities.MainActivity

class AppManager: Application() {
    private var pendingIntent: PendingIntent? = null

    companion object {
        lateinit var INSTANCE: AppManager private set
        const val NOTIF_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        pendingIntent = buildNotification()
    }

    fun onSimUpdated(icc: String, simInfo: SimInfo) {
        val updated = Directory.instance.update(icc, simInfo)
        if (updated) {
            doAsync {
                val token = getString(R.string.token)
                val channel = getString(R.string.debugChannel)
                val text = ":floppy_disk: Updated sim :iphone: ${Build.MANUFACTURER.capitalize()} ${Build.MODEL.capitalize()}"
                SlackManager.INSTANCE.send(token, channel, text, listOf(SlackAttachment(fields = simInfo.toSlack(icc))))
            }.execute()
        }
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(NOTIF_ID, getNotification())
    }

    fun getNotification(): Notification? {
        val sims = Directory.instance.getAllSimInfo()
        val text = if(sims.isEmpty()) {
            "No sims"
        } else {
            sims.joinToString(", ") { it.getMsisdnOrIcc() }
        }

        return NotificationCompat.Builder(this, MainActivity.NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(text)
                .setContentText("Touch to open")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
    }

    private fun SimInfo.toSlack(icc: String): List<Pair<String, String>> = listOf(
            Pair("ICC", icc),
            Pair("MSISDN", msisdn),
            Pair("Provider", provider.toSlack()),
            Pair("Payment Model", paymentModel.toString())
    )

    private fun buildNotification(): PendingIntent? {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Meu Simtron"
            val descriptionText = "Meu Simtron persistent notification"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(MainActivity.NOTIFICATION_CHANNEL, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val resultIntent = Intent(this, MainActivity::class.java)
        return TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(resultIntent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    private fun Sim.getMsisdnOrIcc() =
            simInfo?.msisdn ?: icc
}
