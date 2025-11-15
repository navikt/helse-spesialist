package no.nav.helse.spesialist.application

import no.nav.helse.db.PartialOppgaveDao
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.domain.BehandlingUnikId
import java.util.UUID

class InMemoryOppgaveDao(
    private val oppgaveRepository: InMemoryOppgaveRepository,
    private val behandlingRepository: InMemoryBehandlingRepository,
    private val vedtaksperiodeRepository: InMemoryVedtaksperiodeRepository
) : PartialOppgaveDao {
    override fun finnOppgaveId(fødselsnummer: String): Long? {
        val vedtaksperiodeIder =
            vedtaksperiodeRepository.alle().filter { it.fødselsnummer == fødselsnummer }.map { it.id().value }
        return oppgaveRepository.alle()
            .filter { it.vedtaksperiodeId in vedtaksperiodeIder }
            .firstOrNull { it.tilstand is Oppgave.AvventerSaksbehandler }
            ?.id
    }

    override fun finnSpleisBehandlingId(oppgaveId: Long): UUID =
        behandlingRepository
            .finn(BehandlingUnikId(oppgaveRepository.finn(oppgaveId)!!.behandlingId))!!
            .spleisBehandlingId!!
            .value

    override fun venterPåSaksbehandler(oppgaveId: Long): Boolean =
        oppgaveRepository.finn(oppgaveId)!!.tilstand is Oppgave.AvventerSaksbehandler

    override fun finnOppgaveIdUansettStatus(fødselsnummer: String): Long {
        val vedtaksperiodeIder =
            vedtaksperiodeRepository.alle().filter { it.fødselsnummer == fødselsnummer }.map { it.id().value }
        return oppgaveRepository.alle()
            .filter { it.vedtaksperiodeId in vedtaksperiodeIder }
            .maxOf { it.id }
    }
}
