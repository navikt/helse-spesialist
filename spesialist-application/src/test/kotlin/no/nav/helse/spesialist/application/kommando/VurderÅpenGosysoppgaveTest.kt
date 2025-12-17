package no.nav.helse.spesialist.application.kommando

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.SessionContext
import no.nav.helse.db.ÅpneGosysOppgaverDao
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.meldinger.løsninger.ÅpneGosysOppgaverløsning
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.gosysoppgaver.VurderÅpenGosysoppgave
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel
import no.nav.helse.modell.person.vedtaksperiode.VarselStatusDto
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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
    private val sykefraværstilfelle =
        Sykefraværstilfelle(
            FNR,
            skjæringstidspunkt,
            listOf(behandlingAg1, behandlingAg2),
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

    private fun commandContext(behovsamler: MutableList<Behov>? = null) =
        CommandContext(UUID.randomUUID()).also { commandContext ->
            behovsamler?.let { commandContext.nyObserver(observer(behovsamler)) }
        }

    private fun observer(behovsamler: MutableList<Behov>) =
        object : CommandContextObserver {
            override fun behov(
                behov: Behov,
                commandContextId: UUID,
            ) {
                behovsamler.add(behov)
            }
        }

    @Test
    fun `Ber om åpne oppgaver i gosys`() =
        testMedSessionContext {
            val behov = mutableListOf<Behov>()
            val context = commandContext(behov)
            assertFalse(command().execute(context, it))
            assertEquals(listOf(Behov.ÅpneOppgaver(skjæringstidspunkt.minusYears(1))), behov.toList())
        }

    @Test
    fun `Mangler løsning ved resume`() =
        testMedSessionContext {
            assertFalse(command().resume(commandContext(), it))
            verify(exactly = 0) { åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(any()) }
        }

    @Test
    fun `Lagrer løsning ved resume`() =
        testMedSessionContext {
            val context = commandContext()
            context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 0, false))
            assertTrue(command().resume(context, it))
            verify(exactly = 1) { åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(any()) }
        }

    @Test
    fun `Lagrer ikke varsel ved ingen åpne oppgaver og deaktiverer eventuelt eksisterende varsel`() =
        testMedSessionContext {
            behandlingAg1.håndterNyttVarsel(LegacyVarsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), VEDTAKPERIODE_ID_AG_1))
            behandlingAg1.inspektør {
                assertEquals(1, varsler.size)
            }
            commandContext().let { commandContext ->
                commandContext.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 0, false))
                assertTrue(command().resume(commandContext, it))
            }
            verify(exactly = 1) { åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(any()) }
            behandlingAg1.inspektør {
                assertEquals(1, varsler.size)
                assertEquals("SB_EX_1", varsler.first().varselkode)
                assertEquals(VarselStatusDto.INAKTIV, varsler.first().status)
            }
            verify(exactly = 1) { oppgaveService.fjernGosysEgenskap(any()) }
        }

    @Test
    fun `Deaktiverer ikke varsel dersom oppgave er tildelt`() =
        testMedSessionContext {
            behandlingAg1.håndterNyttVarsel(LegacyVarsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), VEDTAKPERIODE_ID_AG_1))
            val context = commandContext()
            context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 0, false))
            assertTrue(command(harTildeltOppgave = true).resume(context, it))
            behandlingAg1.inspektør {
                assertEquals(1, varsler.size)
                assertEquals("SB_EX_1", varsler.first().varselkode)
                assertEquals(VarselStatusDto.AKTIV, varsler.first().status)
            }
        }

    @Test
    fun `Lagrer varsel ved åpne oppgaver, uavhengig om eventuell oppgave er tildelt eller ikke`() =
        testMedSessionContext {
            lagrerVarselVedÅpneOppgaver(harTildeltOppgave = false, commandContext(), it)
            verify(exactly = 1) { oppgaveService.leggTilGosysEgenskap(any()) }
            clearMocks(åpneGosysOppgaverDao)
            lagrerVarselVedÅpneOppgaver(harTildeltOppgave = true, commandContext(), it)
        }

    @Test
    fun `Lagrer varsel ved oppslag feilet`() =
        testMedSessionContext {
            val context = commandContext()
            context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, null, true))
            assertTrue(command().resume(context, it))
            verify(exactly = 1) { åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(any()) }
            behandlingAg1.inspektør {
                assertEquals(1, varsler.size)
                assertEquals("SB_EX_3", varsler.first().varselkode)
            }
        }

    @Test
    fun `Legger ikke til egenskap for gosys dersom det er andre varsler på perioden`() =
        testMedSessionContext {
            behandlingAg1.håndterNyttVarsel(LegacyVarsel(UUID.randomUUID(), "SB_EX_4", LocalDateTime.now(), VEDTAKPERIODE_ID_AG_1))
            val context = commandContext()
            context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 1, false))
            command().resume(context, it)
            verify(exactly = 0) { oppgaveService.leggTilGosysEgenskap(any()) }
        }

    @Test
    fun `Legger ikke til egenskap for gosys dersom det er andre varsler på andre overlappende perioder`() =
        testMedSessionContext {
            behandlingAg2.håndterNyttVarsel(LegacyVarsel(UUID.randomUUID(), "SB_EX_4", LocalDateTime.now(), VEDTAKPERIODE_ID_AG_2))
            val context = commandContext()
            context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 1, false))
            command().resume(context, it)
            verify(exactly = 0) { oppgaveService.leggTilGosysEgenskap(any()) }
        }

    private fun lagrerVarselVedÅpneOppgaver(
        harTildeltOppgave: Boolean,
        context: CommandContext,
        sessionContext: SessionContext,
    ) {
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 1, false))
        assertTrue(command(harTildeltOppgave).resume(context, sessionContext))
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
            yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
        )
}

internal fun LegacyBehandling.inspektør(block: BehandlingDto.() -> Unit) {
    this.toDto().block()
}
