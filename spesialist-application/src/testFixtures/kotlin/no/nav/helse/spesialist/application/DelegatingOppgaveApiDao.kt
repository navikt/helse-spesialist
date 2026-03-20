package no.nav.helse.spesialist.application

import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.OppgaveForPeriodevisningDto
import no.nav.helse.spesialist.domain.oppgave.Oppgave
import java.util.UUID

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
            .firstOrNull { it.tilstand is Oppgave.AvventerSaksbehandler }
            ?.id
            ?.value
    }

    override fun finnPeriodeoppgave(vedtaksperiodeId: UUID): OppgaveForPeriodevisningDto? =
        oppgaveRepository
            .alle()
            .firstOrNull { it.vedtaksperiodeId.value == vedtaksperiodeId && it.tilstand is Oppgave.AvventerSaksbehandler }
            ?.let { OppgaveForPeriodevisningDto(id = it.id.value.toString(), kanAvvises = it.kanAvvises) }
}
