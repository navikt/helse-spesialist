package no.nav.helse.db

import no.nav.helse.spesialist.domain.Saksbehandler
import java.util.UUID

interface ReservasjonDao {
    fun reserverPerson(
        saksbehandlerOid: UUID,
        fødselsnummer: String,
    )

    fun hentReservasjonFor(fødselsnummer: String): Reservasjon?
}

data class Reservasjon(
    val reservertTil: Saksbehandler,
)
