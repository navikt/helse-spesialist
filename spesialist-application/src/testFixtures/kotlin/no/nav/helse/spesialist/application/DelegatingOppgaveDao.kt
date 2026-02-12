package no.nav.helse.spesialist.application

import no.nav.helse.db.AntallOppgaverFraDatabase
import no.nav.helse.db.BehandletOppgaveFraDatabaseForVisning
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.domain.BehandlingUnikId
import java.time.LocalDate
import java.util.UUID

class DelegatingOppgaveDao(
    private val oppgaveRepository: InMemoryOppgaveRepository,
    private val behandlingRepository: InMemoryBehandlingRepository,
    private val vedtaksperiodeRepository: InMemoryVedtaksperiodeRepository
) : OppgaveDao {
    override fun finnOppgaveId(fødselsnummer: String): Long? {
        val vedtaksperiodeIder =
            vedtaksperiodeRepository.alle().filter { it.identitetsnummer.value == fødselsnummer }.map { it.id.value }
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

    override fun finnOppgaveIdUansettStatus(fødselsnummer: String): Long {
        val vedtaksperiodeIder =
            vedtaksperiodeRepository.alle().filter { it.identitetsnummer.value == fødselsnummer }.map { it.id.value }
        return oppgaveRepository.alle()
            .filter { it.vedtaksperiodeId in vedtaksperiodeIder }
            .maxOf { it.id }
    }

    override fun finnBehandlingId(oppgaveId: Long): UUID {
        error("Not implemented for this test")
    }

    override fun finnOppgaveId(utbetalingId: UUID): Long? {
        error("Not implemented for this test")
    }

    override fun finnVedtaksperiodeId(oppgaveId: Long): UUID {
        error("Not implemented for this test")
    }

    override fun invaliderOppgave(oppgaveId: Long) {
        error("Not implemented for this test")
    }

    override fun reserverNesteId(): Long {
        error("Not implemented for this test")
    }

    override fun finnAntallOppgaver(saksbehandlerOid: UUID): AntallOppgaverFraDatabase {
        error("Not implemented for this test")
    }

    override fun finnBehandledeOppgaver(
        behandletAvOid: UUID,
        offset: Int,
        limit: Int,
        fom: LocalDate,
        tom: LocalDate,
    ): List<BehandletOppgaveFraDatabaseForVisning> {
        error("Not implemented for this test")
    }

    override fun finnEgenskaper(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<EgenskapForDatabase>? {
        error("Not implemented for this test")
    }

    override fun finnIdForAktivOppgave(vedtaksperiodeId: UUID): Long? {
        error("Not implemented for this test")
    }

    override fun finnFødselsnummer(oppgaveId: Long): String {
        error("Not implemented for this test")
    }

    override fun harFerdigstiltOppgave(vedtaksperiodeId: UUID): Boolean {
        error("Not implemented for this test")
    }

    override fun oppdaterPekerTilGodkjenningsbehov(
        godkjenningsbehovId: UUID,
        utbetalingId: UUID,
    ) {
        error("Not implemented for this test")
    }
}
