package no.nav.helse.spesialist.application.modell

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.ÅpneGosysOppgaverDao
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.meldinger.løsninger.ÅpneGosysOppgaverløsning
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.gosysoppgaver.VurderÅpenGosysoppgave
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.person.vedtaksperiode.VarselStatusDto
import no.nav.helse.spesialist.application.jan
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class VurderÅpenGosysoppgaveTest {
    private companion object {
        private const val FNR = "12345678911"
        private val VEDTAKPERIODE_ID_AG_1 = UUID.randomUUID()
        private val VEDTAKPERIODE_ID_AG_2 = UUID.randomUUID()
    }

    private val behandlingAg1 = generasjon(VEDTAKPERIODE_ID_AG_1)
    private val behandlingAg2 = generasjon(VEDTAKPERIODE_ID_AG_2)
    private val skjæringstidspunkt = LocalDate.now().minusDays(17)
    private val sykefraværstilfelle = Sykefraværstilfelle(
        FNR,
        skjæringstidspunkt,
        listOf(behandlingAg1, behandlingAg2)
    )
    private val åpneGosysOppgaverDao = mockk<ÅpneGosysOppgaverDao>(relaxed = true)
    private val oppgaveService = mockk<OppgaveService>(relaxed = true)

    private fun command(
        harTildeltOppgave: Boolean = false,
    ) = VurderÅpenGosysoppgave(
        åpneGosysOppgaverDao = åpneGosysOppgaverDao,
        vedtaksperiodeId = VEDTAKPERIODE_ID_AG_1,
        sykefraværstilfelle = sykefraværstilfelle,
        harTildeltOppgave = harTildeltOppgave,
        oppgaveService = oppgaveService,
    )

    private lateinit var context: CommandContext

    private val observer =
        object : CommandContextObserver {
            val behov = mutableListOf<Behov>()

            override fun behov(behov: Behov, commandContextId: UUID) {
                this.behov.add(behov)
            }
        }

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        context.nyObserver(observer)
        clearMocks(åpneGosysOppgaverDao)
    }

    @Test
    fun `Ber om åpne oppgaver i gosys`() {
        assertFalse(command().execute(context))
        assertEquals(listOf(Behov.ÅpneOppgaver(skjæringstidspunkt.minusYears(1))), observer.behov.toList())
    }

    @Test
    fun `Mangler løsning ved resume`() {
        assertFalse(command().resume(context))
        verify(exactly = 0) { åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(any()) }
    }

    @Test
    fun `Lagrer løsning ved resume`() {
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 0, false))
        assertTrue(command().resume(context))
        verify(exactly = 1) { åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(any()) }
    }

    @Test
    fun `Lagrer ikke varsel ved ingen åpne oppgaver og deaktiverer eventuelt eksisterende varsel`() {
        behandlingAg1.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), VEDTAKPERIODE_ID_AG_1))
        behandlingAg1.inspektør {
            assertEquals(1, varsler.size)
        }
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 0, false))
        assertTrue(command().resume(context))
        verify(exactly = 1) { åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(any()) }
        behandlingAg1.inspektør {
            assertEquals(1, varsler.size)
            assertEquals("SB_EX_1", varsler.first().varselkode)
            assertEquals(VarselStatusDto.INAKTIV, varsler.first().status)
        }
        verify(exactly = 1) { oppgaveService.fjernGosysEgenskap(any()) }
    }

    @Test
    fun `Deaktiverer ikke varsel dersom oppgave er tildelt`() {
        behandlingAg1.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), VEDTAKPERIODE_ID_AG_1))
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 0, false))
        assertTrue(command(harTildeltOppgave = true).resume(context))
        behandlingAg1.inspektør {
            assertEquals(1, varsler.size)
            assertEquals("SB_EX_1", varsler.first().varselkode)
            assertEquals(VarselStatusDto.AKTIV, varsler.first().status)
        }
    }

    @Test
    fun `Lagrer varsel ved åpne oppgaver, uavhengig om eventuell oppgave er tildelt eller ikke`() {
        lagrerVarselVedÅpneOppgaver(false)
        verify(exactly = 1) { oppgaveService.leggTilGosysEgenskap(any()) }
        setup()
        lagrerVarselVedÅpneOppgaver(true)
    }

    @Test
    fun `Lagrer varsel ved oppslag feilet`() {
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, null, true))
        assertTrue(command().resume(context))
        verify(exactly = 1) { åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(any()) }
        behandlingAg1.inspektør {
            assertEquals(1, varsler.size)
            assertEquals("SB_EX_3", varsler.first().varselkode)
        }
    }

    @Test
    fun `Legger ikke til egenskap for gosys dersom det er andre varsler på perioden`() {
        behandlingAg1.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_4", LocalDateTime.now(), VEDTAKPERIODE_ID_AG_1))
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 1, false))
        command().resume(context)
        verify(exactly = 0) { oppgaveService.leggTilGosysEgenskap(any()) }
    }

    @Test
    fun `Legger ikke til egenskap for gosys dersom det er andre varsler på andre overlappende perioder`() {
        behandlingAg2.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_4", LocalDateTime.now(), VEDTAKPERIODE_ID_AG_2))
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 1, false))
        command().resume(context)
        verify(exactly = 0) { oppgaveService.leggTilGosysEgenskap(any()) }
    }

    private fun lagrerVarselVedÅpneOppgaver(harTildeltOppgave: Boolean) {
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 1, false))
        assertTrue(command(harTildeltOppgave).resume(context))
        verify(exactly = 1) { åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(any()) }
        behandlingAg1.inspektør {
            assertEquals(1, varsler.size)
            assertEquals("SB_EX_1", varsler.first().varselkode)
        }
    }

    private fun generasjon(vedtaksperiodeId: UUID = UUID.randomUUID()) =
        LegacyBehandling(
            id = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId,
            fom = 1 jan 2018,
            tom = 31 jan 2018,
            skjæringstidspunkt = 1 jan 2018,
        )
}

internal fun LegacyBehandling.inspektør(block: BehandlingDto.() -> Unit) {
    this.toDto().block()
}
