package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.saksbehandler.Saksbehandler

interface Handling {
    fun utførAv(saksbehandler: Saksbehandler)
    fun loggnavn(): String
}

interface Overstyring: Handling {
    fun gjelderFødselsnummer(): String
}