package no.nav.helse.spesialist.domain

class Aktivitetslogg(private val lokasjon: String, private var forelder: Aktivitetslogg? = null) {
    private val meldinger = mutableListOf<Aktivitet>()

    fun meldinger() = meldinger.toList()

    fun info(
        tekst: String,
        vararg kontekst: Pair<String, Any>,
    ) {
        val aktivitet = Aktivitet(tekst, kontekst.toMap())
        nyAktivitet(aktivitet)
    }

    private fun nyAktivitet(aktivitet: Aktivitet) {
        aktivitet.nyttNivå(lokasjon)
        meldinger.add(aktivitet)
        forelder?.nyAktivitet(aktivitet)
    }

    fun nyForelder(forelder: Aktivitetslogg) {
        this.forelder = forelder
    }
}

data class Aktivitet(val tekst: String, val kontekst: Map<String, Any>) {
    private val lokasjon = mutableListOf<String>()

    fun lokasjon() = lokasjon.toList()

    fun nyttNivå(hvor: String) {
        lokasjon.addFirst(hvor)
    }
}
