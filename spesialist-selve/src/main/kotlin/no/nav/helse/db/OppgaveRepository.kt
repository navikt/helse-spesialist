package no.nav.helse.db

import no.nav.helse.modell.gosysoppgaver.OppgaveDataForAutomatisering
import no.nav.helse.modell.oppgave.Egenskap
import java.util.UUID

interface OppgaveRepository {
    fun finnOppgaveIdUansettStatus(fødselsnummer: String): Long

    fun finnUtbetalingId(oppgaveId: Long): UUID?

    fun finnGenerasjonId(oppgaveId: Long): UUID

    fun finnOppgave(id: Long): OppgaveFraDatabase?

    fun finnOppgaveId(fødselsnummer: String): Long?

    fun finnOppgaveId(utbetalingId: UUID): Long?

    fun finnVedtaksperiodeId(fødselsnummer: String): UUID

    fun finnVedtaksperiodeId(oppgaveId: Long): UUID

    fun harGyldigOppgave(utbetalingId: UUID): Boolean

    fun finnHendelseId(id: Long): UUID

    fun invaliderOppgaveFor(fødselsnummer: String)

    fun reserverNesteId(): Long

    fun venterPåSaksbehandler(oppgaveId: Long): Boolean

    fun finnSpleisBehandlingId(oppgaveId: Long): UUID

    fun oppgaveDataForAutomatisering(oppgaveId: Long): OppgaveDataForAutomatisering?

    fun finnOppgaverForVisning(
        ekskluderEgenskaper: List<String>,
        saksbehandlerOid: UUID,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE,
        sortering: List<OppgavesorteringForDatabase> = emptyList(),
        egneSakerPåVent: Boolean = false,
        egneSaker: Boolean = false,
        tildelt: Boolean? = null,
        grupperteFiltrerteEgenskaper: Map<Egenskap.Kategori, List<EgenskapForDatabase>>? = emptyMap(),
    ): List<OppgaveFraDatabaseForVisning>

    fun finnAntallOppgaver(saksbehandlerOid: UUID): AntallOppgaverFraDatabase

    fun finnBehandledeOppgaver(
        behandletAvOid: UUID,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE,
    ): List<BehandletOppgaveFraDatabaseForVisning>

    fun finnEgenskaper(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<EgenskapForDatabase>?

    fun finnIdForAktivOppgave(vedtaksperiodeId: UUID): Long?

    fun opprettOppgave(
        id: Long,
        commandContextId: UUID,
        egenskaper: List<EgenskapForDatabase>,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        kanAvvises: Boolean,
    ): Long

    fun finnFødselsnummer(oppgaveId: Long): String

    fun updateOppgave(
        oppgaveId: Long,
        oppgavestatus: String,
        ferdigstiltAv: String? = null,
        oid: UUID? = null,
        egenskaper: List<EgenskapForDatabase>,
    ): Int

    fun harFerdigstiltOppgave(vedtaksperiodeId: UUID): Boolean
}
