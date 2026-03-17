package no.nav.helse.spesialist.application

import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.oppgave.Oppgave
import java.util.UUID

class DelegatingOppgaveDao(
    private val oppgaveRepository: InMemoryOppgaveRepository,
    private val behandlingRepository: InMemoryBehandlingRepository,
    private val vedtaksperiodeRepository: InMemoryVedtaksperiodeRepository,
) : OppgaveDao {
    override fun finnOppgaveId(fødselsnummer: String): Long? {
        val vedtaksperiodeIder =
            vedtaksperiodeRepository.alle().filter { it.identitetsnummer.value == fødselsnummer }.map { it.id.value }
        return oppgaveRepository
            .alle()
            .filter { it.vedtaksperiodeId in vedtaksperiodeIder }
            .firstOrNull { it.tilstand is Oppgave.AvventerSaksbehandler }
            ?.id
            ?.value
    }

    override fun finnSpleisBehandlingId(oppgaveId: Long): UUID =
        behandlingRepository
            .finn(BehandlingUnikId(oppgaveRepository.finn(oppgaveId)!!.behandlingId))!!
            .spleisBehandlingId!!
            .value

    override fun finnOppgaveIdUansettStatus(fødselsnummer: String): Long {
        val vedtaksperiodeIder =
            vedtaksperiodeRepository.alle().filter { it.identitetsnummer.value == fødselsnummer }.map { it.id.value }
        return oppgaveRepository
            .alle()
            .filter { it.vedtaksperiodeId in vedtaksperiodeIder }
            .maxOf { it.id.value }
    }

    override fun finnBehandlingId(oppgaveId: Long): UUID =
        behandlingRepository
            .alle()
            .find { it.spleisBehandlingId?.value == oppgaveRepository.finn(oppgaveId)!!.behandlingId }!!
            .id.value

    override fun finnOppgaveId(utbetalingId: UUID): Long? =
        oppgaveRepository
            .alle()
            .filter { it.utbetalingId == utbetalingId && it.tilstand !is Oppgave.Invalidert && it.tilstand !is Oppgave.Ferdigstilt }
            .maxByOrNull { it.id.value }
            ?.id
            ?.value

    override fun finnVedtaksperiodeId(oppgaveId: Long): UUID = oppgaveRepository.finn(oppgaveId)!!.vedtaksperiodeId

    override fun reserverNesteId(): Long = (oppgaveRepository.alle().maxOfOrNull { it.id.value } ?: 0L) + 1L

    override fun finnEgenskaper(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<EgenskapForDatabase>? =
        oppgaveRepository
            .alle()
            .filter { it.vedtaksperiodeId == vedtaksperiodeId && it.utbetalingId == utbetalingId }
            .maxByOrNull { it.opprettet }
            ?.egenskaper
            ?.map { EgenskapForDatabase.valueOf(it.name) }
            ?.toSet()

    override fun finnIdForAktivOppgave(vedtaksperiodeId: UUID): Long? =
        oppgaveRepository
            .alle()
            .filter { it.vedtaksperiodeId == vedtaksperiodeId && it.tilstand !is Oppgave.Ferdigstilt && it.tilstand !is Oppgave.Invalidert }
            .maxByOrNull { it.opprettet }
            ?.id
            ?.value

    override fun finnFødselsnummer(oppgaveId: Long): String {
        val vedtaksperiodeId = oppgaveRepository.finn(oppgaveId)!!.vedtaksperiodeId
        return vedtaksperiodeRepository
            .alle()
            .find { it.id.value == vedtaksperiodeId }!!
            .identitetsnummer.value
    }

    override fun harFerdigstiltOppgave(vedtaksperiodeId: UUID): Boolean =
        oppgaveRepository
            .alle()
            .any { it.vedtaksperiodeId == vedtaksperiodeId && it.tilstand is Oppgave.Ferdigstilt }
}
