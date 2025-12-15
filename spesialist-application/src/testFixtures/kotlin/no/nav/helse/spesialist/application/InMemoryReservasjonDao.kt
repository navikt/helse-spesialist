package no.nav.helse.spesialist.application

import no.nav.helse.db.Reservasjon
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.spesialist.domain.NAVIdent

class InMemoryReservasjonDao(
    private val saksbehandlerRepository: SaksbehandlerRepository,
) : ReservasjonDao {
    val data = mutableMapOf<String, Reservasjon>()

    override fun reserverPerson(
        saksbehandlersIdent: NAVIdent,
        fødselsnummer: String,
    ) {
        val reservertTil =
            saksbehandlerRepository.finn(saksbehandlersIdent)
                ?: error("Fant ikke saksbehandler i saksbehandlerRepository")
        data[fødselsnummer] = Reservasjon(reservertTil)
    }

    override fun hentReservasjonFor(fødselsnummer: String): Reservasjon? = data[fødselsnummer]
}
