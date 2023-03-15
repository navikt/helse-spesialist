package no.nav.helse.modell.kommando

import ToggleHelpers.disable
import ToggleHelpers.enable
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.Toggle
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingDao.Totrinnsvurdering
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


internal class TrengerTotrinnsvurderingCommandTest {

    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val FØDSELSNUMMER = "fnr"
    }

    private val warningDao = mockk<WarningDao>(relaxed = true)
    private val totrinnsvurderingMediator = mockk<TotrinnsvurderingMediator>(relaxed = true)
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val overstyringDao = mockk<OverstyringDao>(relaxed = true)
    private val varselRepository = mockk<VarselRepository>(relaxed = true)
    private val generasjonRepository = mockk<GenerasjonRepository>(relaxed = true)
    private lateinit var context: CommandContext

    private val command = TrengerTotrinnsvurderingCommand(
        fødselsnummer = FØDSELSNUMMER,
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        warningDao = warningDao,
        oppgaveMediator = oppgaveMediator,
        overstyringDao = overstyringDao,
        totrinnsvurderingMediator = totrinnsvurderingMediator,
        varselRepository = varselRepository,
        generasjonRepository = generasjonRepository
    )

    @BeforeEach
    fun setUp() {
        context = CommandContext(UUID.randomUUID())
    }

    @Test
    fun `Oppretter totrinnsvurdering dersom vedtaksperioden finnes i overstyringer_for_vedtaksperioder`() {
        Toggle.Totrinnsvurdering.enable()
        every { overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(any()) } returns listOf(OverstyringType.Dager)

        assertTrue(command.execute(context))

        verify(exactly = 1) { totrinnsvurderingMediator.opprett(any()) }
        Toggle.Totrinnsvurdering.disable()
    }

    @Test
    fun `Oppretter totrinssvurdering dersom vedtaksperioden har varsel for lovvalg og medlemskap, og ikke har hatt oppgave som har vært ferdigstilt før`() {
        Toggle.Totrinnsvurdering.enable()
        val testWarningVurderMedlemskap = "Vurder lovvalg og medlemskap"
        every {
            warningDao.finnAktiveWarningsMedMelding(
                VEDTAKSPERIODE_ID,
                testWarningVurderMedlemskap
            )
        } returns listOf(Warning(testWarningVurderMedlemskap, WarningKilde.Spleis, LocalDateTime.now()))
        every { oppgaveMediator.harFerdigstiltOppgave(VEDTAKSPERIODE_ID) } returns false

        assertTrue(command.execute(context))
        verify(exactly = 1) { totrinnsvurderingMediator.opprett(any()) }
        Toggle.Totrinnsvurdering.disable()
    }

    @Test
    fun `Hvis totrinnsvurdering har saksbehander skal oppgaven reserveres`() {
        Toggle.Totrinnsvurdering.enable()

        val saksbehander = UUID.randomUUID()

        every { overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(any()) } returns listOf(OverstyringType.Dager)
        every { totrinnsvurderingMediator.opprett(any()) } returns Totrinnsvurdering(
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            erRetur = false,
            saksbehandler = saksbehander,
            beslutter = null,
            utbetalingIdRef = null,
            opprettet = LocalDateTime.now(),
            oppdatert = null
        )

        assertTrue(command.execute(context))

        verify(exactly = 1) { totrinnsvurderingMediator.opprett(any()) }
        verify(exactly = 1) { oppgaveMediator.reserverOppgave(saksbehander, FØDSELSNUMMER) }

        Toggle.Totrinnsvurdering.disable()
    }

    @Test
    fun `Hvis totrinnsvurdering har beslutter skal totrinnsvurderingen markeres som retur`() {
        Toggle.Totrinnsvurdering.enable()
        val saksbehander = UUID.randomUUID()
        val beslutter = UUID.randomUUID()

        every { overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(any()) } returns listOf(OverstyringType.Dager)
        every { totrinnsvurderingMediator.opprett(any()) } returns Totrinnsvurdering(
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            erRetur = false,
            saksbehandler = saksbehander,
            beslutter = beslutter,
            utbetalingIdRef = null,
            opprettet = LocalDateTime.now(),
            oppdatert = null
        )

        assertTrue(command.execute(context))

        verify(exactly = 1) { totrinnsvurderingMediator.opprett(any()) }
        verify(exactly = 1) { oppgaveMediator.reserverOppgave(saksbehander, FØDSELSNUMMER) }
        verify(exactly = 1) { totrinnsvurderingMediator.settErRetur(VEDTAKSPERIODE_ID) }
        Toggle.Totrinnsvurdering.disable()
    }

    @Test
    fun `Oppretter ikke totrinnsvurdering om det ikke er overstyring eller varsel for lovvalg og medlemskap`() {
        assertTrue(command.execute(context))

        verify(exactly = 0) { totrinnsvurderingMediator.opprett(any()) }
    }

    @Test
    fun `Setter trengerTotrinnsvurdering dersom oppgaven har varsel vurder lovvalg og medlemskap`() {
        every { warningDao.finnAktiveWarningsMedMelding(any(), any()) } returns listOf(
            Warning(
                melding = "Vurder lovvalg og medlemskap",
                kilde = WarningKilde.Spesialist,
                opprettet = LocalDateTime.now(),
            )
        )

        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.alleUlagredeOppgaverTilTotrinnsvurdering() }
        verify(exactly = 1) { warningDao.leggTilWarning(VEDTAKSPERIODE_ID, any()) }
    }

    @Test
    fun `Setter trengerTotrinnsvurdering dersom oppgaven har blitt overstyrt`() {
        every { overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(any()) } returns listOf(OverstyringType.Dager)

        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.alleUlagredeOppgaverTilTotrinnsvurdering() }
        verify(exactly = 1) { warningDao.leggTilWarning(VEDTAKSPERIODE_ID, any()) }
    }

    @Test
    fun `Setter trengerTotrinnsvurdering for lovvalg og medlemskap dersom vedtaksperioden har hatt oppgave som ikke har vært ferdigstilt før`() {
        val testWarningVurderMedlemskap = "Vurder lovvalg og medlemskap"
        every {
            warningDao.finnAktiveWarningsMedMelding(
                VEDTAKSPERIODE_ID,
                testWarningVurderMedlemskap
            )
        } returns listOf(Warning(testWarningVurderMedlemskap, WarningKilde.Spleis, LocalDateTime.now()))
        every { oppgaveMediator.harFerdigstiltOppgave(VEDTAKSPERIODE_ID) } returns false

        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.alleUlagredeOppgaverTilTotrinnsvurdering() }
        verify(exactly = 1) { warningDao.leggTilWarning(VEDTAKSPERIODE_ID, any()) }
    }

    @Test
    fun `Setter ikke trengerTotrinnsvurdering for lovvalg og medlemskap dersom vedtaksperioden har hatt oppgave som har vært ferdigstilt før`() {
        val testWarningVurderMedlemskap = "Vurder lovvalg og medlemskap"
        every {
            warningDao.finnAktiveWarningsMedMelding(
                VEDTAKSPERIODE_ID,
                testWarningVurderMedlemskap
            )
        } returns listOf(Warning(testWarningVurderMedlemskap, WarningKilde.Spleis, LocalDateTime.now()))
        every { oppgaveMediator.harFerdigstiltOppgave(VEDTAKSPERIODE_ID) } returns true

        assertTrue(command.execute(context))
        verify(exactly = 0) { oppgaveMediator.alleUlagredeOppgaverTilTotrinnsvurdering() }
        verify(exactly = 0) { warningDao.leggTilWarning(VEDTAKSPERIODE_ID, any()) }
    }

    @Test
    fun `Setter ikke trengerTotrinnsvurdering dersom oppgaven ikke har aktive warnings med spesifikk melding`() {
        val testWarningVurderMedlemskap = "Vurder lovvalg og medlemskap"
        every {
            warningDao.finnAktiveWarningsMedMelding(
                VEDTAKSPERIODE_ID,
                testWarningVurderMedlemskap
            )
        } returns emptyList()

        assertTrue(command.execute(context))
        verify(exactly = 0) { oppgaveMediator.alleUlagredeOppgaverTilTotrinnsvurdering() }
        verify(exactly = 0) { warningDao.leggTilWarning(VEDTAKSPERIODE_ID, any()) }
    }

    @Test
    fun `Warningtekst blir riktig for ulike årsaker`() {
        assertEquals("Beslutteroppgave: Lovvalg og medlemskap", command.getWarningtekst(listOf(), true))
        assertEquals(
            "Beslutteroppgave: Overstyring av utbetalingsdager",
            command.getWarningtekst(listOf(OverstyringType.Dager), false)
        )
        assertEquals(
            "Beslutteroppgave: Overstyring av inntekt",
            command.getWarningtekst(listOf(OverstyringType.Inntekt), false)
        )
        assertEquals(
            "Beslutteroppgave: Overstyring av annet arbeidsforhold",
            command.getWarningtekst(listOf(OverstyringType.Arbeidsforhold), false)
        )
        assertEquals(
            "Beslutteroppgave: Lovvalg og medlemskap, Overstyring av utbetalingsdager, Overstyring av inntekt og Overstyring av annet arbeidsforhold",
            command.getWarningtekst(
                listOf(
                    OverstyringType.Dager,
                    OverstyringType.Inntekt,
                    OverstyringType.Arbeidsforhold
                ), true
            )
        )
    }
}
