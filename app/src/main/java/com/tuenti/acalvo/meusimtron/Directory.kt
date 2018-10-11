package com.tuenti.acalvo.meusimtron

enum class PaymentModel {
    PREPAY,
    CONTROL,
    POSTPAY;

    override fun toString(): String = name.capitalize()
}

enum class Provider(val country: Country, private val displayName: String) {
    MOVISTAR_AR(Country.AR, "Movistar Ar B2C"),
    MOVISTAR_B2B_AR(Country.AR, "Movistar Ar B2C"),;

    override fun toString(): String = displayName
}

enum class Country(val slack: String, val unicode: String) {
    AR(":flag-ar:", "🇦🇷"),
    BR(":flag-br", "🇧🇷"),
    ES(":flag-es:","🇪🇸"),
    GB("flag-gb", "🇬🇧")
}

typealias Icc = String
data class Sim(val icc: Icc, val simInfo: SimInfo?) {
    constructor(icc: String, msisdn: String, provider: Provider, paymentModel: PaymentModel):
            this(icc, SimInfo(msisdn, provider, paymentModel))
    constructor(icc: String): this(icc, null)

    override fun toString() = simInfo?.toString() ?: "🇦🇶 $icc"
    fun toSlackStatus() = simInfo?.toSlackStatus() ?: icc
    fun toSlackInfo() = simInfo?.toSlackInfo() ?: icc
    fun hasProviderInfo() = simInfo != null
}

typealias Msisdn = String
data class SimInfo(
        val msisdn: Msisdn,
        val provider: Provider,
        val paymentModel: PaymentModel
) {
    override fun toString() =  "${provider.country.unicode} $msisdn - $provider $paymentModel"
    fun toSlackStatus() = "${provider.country.slack} *$msisdn $provider $paymentModel*. Registered in network."
    fun toSlackInfo() = "${provider.country.slack} *$msisdn $provider $paymentModel*."
}

class Directory private constructor() {
    private object Holder { val INSTANCE = Directory() }

    companion object {
        val instance: Directory by lazy { Holder.INSTANCE }
        private val directory: Map<String, Sim> = mapOf(
                 // Madrid
                "8954073144104702194" to Sim("8954073144104702194", "541165099125", Provider.MOVISTAR_AR, PaymentModel.PREPAY),
                "8954073144216962371" to Sim("8954073144216962371", "541149753602", Provider.MOVISTAR_AR, PaymentModel.PREPAY),
                "8954079144222272256" to Sim("8954079144222272256", "541156905551", Provider.MOVISTAR_B2B_AR, PaymentModel.CONTROL),
                // Argentina
                "8954073144322987361" to Sim("8954073144322987361", "542236155363", Provider.MOVISTAR_AR, PaymentModel.PREPAY),
                "8954078100329655471" to Sim("8954078100329655471", "542233055140", Provider.MOVISTAR_AR, PaymentModel.CONTROL),
                "8954078144384519222" to Sim("8954078144384519222", "542236870308", Provider.MOVISTAR_B2B_AR, PaymentModel.CONTROL),
                "8954078100329655489" to Sim("8954078100329655489", "542234248531", Provider.MOVISTAR_AR, PaymentModel.POSTPAY),
                "8954078144384519214" to Sim("8954078144384519214", "542236865242", Provider.MOVISTAR_AR, PaymentModel.POSTPAY)
        )
    }

    private val slots: MutableMap<Int, String> = mutableMapOf()

    private fun addSim(slot: Int, icc: String) {
        val normalizedIcc =
                if (icc.length > 19) icc.substring(0..18)
                else icc
        slots[slot] = normalizedIcc
    }

    fun getSimInfo(slot: Int): Sim? = directory[slots[slot]]

    fun getAllSimInfo(): List<Sim> =
            slots.toList()
                    .sortedBy { (key, _) -> key }
                    .map { directory.getOrDefault(it.second, Sim(it.second)) }

    fun sync(iccs: List<Pair<Int, String>>) {
        slots.clear()
        iccs.forEach {
            Directory.instance.addSim(it.first, it.second)
        }
    }
}