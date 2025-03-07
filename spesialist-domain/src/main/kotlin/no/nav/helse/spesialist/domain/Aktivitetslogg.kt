package no.nav.helse.spesialist.domain

import java.time.LocalDateTime

class Aktivitetslogg(private val lokasjon: String, private var forelder: Aktivitetslogg? = null) {
    private val meldinger = mutableMapOf<List<String>, MutableList<Aktivitet>>()

    fun aktiviteter() = meldinger.mapValues { it.value.toList() }

    fun info(
        tekst: String,
        vararg kontekst: Pair<String, Any>,
    ) {
        val aktivitet = Aktivitet(tekst, kontekst.toMap())
        nyAktivitet(listOf(lokasjon), aktivitet)
    }

    private fun nyAktivitet(
        lokasjoner: List<String>,
        aktivitet: Aktivitet,
    ) {
        val forelder = this.forelder
        if (forelder == null) {
            meldinger.getOrPut(lokasjoner) { mutableListOf() }.add(aktivitet)
        } else {
            forelder.nyAktivitet(listOf(forelder.lokasjon) + lokasjoner, aktivitet)
        }
    }

    fun nyForelder(forelder: Aktivitetslogg) {
        this.forelder = forelder
    }
}

data class Aktivitet(val tekst: String, val kontekst: Map<String, Any>) {
    val tidspunkt = LocalDateTime.now()
}
