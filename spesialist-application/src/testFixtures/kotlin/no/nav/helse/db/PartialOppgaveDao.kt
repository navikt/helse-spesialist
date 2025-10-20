package no.nav.helse.db

import no.nav.helse.modell.gosysoppgaver.OppgaveDataForAutomatisering
import java.time.LocalDate
import java.util.UUID

interface PartialOppgaveDao : OppgaveDao {
    override fun finnOppgaveIdUansettStatus(fødselsnummer: String): Long {
        error("Not implemented for this test")
    }

    override fun finnGenerasjonId(oppgaveId: Long): UUID {
        error("Not implemented for this test")
    }

    override fun finnOppgaveId(fødselsnummer: String): Long? {
        error("Not implemented for this test")
    }

    override fun finnOppgaveId(utbetalingId: UUID): Long? {
        error("Not implemented for this test")
    }

    override fun finnVedtaksperiodeId(fødselsnummer: String): UUID {
        error("Not implemented for this test")
    }

    override fun finnVedtaksperiodeId(oppgaveId: Long): UUID {
        error("Not implemented for this test")
    }

    override fun finnHendelseId(id: Long): UUID {
        error("Not implemented for this test")
    }

    override fun invaliderOppgaveFor(fødselsnummer: String) {
        error("Not implemented for this test")
    }

    override fun invaliderOppgave(oppgaveId: Long) {
        error("Not implemented for this test")
    }

    override fun reserverNesteId(): Long {
        error("Not implemented for this test")
    }

    override fun venterPåSaksbehandler(oppgaveId: Long): Boolean {
        error("Not implemented for this test")
    }

    override fun finnSpleisBehandlingId(oppgaveId: Long): UUID {
        error("Not implemented for this test")
    }

    override fun oppgaveDataForAutomatisering(oppgaveId: Long): OppgaveDataForAutomatisering? {
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
        tom: LocalDate
    ): List<BehandletOppgaveFraDatabaseForVisning> {
        error("Not implemented for this test")
    }

    override fun finnEgenskaper(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID
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

    override fun oppdaterPekerTilGodkjenningsbehov(godkjenningsbehovId: UUID, utbetalingId: UUID) {
        error("Not implemented for this test")
    }
}
