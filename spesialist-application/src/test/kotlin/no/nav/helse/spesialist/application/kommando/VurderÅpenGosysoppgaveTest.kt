package no.nav.helse.spesialist.application.kommando

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.meldinger.løsninger.ÅpneGosysOppgaverløsning
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.gosysoppgaver.VurderÅpenGosysoppgave
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.vedtaksperiode.Varselkode
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.*
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class VurderÅpenGosysoppgaveTest : ApplicationTest() {
    private companion object {
        private const val FNR = "12345678911"
        private val VEDTAKPERIODE_ID_AG_1 = UUID.randomUUID()
        private val VEDTAKPERIODE_ID_AG_2 = UUID.randomUUID()
    }

    private val behandlingAg1 = behandling(VEDTAKPERIODE_ID_AG_1)
    private val behandlingAg2 = behandling(VEDTAKPERIODE_ID_AG_2)
    private val skjæringstidspunkt = LocalDate.now().minusDays(17)
    private val oppgaveService = mockk<OppgaveService>(relaxed = true)

    private fun command(
        harTildeltOppgave: Boolean = false,
    ) = VurderÅpenGosysoppgave(
        vedtaksperiodeId = VEDTAKPERIODE_ID_AG_1,
        skjæringstidspunkt = skjæringstidspunkt,
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
                sti: List<Int>,
            ) {
                behovsamler.add(behov)
            }
        }

    private fun persisterteÅpneGosysOppgaver() = sessionContext.åpneGosysOppgaverDao.persisterteÅpneGosysOppgaver

    @Test
    fun `Ber om åpne oppgaver i gosys`() {
        val behov = mutableListOf<Behov>()
        val context = commandContext(behov)
        assertFalse(command().execute(context, sessionContext, outbox))
        assertEquals(listOf(Behov.ÅpneOppgaver(skjæringstidspunkt.minusYears(1))), behov.toList())
    }

    @Test
    fun `Mangler løsning ved resume`() {
        assertFalse(command().resume(commandContext(), sessionContext, outbox))
        assertEquals(0, persisterteÅpneGosysOppgaver().size)
    }

    @Test
    fun `Lagrer løsning ved resume`() {
        val context = commandContext()
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 0, false))
        assertTrue(command().resume(context, sessionContext, outbox))
        assertEquals(1, persisterteÅpneGosysOppgaver().size)
    }

    @Test
    fun `Lagrer ikke varsel ved ingen åpne oppgaver og deaktiverer eventuelt eksisterende varsel`() {
        sessionContext.varselRepository.lagre(
            Varsel.nytt(
                VarselId(UUID.randomUUID()),
                behandlingAg1.id,
                behandlingAg1.spleisBehandlingId,
                Varselkode.SB_EX_1.name,
                LocalDateTime.now(),
            ),
        )
        assertEquals(1, sessionContext.varselRepository.finnVarslerFor(behandlingUnikId = behandlingAg1.id).size)
        commandContext().let { commandContext ->
            commandContext.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 0, false))
            assertTrue(command().resume(commandContext, sessionContext, outbox))
        }
        assertEquals(1, persisterteÅpneGosysOppgaver().size)
        sessionContext.varselRepository.finnVarslerFor(behandlingUnikId = behandlingAg1.id).let { varsler ->
            assertEquals(1, varsler.size)
            assertEquals("SB_EX_1", varsler.first().kode)
            assertEquals(Varsel.Status.INAKTIV, varsler.first().status)
        }
        verify(exactly = 1) { oppgaveService.fjernGosysEgenskap(any()) }
    }

    @Test
    fun `Deaktiverer ikke varsel dersom oppgave er tildelt`() {
        sessionContext.varselRepository.lagre(
            Varsel.nytt(
                VarselId(UUID.randomUUID()),
                behandlingAg1.id,
                behandlingAg1.spleisBehandlingId,
                Varselkode.SB_EX_1.name,
                LocalDateTime.now(),
            ),
        )
        val context = commandContext()
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 0, false))
        assertTrue(command(harTildeltOppgave = true).resume(context, sessionContext, outbox))
        sessionContext.varselRepository.finnVarslerFor(behandlingUnikId = behandlingAg1.id).let { varsler ->
            assertEquals(1, varsler.size)
            assertEquals("SB_EX_1", varsler.first().kode)
            assertEquals(Varsel.Status.AKTIV, varsler.first().status)
        }
    }

    @Test
    fun `Lagrer varsel ved åpne oppgaver, uavhengig om eventuell oppgave er tildelt eller ikke`() {
        lagrerVarselVedÅpneOppgaver(harTildeltOppgave = false, commandContext())
        verify(exactly = 1) { oppgaveService.leggTilGosysEgenskap(any()) }
        lagrerVarselVedÅpneOppgaver(harTildeltOppgave = true, commandContext())
    }

    @Test
    fun `Lagrer varsel ved oppslag feilet`() {
        val context = commandContext()
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, null, true))
        assertTrue(command().resume(context, sessionContext, outbox))
        assertEquals(1, persisterteÅpneGosysOppgaver().size)
        sessionContext.varselRepository.finnVarslerFor(behandlingUnikId = behandlingAg1.id).let { varsler ->
            assertEquals(1, varsler.size)
            assertEquals("SB_EX_3", varsler.first().kode)
        }
    }

    @Test
    fun `Legger ikke til egenskap for gosys dersom det er andre varsler på perioden`() {
        sessionContext.varselRepository.lagre(
            Varsel.nytt(
                VarselId(UUID.randomUUID()),
                behandlingAg1.id,
                behandlingAg1.spleisBehandlingId,
                Varselkode.RV_IV_2.name,
                LocalDateTime.now(),
            ),
        )
        val context = commandContext()
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 1, false))
        command().resume(context, sessionContext, outbox)
        verify(exactly = 0) { oppgaveService.leggTilGosysEgenskap(any()) }
    }

    @Test
    fun `Legger ikke til egenskap for gosys dersom det er andre varsler på andre overlappende perioder`() {
        sessionContext.varselRepository.lagre(
            Varsel.nytt(
                VarselId(UUID.randomUUID()),
                behandlingAg2.id,
                behandlingAg2.spleisBehandlingId,
                Varselkode.SB_EX_4.name,
                LocalDateTime.now(),
            ),
        )
        val context = commandContext()
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 1, false))
        command().resume(context, sessionContext, outbox)
        verify(exactly = 0) { oppgaveService.leggTilGosysEgenskap(any()) }
    }

    private fun lagrerVarselVedÅpneOppgaver(
        harTildeltOppgave: Boolean,
        commandContext: CommandContext,
    ) {
        val forventetAntallFørDenneOppgaven = persisterteÅpneGosysOppgaver().size + 1
        commandContext.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), FNR, 1, false))
        assertTrue(command(harTildeltOppgave).resume(commandContext, sessionContext, outbox))
        assertEquals(forventetAntallFørDenneOppgaven, persisterteÅpneGosysOppgaver().size)
        sessionContext.varselRepository.finnVarslerFor(behandlingUnikId = behandlingAg1.id).let { varsler ->
            assertEquals(1, varsler.size)
            assertEquals("SB_EX_1", varsler.first().kode)
        }
    }

    private fun behandling(vedtaksperiodeId: UUID = UUID.randomUUID()): Behandling =
        Behandling
            .ny(
                spleisBehandlingId = SpleisBehandlingId(UUID.randomUUID()),
                vedtaksperiodeId = VedtaksperiodeId(vedtaksperiodeId),
                fom = 1 jan 2018,
                tom = 31 jan 2018,
                yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
            ).also { sessionContext.behandlingRepository.lagre(it) }
}
