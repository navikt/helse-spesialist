package no.nav.helse.spesialist.application

import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.spesialist.domain.oppgave.Oppgave

class DelegatingOppgaveApiDao(
    private val oppgaveRepository: InMemoryOppgaveRepository,
    private val vedtaksperiodeRepository: InMemoryVedtaksperiodeRepository,
) : OppgaveApiDao {
    override fun finnOppgaveId(fødselsnummer: String): Long? {
        val vedtaksperiodeIder =
            vedtaksperiodeRepository.alle().filter { it.identitetsnummer.value == fødselsnummer }.map { it.id.value }
        return oppgaveRepository
            .alle()
            .filter { it.vedtaksperiodeId.value in vedtaksperiodeIder }
            .firstOrNull { it.tilstand == Oppgave.Tilstand.AvventerSaksbehandler }
            ?.id
            ?.value
    }
}
