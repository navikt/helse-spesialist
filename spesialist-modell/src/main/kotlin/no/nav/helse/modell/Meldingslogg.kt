package no.nav.helse.modell

import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.melding.UtgåendeMelding

class Meldingslogg {
    private val logg: MutableList<UtgåendeMelding> = mutableListOf()

    fun nyMelding(melding: UtgåendeMelding) {
        logg.add(melding)
    }

    fun hendelser(): List<UtgåendeHendelse> = logg.filterIsInstance<UtgåendeHendelse>()

    fun behov(): List<Behov> = logg.filterIsInstance<Behov>()
}
