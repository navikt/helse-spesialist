package no.nav.helse.spesialist.application

import no.nav.helse.db.TildelingDao
import no.nav.helse.db.TildelingDto
import no.nav.helse.spesialist.domain.Identitetsnummer

class DelegatingTildelingDao(
    private val oppgaveRepository: InMemoryOppgaveRepository,
    private val saksbehandlerRepository: InMemorySaksbehandlerRepository,
) : TildelingDao {
    override fun tildelingForPerson(fødselsnummer: String): TildelingDto? {
        val oppgave = oppgaveRepository.finnAktivForPerson(Identitetsnummer.fraString(fødselsnummer)) ?: return null
        val tildeltTil = oppgave.tildeltTil ?: return null
        val saksbehandler = saksbehandlerRepository.finn(tildeltTil) ?: error("Finner ikke saksbehandler")
        return TildelingDto(
            navn = saksbehandler.navn,
            epost = saksbehandler.epost,
            oid = saksbehandler.id.value,
        )
    }
}
