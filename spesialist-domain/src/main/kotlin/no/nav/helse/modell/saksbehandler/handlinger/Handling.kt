package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper

interface Handling {
    fun utførAv(saksbehandlerWrapper: SaksbehandlerWrapper)

    fun loggnavn(): String
}

interface Personhandling : Handling {
    val fødselsnummer: String
}
