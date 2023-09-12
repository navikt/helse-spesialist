package no.nav.helse.spesialist.api.modell.saksbehandling.hendelser

import no.nav.helse.spesialist.api.modell.Saksbehandler

interface Handling {
    fun utførAv(saksbehandler: Saksbehandler)
}

interface Overstyring: Handling {
    fun gjelderFødselsnummer(): String
}