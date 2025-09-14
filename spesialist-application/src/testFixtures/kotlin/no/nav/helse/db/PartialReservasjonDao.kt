package no.nav.helse.db

import java.util.UUID

interface PartialReservasjonDao : ReservasjonDao {
    override fun reserverPerson(saksbehandlerOid: UUID, fødselsnummer: String) {
        error("Not implemented for this test")
    }

    override fun hentReservasjonFor(fødselsnummer: String): Reservasjon? {
        error("Not implemented for this test")
    }
}
