package no.nav.helse.db

import no.nav.helse.modell.gosysoppgaver.OppgaveDataForAutomatisering
import no.nav.helse.modell.oppgave.Egenskap
import java.time.LocalDate
import java.util.UUID

interface OppgaveDao {
    fun finnOppgaveIdUansettStatus(fødselsnummer: String): Long

    fun finnGenerasjonId(oppgaveId: Long): UUID

    fun finnOppgaveId(fødselsnummer: String): Long?

    fun finnOppgaveId(utbetalingId: UUID): Long?

    fun finnVedtaksperiodeId(fødselsnummer: String): UUID

    fun finnVedtaksperiodeId(oppgaveId: Long): UUID

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
        filtreringer: Filtreringer = Filtreringer(emptyMap()),
    ): List<OppgaveFraDatabaseForVisning>

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

    data class Filtreringer(private val grupperteFiltrerteEgenskaper: Map<Egenskap.Kategori, List<EgenskapForDatabase>>) {
        val ukategoriserteEgenskaper = grupperteFiltrerteEgenskaper[Egenskap.Kategori.Ukategorisert]
        val oppgavetypeegenskaper = grupperteFiltrerteEgenskaper[Egenskap.Kategori.Oppgavetype]
        val periodetypeegenskaper = grupperteFiltrerteEgenskaper[Egenskap.Kategori.Periodetype]
        val mottakeregenskaper = grupperteFiltrerteEgenskaper[Egenskap.Kategori.Mottaker]
        val antallArbeidsforholdEgenskaper = grupperteFiltrerteEgenskaper[Egenskap.Kategori.Inntektskilde]
        val statusegenskaper = grupperteFiltrerteEgenskaper[Egenskap.Kategori.Status]
    }
}
