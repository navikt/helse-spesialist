package no.nav.helse.db

import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.Saksbehandler

interface ReservasjonDao {
    fun reserverPerson(
        saksbehandlersIdent: NAVIdent,
        fødselsnummer: String,
    )

    fun hentReservasjonFor(fødselsnummer: String): Reservasjon?
}

data class Reservasjon(
    val reservertTil: Saksbehandler,
)
