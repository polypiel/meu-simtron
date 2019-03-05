package com.tuenti.acalvo.meusimtron

import android.app.Application

class AppManager: Application() {
    companion object {
        lateinit var INSTANCE: AppManager private set
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
    }

    fun onSimUpdated(icc: String, simInfo: SimInfo) {
        val updated = Directory.instance.update(icc, simInfo)
        if (updated) {
            doAsync {
                val token = getString(R.string.token)
                val channel = getString(R.string.debugChannel)
                // TODO add mobile info
                SlackManager.INSTANCE.send(token, channel, ":floppy_disk: Updated sim", listOf(SlackAttachment("", simInfo.toSlack(icc))))
            }.execute()
        }
    }

    private fun SimInfo.toSlack(icc: String): List<Pair<String, String>> = listOf(
            Pair("ICC", icc),
            Pair("MSISDN", msisdn),
            Pair("Provider", provider.toSlack()),
            Pair("Payment Model", paymentModel.toString())
    )
}
