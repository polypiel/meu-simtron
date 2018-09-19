package com.tuenti.acalvo.meusimtron

import android.telephony.SubscriptionManager
import android.util.Log

enum class Country(val slack: String, val unicode: String) {
    AR(":flag-ar:", "🇦🇷"),
    BR(":flag-br", "🇧🇷"),
    ES(":flag-es:","🇪🇸"),
    GB("flag-gb", "🇬🇧")
}

data class SimData(val icc: String, val providerInfo: ProviderInfo?) {
    constructor(icc: String, msisdn: String, provider: String, paymentModel: String, country: Country):
            this(icc, ProviderInfo(msisdn, provider, paymentModel, country))
    constructor(icc: String): this(icc, null)

    override fun toString() = providerInfo?.toString() ?: "🤷 $icc"
    fun toSlackStatus() = providerInfo?.toSlackStatus() ?: icc
    fun toSlackInfo() = providerInfo?.toSlackInfo() ?: icc
}
data class ProviderInfo(
        val msisdn: String,
        val provider: String,
        val paymentModel: String,
        val flag: Country
) {
    override fun toString() =  "${flag.unicode} $msisdn - $provider $paymentModel"
    fun toSlackStatus() = "${flag.slack} *$msisdn $provider $paymentModel*. Registered in network."
    fun toSlackInfo() = "${flag.slack} *$msisdn $provider $paymentModel*."
}

class Directory private constructor() {
    private object Holder { val INSTANCE = Directory() }

    companion object {
        val instance: Directory by lazy { Holder.INSTANCE }
        private val directory: Map<String, SimData> = mapOf(
                 // Madrid
                "8954073144104702194" to SimData("8954073144104702194", "541165099125", "Movistar Ar B2C", "Prepay", Country.AR),
                "8954079144222272256" to SimData("8954079144222272256", "541156905551", "Movistar Ar B2B", "Control", Country.AR),
                "8954073144216962371" to SimData("8954073144216962371", "541149753602", "Movistar Ar B2C", "Prepay", Country.AR),
                // Argentina
                "8954073144322987361" to SimData("8954073144322987361", "542236155363", "Movistar Ar B2C", "Prepay", Country.AR),
                "8954078100329655471" to SimData("8954078100329655471", "542233055140", "Movistar Ar B2C", "Control", Country.AR),
                "8954078100329655489" to SimData("8954078100329655489", "542234248531", "Movistar Ar B2C", "Postpay", Country.AR),
                "8954078144384519222" to SimData("8954078144384519222", "542236870308", "Movistar Ar B2B", "Control", Country.AR),
                "8954078144384519214" to SimData("8954078144384519214", "542236865242", "Movistar Ar B2C", "Postpay", Country.AR)
        )
    }

    private val slots: MutableMap<Int, String> = mutableMapOf()

    private fun addSim(slot: Int, icc: String) {
        slots[slot] = icc
    }

    fun getSimInfo(slot: Int): SimData? = directory[slots[slot]]

    fun getAllSimInfo(): List<SimData> =
            slots.toList()
                    .sortedBy { (key, _) -> key }
                    .map { directory.getOrDefault(it.second, SimData(it.second)) }

    fun sync(manager: SubscriptionManager) {
        slots.clear()
        val nSims = manager.activeSubscriptionInfoCount
        for (i in 0 until nSims) {
            val icc = manager.getActiveSubscriptionInfoForSimSlotIndex(i).iccId
            Log.d("SIM", "$icc found in slot $i")
            Directory.instance.addSim(i, icc)
        }
    }
}