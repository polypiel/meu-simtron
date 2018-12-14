package com.tuenti.acalvo.meusimtron

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast


class RestartReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("MEU-RESTART", "Service tried to stop")
        Toast.makeText(context, "MeuSimtron restarted", Toast.LENGTH_SHORT).show()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context?.startForegroundService(Intent(context, StatusService::class.java))
        } else {
            context?.startService(Intent(context, StatusService::class.java))
        }
    }
}