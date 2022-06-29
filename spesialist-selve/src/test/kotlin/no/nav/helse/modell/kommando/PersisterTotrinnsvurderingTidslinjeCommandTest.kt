package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.spesialist.api.oppgave.OppgaveDao
import no.nav.helse.spesialist.api.oppgave.OppgaveDao.NyesteVedtaksperiodeTotrinn
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus.Ferdigstilt
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import no.nav.helse.spesialist.api.overstyring.OverstyrtVedtaksperiodeDao
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PersisterTotrinnsvurderingTidslinjeCommandTest {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val VEDTAKSPERIODE_ID2 = UUID.randomUUID()
        private val VEDTAKSPERIODE_ID3 = UUID.randomUUID()
        private const val FNR = "12345678911"
        private const val ORGNR = "123456789"
        private val OVERSTYRTE_DAGER = listOf(
            OverstyringDagDto(
                dato = 1.januar,
                type = Dagtype.Sykedag,
                grad = 100
            )
        )
    }

    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val overstyrtVedtaksperiodeDao = mockk<OverstyrtVedtaksperiodeDao>(relaxed = true)
    private val automatiseringDao = mockk<AutomatiseringDao>(relaxed = true)

    private val context = CommandContext(UUID.randomUUID())

    @BeforeEach
    fun setup() {
        clearMocks(oppgaveDao, overstyrtVedtaksperiodeDao, automatiseringDao)
    }

    @Test
    fun `lagrer overstyrt vedtaksperiode hvis vi finner manuelt utbetalt vedtaksperiode som inneholder første overstyrt dag`() {
        every { oppgaveDao.finnNyesteVedtaksperiodeIdMedStatus(FNR, ORGNR, OVERSTYRTE_DAGER.first().dato, Ferdigstilt) }.returns(
            NyesteVedtaksperiodeTotrinn(VEDTAKSPERIODE_ID, LocalDate.now())
        )

        val command = PersisterTotrinnsvurderingTidslinjeCommand(
            FNR,
            ORGNR,
            OVERSTYRTE_DAGER,
            oppgaveDao,
            overstyrtVedtaksperiodeDao,
            automatiseringDao
        )
        command.execute(context)

        verify(exactly = 1) {
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(VEDTAKSPERIODE_ID, OverstyringType.Dager)
        }
    }

    @Test
    fun `lagrer overstyrt vedtaksperiode hvis vi finner vedtaksperiode til godkjenning som inneholder første overstyrt dag`() {
        every { oppgaveDao.finnNyesteVedtaksperiodeIdMedStatus(FNR, ORGNR, OVERSTYRTE_DAGER.first().dato, AvventerSaksbehandler) }.returns(
            NyesteVedtaksperiodeTotrinn(VEDTAKSPERIODE_ID, LocalDate.now())
        )
        every { oppgaveDao.finnNyesteVedtaksperiodeIdMedStatus(FNR, ORGNR, OVERSTYRTE_DAGER.first().dato, Ferdigstilt) }.returns(
            null
        )
        every { automatiseringDao.finnSisteAutomatiserteVedtaksperiodeId(FNR, ORGNR) }.returns(
            null
        )

        val command = PersisterTotrinnsvurderingTidslinjeCommand(
            FNR,
            ORGNR,
            OVERSTYRTE_DAGER,
            oppgaveDao,
            overstyrtVedtaksperiodeDao,
            automatiseringDao
        )
        command.execute(context)

        verify(exactly = 1) {
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(VEDTAKSPERIODE_ID, OverstyringType.Dager)
        }
    }

    @Test
    fun `lagrer overstyrt vedtaksperiode hvis vi finner automatisk utbetalt vedtaksperiode som inneholder første overstyrt dag`() {
        every { automatiseringDao.finnSisteAutomatiserteVedtaksperiodeId(FNR, ORGNR) }.returns(
            NyesteVedtaksperiodeTotrinn(VEDTAKSPERIODE_ID, LocalDate.now())
        )

        val command = PersisterTotrinnsvurderingTidslinjeCommand(
            FNR,
            ORGNR,
            OVERSTYRTE_DAGER,
            oppgaveDao,
            overstyrtVedtaksperiodeDao,
            automatiseringDao
        )
        command.execute(context)

        verify(exactly = 1) {
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(VEDTAKSPERIODE_ID, OverstyringType.Dager)
        }
    }

    @Test
    fun `lagrer ikke overstyrt vedtaksperiode hvis vi ikke finner utbetalt vedtaksperiode, automatisk utbetalt eller tilgodkjenning som inneholder første overstyrt dag`() {
        every { oppgaveDao.finnNyesteVedtaksperiodeIdMedStatus(FNR, ORGNR, OVERSTYRTE_DAGER.first().dato, AvventerSaksbehandler) }.returns(
            null
        )
        every { oppgaveDao.finnNyesteVedtaksperiodeIdMedStatus(FNR, ORGNR, OVERSTYRTE_DAGER.first().dato, Ferdigstilt) }.returns(
            null
        )
        every { automatiseringDao.finnSisteAutomatiserteVedtaksperiodeId(FNR, ORGNR) }.returns(
            null
        )

        val command = PersisterTotrinnsvurderingTidslinjeCommand(
            FNR,
            ORGNR,
            OVERSTYRTE_DAGER,
            oppgaveDao,
            overstyrtVedtaksperiodeDao,
            automatiseringDao)
        command.execute(context)

        verify(exactly = 0) {
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(any(), any())
        }
    }

    @Test
    fun `lagrer overstyring på den manuelle vedtaksperioden hvis den er nyest`() {
        every { oppgaveDao.finnNyesteVedtaksperiodeIdMedStatus(FNR, ORGNR, OVERSTYRTE_DAGER.first().dato, Ferdigstilt) }.returns(
            NyesteVedtaksperiodeTotrinn(VEDTAKSPERIODE_ID, LocalDate.now())
        )
        every { oppgaveDao.finnNyesteVedtaksperiodeIdMedStatus(FNR, ORGNR, OVERSTYRTE_DAGER.first().dato, AvventerSaksbehandler) }.returns(
            NyesteVedtaksperiodeTotrinn(VEDTAKSPERIODE_ID2, LocalDate.now())
        )
        every { automatiseringDao.finnSisteAutomatiserteVedtaksperiodeId(FNR, ORGNR) }.returns(
            NyesteVedtaksperiodeTotrinn(VEDTAKSPERIODE_ID3, LocalDate.now().minusDays(1))
        )

        val command = PersisterTotrinnsvurderingTidslinjeCommand(
            FNR,
            ORGNR,
            OVERSTYRTE_DAGER,
            oppgaveDao,
            overstyrtVedtaksperiodeDao,
            automatiseringDao
        )
        command.execute(context)

        verify(exactly = 1) {
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(VEDTAKSPERIODE_ID, OverstyringType.Dager)
        }
    }

    @Test
    fun `lagrer overstyring på den automatiske vedtaksperioden hvis den er nyest`() {
        every { oppgaveDao.finnNyesteVedtaksperiodeIdMedStatus(FNR, ORGNR, OVERSTYRTE_DAGER.first().dato, Ferdigstilt) }.returns(
            NyesteVedtaksperiodeTotrinn(VEDTAKSPERIODE_ID, LocalDate.now().minusDays(1))
        )
        every { oppgaveDao.finnNyesteVedtaksperiodeIdMedStatus(FNR, ORGNR, OVERSTYRTE_DAGER.first().dato, AvventerSaksbehandler) }.returns(
            NyesteVedtaksperiodeTotrinn(VEDTAKSPERIODE_ID2, LocalDate.now())
        )
        every { automatiseringDao.finnSisteAutomatiserteVedtaksperiodeId(FNR, ORGNR) }.returns(
            NyesteVedtaksperiodeTotrinn(VEDTAKSPERIODE_ID3, LocalDate.now())
        )

        val command = PersisterTotrinnsvurderingTidslinjeCommand(
            FNR,
            ORGNR,
            OVERSTYRTE_DAGER,
            oppgaveDao,
            overstyrtVedtaksperiodeDao,
            automatiseringDao
        )
        command.execute(context)

        verify(exactly = 1) {
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(VEDTAKSPERIODE_ID3, OverstyringType.Dager)
        }
    }

    @Test
    fun `lagrer overstyring for utbetalt periode fremfor perioden til godkjenning`() {
        every { oppgaveDao.finnNyesteVedtaksperiodeIdMedStatus(FNR, ORGNR, OVERSTYRTE_DAGER.first().dato, Ferdigstilt) }.returns(
            NyesteVedtaksperiodeTotrinn(VEDTAKSPERIODE_ID, LocalDate.now())
        )
        every { oppgaveDao.finnNyesteVedtaksperiodeIdMedStatus(FNR, ORGNR, OVERSTYRTE_DAGER.first().dato, AvventerSaksbehandler) }.returns(
            NyesteVedtaksperiodeTotrinn(VEDTAKSPERIODE_ID2, LocalDate.now())
        )

        val command = PersisterTotrinnsvurderingTidslinjeCommand(
            FNR,
            ORGNR,
            OVERSTYRTE_DAGER,
            oppgaveDao,
            overstyrtVedtaksperiodeDao,
            automatiseringDao
        )
        command.execute(context)

        verify(exactly = 1) {
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(VEDTAKSPERIODE_ID, OverstyringType.Dager)
        }
    }

    @Test
    fun `lagrer overstyring for automatisert periode fremfor perioden til godkjenning`() {
        every { oppgaveDao.finnNyesteVedtaksperiodeIdMedStatus(FNR, ORGNR, OVERSTYRTE_DAGER.first().dato, Ferdigstilt) }.returns(
            null
        )
        every { oppgaveDao.finnNyesteVedtaksperiodeIdMedStatus(FNR, ORGNR, OVERSTYRTE_DAGER.first().dato, AvventerSaksbehandler) }.returns(
            NyesteVedtaksperiodeTotrinn(VEDTAKSPERIODE_ID, LocalDate.now())
        )
        every { automatiseringDao.finnSisteAutomatiserteVedtaksperiodeId(FNR, ORGNR) }.returns(
            NyesteVedtaksperiodeTotrinn(VEDTAKSPERIODE_ID2, LocalDate.now())
        )

        val command = PersisterTotrinnsvurderingTidslinjeCommand(
            FNR,
            ORGNR,
            OVERSTYRTE_DAGER,
            oppgaveDao,
            overstyrtVedtaksperiodeDao,
            automatiseringDao
        )
        command.execute(context)

        verify(exactly = 1) {
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(VEDTAKSPERIODE_ID2, OverstyringType.Dager)
        }
    }

    @Test
    fun `Hopper ut tidlig hvis overstyrte dager er tom`() {
        val command = PersisterTotrinnsvurderingTidslinjeCommand(
            FNR,
            ORGNR,
            listOf(),
            oppgaveDao,
            overstyrtVedtaksperiodeDao,
            automatiseringDao)
        command.execute(context)

        verify(exactly = 0) {
            oppgaveDao.finnNyesteVedtaksperiodeIdMedStatus(any(), any(), any(), any())
        }
        verify(exactly = 0) {
            overstyrtVedtaksperiodeDao.lagreOverstyrtVedtaksperiode(any(), any())
        }
    }
}
