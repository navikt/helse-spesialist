package no.nav.helse.modell.kommando

import ToggleHelpers.disable
import ToggleHelpers.enable
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.mediator.Toggle
import no.nav.helse.modell.TotrinnsvurderingDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


internal class TrengerTotrinnsvurderingCommandTest {

    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
    }

    private val warningDao = mockk<WarningDao>(relaxed = true)
    private val totrinnsvurderingDao = mockk<TotrinnsvurderingDao>(relaxed = true)
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val overstyringDao = mockk<OverstyringDao>(relaxed = true)
    private val varselRepository = mockk<VarselRepository>(relaxed = true)
    private val generasjonRepository = mockk<GenerasjonRepository>(relaxed = true)
    private lateinit var context: CommandContext

    private val command = TrengerTotrinnsvurderingCommand(
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        warningDao = warningDao,
        oppgaveMediator = oppgaveMediator,
        overstyringDao = overstyringDao,
        totrinnsvurderingDao = totrinnsvurderingDao,
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

        verify(exactly = 1) { totrinnsvurderingDao.opprett(any()) }
        Toggle.Totrinnsvurdering.disable()
    }

    @Test
    fun `Oppretter ikke totrinnsvurdering om det ikke er overstyring`() {
        assertTrue(command.execute(context))

        verify(exactly = 0) { totrinnsvurderingDao.opprett(any()) }
    }

    @Test
    fun `Setter trengerTotrinnsvurdering dersom oppgaven har blitt overstyrt`() {
        every { overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(any()) } returns listOf(OverstyringType.Dager)

        assertTrue(command.execute(context))
        verify(exactly = 1) { oppgaveMediator.alleUlagredeOppgaverTilTotrinnsvurdering() }
        verify(exactly = 1) { warningDao.leggTilWarning(VEDTAKSPERIODE_ID, any()) }
    }

    @Test
    fun `Warningtekst blir riktig for ulike årsaker`() {
        assertEquals(
            "Beslutteroppgave: Overstyring av utbetalingsdager",
            command.getWarningtekst(listOf(OverstyringType.Dager))
        )
        assertEquals(
            "Beslutteroppgave: Overstyring av inntekt",
            command.getWarningtekst(listOf(OverstyringType.Inntekt))
        )
        assertEquals(
            "Beslutteroppgave: Overstyring av annet arbeidsforhold",
            command.getWarningtekst(listOf(OverstyringType.Arbeidsforhold))
        )
        assertEquals(
            "Beslutteroppgave: Overstyring av utbetalingsdager, Overstyring av inntekt og Overstyring av annet arbeidsforhold",
            command.getWarningtekst(
                listOf(
                    OverstyringType.Dager,
                    OverstyringType.Inntekt,
                    OverstyringType.Arbeidsforhold
                )
            )
        )
    }
}
