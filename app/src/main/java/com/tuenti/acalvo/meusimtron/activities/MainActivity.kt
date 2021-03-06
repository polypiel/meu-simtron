package com.tuenti.acalvo.meusimtron.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.telephony.SubscriptionManager
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.tuenti.acalvo.meusimtron.*
import kotlinx.android.synthetic.main.listrow.view.*


class MainActivity : AppCompatActivity() {
    private var errorSnackbar: Snackbar? = null
    private var slackInfo: SlackInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val layout = findViewById<ConstraintLayout>(R.id.layout)
        errorSnackbar = Snackbar.make(layout, R.string.no_permissions_error, Snackbar.LENGTH_INDEFINITE)
        errorSnackbar?.view?.setBackgroundColor(Color.parseColor("#ff7961"))

        val aboutText = findViewById<TextView>(R.id.aboutText)
        aboutText.text = String.format(getString(R.string.about), BuildConfig.VERSION_NAME)

        val token = getString(R.string.token)
        val channel = getString(R.string.channel)
        val debugChannel = getString(R.string.debugChannel)
        slackInfo = SlackInfo(token, channel, debugChannel)

        val ok = checkPermissions()
        if (ok) {
            initMeuSimtron()
        } else {
            errorSnackbar?.show()
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, StatusService::class.java))
        } else {
            startService(Intent(this, StatusService::class.java))
        }

        /* TODO
        if (!isMyServiceRunning(mSensorService.getClass())) {
            startService(mServiceIntent);
        }
         */
    }
/* TODO
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }

    override fun onDestroy() {
        stopService(mServiceIntent)
        Log.i("MAINACT", "onDestroy!")
        super.onDestroy()
    }
*/
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
        simsList.adapter = SimRowAdapter(this, Directory.instance.getAllSimInfo())
        simsList.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, SimActivity::class.java).apply {
                putExtra("sim", position)
                putExtra("icc", Directory.instance.getSimInfo(position)!!.icc)
            }
            startActivity(intent)
        }
        // TODO sync notification
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

fun SubscriptionManager.getSims(): List<Pair<Int, String>> = try {
    (0..activeSubscriptionInfoCountMax).mapNotNull {
        val icc = getActiveSubscriptionInfoForSimSlotIndex(it)?.iccId
        if (icc != null) {
            Log.d("MEU-SIM", "$icc found in slot $it")
            Pair(it, icc)
        } else {
            null
        }
    }.toList()
} catch (e: SecurityException) {
    emptyList()
}

class SimRowAdapter(context: Context, list: List<Sim>): ArrayAdapter<Sim>(context, 0, list) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val sim = getItem(position)!!
        val retView: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.listrow, parent, false)
        retView.flag.text = sim.flagUnicode()
        retView.headerLine.text = sim.msisdn()
        retView.tagLine.text = sim.simInfo?.tagLine() ?: "Tap to identify"
        return retView
    }
}
