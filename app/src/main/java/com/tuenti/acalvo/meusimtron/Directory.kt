package com.tuenti.acalvo.meusimtron

import android.os.Build

enum class PaymentModel {
    PREPAY,
    CONTROL,
    POSTPAY;

    override fun toString(): String = name.toLowerCase().capitalize()
}

enum class Provider(val country: Country, val displayName: String) {
    MOVISTAR_AR(Country.AR, "Movistar Ar"),
    MOVISTAR_B2B_AR(Country.AR, "Movistar Ar B2B"),
    MOVISTAR_B2B_T3_AR(Country.AR, "Movistar B2B T3"),
    MOVISTAR_EC(Country.EC, "Movistar Ec"),
    O2_UK(Country.GB, "O2 UK"),
    VIVO_BR(Country.BR, "Vivo"),
    VIVO_BR_LEGACY(Country.BR, "Vivo Legacy");

    override fun toString(): String = "${country.unicode} $displayName"
    fun toSlack(): String = "${country.slack} $displayName"
}

enum class Country(val slack: String, val unicode: String, val prefix: String) {
    AR(":flag-ar:", "🇦🇷", "54"),
    BR(":flag-br:", "🇧🇷", "55"),
    EC(":flag-ec:", "🇪🇨", "593"),
    ES(":flag-es:","🇪🇸", "34"),
    GB(":flag-gb:", "🇬🇧", "44")
}

typealias Icc = String
data class Sim(val icc: Icc, val simInfo: SimInfo?) {
    constructor(icc: String, msisdn: String, provider: Provider, paymentModel: PaymentModel):
            this(icc, SimInfo(msisdn, provider, paymentModel))
    constructor(icc: String): this(icc, null)

    override fun toString() = simInfo?.toString() ?: "🇦🇶 $icc"

    fun toSlack(debug: Boolean = false): String {
        val str = simInfo?.toSlack() ?: "🇦🇶 Unknown sim with icc: $icc"
        return if (debug)
            "$str :iphone: ${Build.MANUFACTURER.capitalize()} ${Build.MODEL.capitalize()}"
        else
            str
    }

    fun hasProviderInfo(): Boolean = simInfo != null
    fun flagUnicode(): String = simInfo?.provider?.country?.unicode ?: "🇦🇶"
    fun msisdn(): String = simInfo?.prettyMsisdn() ?: icc
}

typealias Msisdn = String
data class SimInfo(
        val msisdn: Msisdn,
        val provider: Provider,
        val paymentModel: PaymentModel
) {
    override fun toString() =  "${provider.country.unicode} $msisdn - ${provider.displayName} $paymentModel"
    fun toSlack(): String = "${provider.country.slack} *${provider.country.prefix} $msisdn* ${provider.displayName} $paymentModel"
    fun tagLine(): String = "${provider.displayName} $paymentModel"
    fun prettyMsisdn(): String = "${provider.country.prefix} $msisdn"
}

class Directory private constructor() {
    private object Holder { val INSTANCE = Directory() }

    companion object {
        val instance: Directory by lazy { Holder.INSTANCE }
        private val directory: MutableMap<String, Sim> = mutableMapOf(
                 // Madrid
                "8954073144104702194" to Sim("8954073144104702194", "1165099125", Provider.MOVISTAR_AR, PaymentModel.PREPAY),
                "8954073144216962371" to Sim("8954073144216962371", "1149753602", Provider.MOVISTAR_AR, PaymentModel.PREPAY),
                "8954079144222272256" to Sim("8954079144222272256", "1156905551", Provider.MOVISTAR_B2B_AR, PaymentModel.CONTROL),
                "8954075144249486446" to Sim("8954075144249486446", "1151339576", Provider.MOVISTAR_B2B_AR, PaymentModel.CONTROL),
                // Argentina
                "8954079100845301831" to Sim("8954079100845301831", "2236155363", Provider.MOVISTAR_AR, PaymentModel.PREPAY),
                "8954078100329655471" to Sim("8954078100329655471", "2233055140", Provider.MOVISTAR_AR, PaymentModel.POSTPAY),
                "8954078144384519222" to Sim("8954078144384519222", "2236870308", Provider.MOVISTAR_B2B_AR, PaymentModel.CONTROL),
                "8954078100329655489" to Sim("8954078100329655489", "2234248531", Provider.MOVISTAR_AR, PaymentModel.POSTPAY),
                "8954078144384519214" to Sim("8954078144384519214", "2236865242", Provider.MOVISTAR_AR, PaymentModel.POSTPAY),
                // Ops & QA
                "8954079144256579428" to Sim("8954079144256579428", "1158057661", Provider.MOVISTAR_B2B_AR, PaymentModel.CONTROL),
                "8954079144256579188" to Sim("8954079144256579188", "1145268397", Provider.MOVISTAR_B2B_AR, PaymentModel.POSTPAY),
                "8954079144256579204" to Sim("8954079144256579204", "1128565594", Provider.MOVISTAR_B2B_AR, PaymentModel.POSTPAY),
                // B2B T3 Madrid
                "8954078100390199995" to Sim("8954078100390199995", "1137009085", Provider.MOVISTAR_B2B_T3_AR, PaymentModel.POSTPAY),
                "8954078100390199961" to Sim("8954078100390199961", "1135701285", Provider.MOVISTAR_B2B_T3_AR, PaymentModel.POSTPAY)
        )
    }

    private val slots: MutableMap<Int, String> = mutableMapOf()

    private fun addSim(slot: Int, icc: String) {
        val normalizedIcc =
                if (icc.length > 19) icc.substring(0..18)
                else icc
        slots[slot] = normalizedIcc
    }

    fun getSimInfo(slot: Int): Sim? {
        val icc = slots[slot] ?: return null
        return getSimInfo(icc)
    }

    fun getSimInfo(icc: String): Sim? {
        return directory.getOrDefault(icc, Sim(icc, null))
    }

    fun getAllSimInfo(): List<Sim> =
            slots.toList()
                    .sortedBy { (key, _) -> key }
                    .map { directory.getOrDefault(it.second, Sim(it.second)) }

    // TODO return bool updated
    fun sync(iccs: List<Pair<Int, String>>) {
        slots.clear()
        iccs.forEach {
            Directory.instance.addSim(it.first, it.second)
        }
    }

    fun update(icc: String, simInfo: SimInfo): Boolean {
        val oldSimInfo = directory[icc]?.simInfo
        directory[icc] = Sim(icc, simInfo)
        return simInfo != oldSimInfo
    }
}
