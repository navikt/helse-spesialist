package no.nav.helse.modell.kommando

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.overstyring.OverstyringType
import no.nav.helse.overstyring.OverstyrtVedtaksperiodeDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


internal class TrengerTotrinnsvurderingCommandTest {

    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
    }

    private val warningDao = mockk<WarningDao>(relaxed = true)
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val overstyrtVedtaksperiodeDao = mockk<OverstyrtVedtaksperiodeDao>(relaxed = true)
    private lateinit var context: CommandContext

    private val command = TrengerTotrinnsvurderingCommand(
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        warningDao = warningDao,
        oppgaveMediator = oppgaveMediator,
        overstyrtVedtaksperiodeDao = overstyrtVedtaksperiodeDao
    )

    @BeforeEach
    fun setUp() {
        context = CommandContext(UUID.randomUUID())
    }

    @Test
    fun `Setter trenger totrinnsvudering dersom oppgaven har blitt overstyrt`() {
        every { warningDao.finnAktiveWarningsMedMelding(any(), any()) } returns listOf(
            Warning(
                melding = "melding",
                kilde = WarningKilde.Spesialist,
                opprettet = LocalDateTime.now(),
            )
        )

        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.alleUlagredeOppgaverTilTotrinnsvurdering() }
        verify(exactly = 1) { warningDao.leggTilWarning(VEDTAKSPERIODE_ID, any()) }
    }

    @Test
    fun `Setter trenger totrinnsvudering dersom oppgaven ikke har blitt overstyrt`() {
        every { overstyrtVedtaksperiodeDao.hentVedtaksperiodeOverstyrtTyper(any()) } returns listOf(OverstyringType.Dager)

        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.alleUlagredeOppgaverTilTotrinnsvurdering() }
        verify(exactly = 1) { warningDao.leggTilWarning(VEDTAKSPERIODE_ID, any()) }
    }

    @Test
    fun `Setter trenger totrinnsvudering dersom oppgaven har aktive warnings med spesifikk melding`() {
        val testWarningVurderMedlemskap = "Vurder lovvalg og medlemskap"
        every {
            warningDao.finnAktiveWarningsMedMelding(
                VEDTAKSPERIODE_ID,
                testWarningVurderMedlemskap
            )
        } returns listOf(Warning(testWarningVurderMedlemskap, WarningKilde.Spleis, LocalDateTime.now()))

        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.alleUlagredeOppgaverTilTotrinnsvurdering() }
        verify(exactly = 1) { warningDao.leggTilWarning(VEDTAKSPERIODE_ID, any()) }
    }

    @Test
    fun `Setter trenger totrinnsvudering dersom oppgaven har ingen aktive warnings med spesifikk melding`() {
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