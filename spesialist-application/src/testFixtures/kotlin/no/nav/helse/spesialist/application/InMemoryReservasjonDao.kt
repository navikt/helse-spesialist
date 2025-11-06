package no.nav.helse.spesialist.application

import no.nav.helse.db.Reservasjon
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import java.util.UUID

class InMemoryReservasjonDao(private val saksbehandlerRepository: SaksbehandlerRepository) : ReservasjonDao {
    val data = mutableMapOf<String, Reservasjon>()

    override fun reserverPerson(saksbehandlerOid: UUID, fødselsnummer: String) {
        val reservertTil = saksbehandlerRepository.finn(SaksbehandlerOid(saksbehandlerOid))
            ?: error("Fant ikke saksbehandler i saksbehandlerRepository")
        data[fødselsnummer] = Reservasjon(reservertTil)
    }

    override fun hentReservasjonFor(fødselsnummer: String): Reservasjon? = data[fødselsnummer]
}
