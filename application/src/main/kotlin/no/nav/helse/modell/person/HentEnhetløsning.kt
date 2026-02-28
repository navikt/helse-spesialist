package no.nav.helse.modell.person

class HentEnhetløsning(
    private val enhetNr: String,
) {
    internal companion object {
        private val UTLANDSENHETER = setOf("0393", "2101")

        internal fun erEnhetUtland(enhet: String) = enhet in UTLANDSENHETER
    }

    fun enhet() = enhetNr.toInt()
}
