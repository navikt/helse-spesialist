package no.nav.helse.modell

import no.nav.helse.modell.hendelse.UtgåendeHendelse

class Hendelselogg {
    private val logg: MutableList<UtgåendeHendelse> = mutableListOf()

    fun nyHendelse(hendelse: UtgåendeHendelse) {
        logg.add(hendelse)
    }

    fun hendelser() = logg.toList()
}
