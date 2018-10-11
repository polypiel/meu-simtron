package com.tuenti.acalvo.meusimtron.activities

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.Snackbar
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.telephony.SubscriptionManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import com.tuenti.acalvo.meusimtron.*


class MainActivity : AppCompatActivity() {
    private var errorSnackbar: Snackbar? = null
    private var slackInfo: SlackInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val layout = findViewById<ConstraintLayout>(R.id.layout)
        errorSnackbar = Snackbar.make(layout, R.string.no_permissions_error, Snackbar.LENGTH_INDEFINITE)
        errorSnackbar?.view?.setBackgroundColor(ContextCompat.getColor(this, R.color.error_color_material_light))

        val token = getString(R.string.token)
        val channel = getString(R.string.channel)
        slackInfo = SlackInfo(token, channel)

        val ok = checkPermissions()
        if (ok) {
            initMeuSimtron()
        } else {
            errorSnackbar?.show()
        }

        createNotification()
        startService(Intent(this, StatusService::class.java))
    }

    private fun createNotification() {
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
        val mBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_ms)
                .setContentTitle("MeuSimtron")
                .setContentText("MeuSimtron")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(0, mBuilder.build())
        }
    }

    private fun initMeuSimtron() {
        errorSnackbar?.dismiss()
        syncSims()
    }

    private fun checkPermissions(): Boolean {
        val neededPermissions = mutableListOf<String>()

        if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.RECEIVE_SMS)
        }
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.READ_PHONE_STATE)
        }

        if (!neededPermissions.isEmpty()) {
            requestPermissions(neededPermissions.toTypedArray(), REQUEST_CODE)
        }
        return neededPermissions.isEmpty()
    }

    private fun syncSims() {
        val sims = applicationContext.getSystemService(SubscriptionManager::class.java).getSims()
        Directory.instance.sync(sims)
        val simsList = findViewById<ListView>(R.id.simsList)
        simsList.adapter = ArrayAdapter<Sim>(
                this,
                android.R.layout.simple_list_item_1,
                Directory.instance.getAllSimInfo())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            when (item.itemId) {
                R.id.action_refresh -> {
                    if (checkPermissions()) {
                        syncSims()
                        val layout = findViewById<ConstraintLayout>(R.id.layout)
                        Snackbar.make(layout, R.string.list_refreshed, Snackbar.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.action_add_sim -> {
                    val intent = Intent(this, SimActivity::class.java).apply {
                        //putExtra(EXTRA_MESSAGE, message)
                    }
                    startActivity(intent)
                    true
                }

                else -> {
                    // If we got here, the user's action was not recognized.
                    // Invoke the superclass to handle it.
                    super.onOptionsItemSelected(item)
                }
            }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE
                && grantResults.isNotEmpty()
                && grantResults.all { it == PackageManager.PERMISSION_GRANTED}) {
            initMeuSimtron()
        }
    }

    companion object {
        const val REQUEST_CODE = 1
        const val NOTIFICATION_CHANNEL = "meu-simtron-not"
    }
}

fun SubscriptionManager.getSims(): List<Pair<Int, String>> =
    (0..activeSubscriptionInfoCountMax)
            .map {
                val icc = getActiveSubscriptionInfoForSimSlotIndex(it)?.iccId
                if (icc != null) {
                    Log.d("SIM", "$icc found in slot $it")
                    Pair(it, icc)
                } else {
                    null
                }
            }.filterNotNull().toList()
