package no.nav.helse.db

import java.time.LocalDate
import java.util.UUID

interface OppgaveDao {
    fun finnOppgaveIdUansettStatus(fødselsnummer: String): Long?

    fun finnGenerasjonId(oppgaveId: Long): UUID

    fun finnOppgaveId(fødselsnummer: String): Long?

    fun finnOppgaveId(utbetalingId: UUID): Long?

    fun finnVedtaksperiodeId(oppgaveId: Long): UUID

    fun invaliderOppgave(oppgaveId: Long)

    fun reserverNesteId(): Long

    fun finnSpleisBehandlingId(oppgaveId: Long): UUID

    fun finnAntallOppgaver(saksbehandlerOid: UUID): AntallOppgaverFraDatabase

    fun finnBehandledeOppgaver(
        behandletAvOid: UUID,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE,
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
    ): List<BehandletOppgaveFraDatabaseForVisning>

    fun finnEgenskaper(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<EgenskapForDatabase>?

    fun finnIdForAktivOppgave(vedtaksperiodeId: UUID): Long?

    fun finnFødselsnummer(oppgaveId: Long): String

    fun harFerdigstiltOppgave(vedtaksperiodeId: UUID): Boolean

    fun oppdaterPekerTilGodkjenningsbehov(
        godkjenningsbehovId: UUID,
        utbetalingId: UUID,
    )
}
