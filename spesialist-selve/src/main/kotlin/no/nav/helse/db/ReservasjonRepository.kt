package no.nav.helse.db

import java.util.UUID

interface ReservasjonRepository {
    fun reserverPerson(
        saksbehandlerOid: UUID,
        fødselsnummer: String,
    )
}
