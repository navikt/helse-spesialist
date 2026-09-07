package no.nav.helse.spesialist.application.kommando

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.meldinger.løsninger.ÅpneGosysOppgaverløsning
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.gosysoppgaver.VurderÅpenGosysoppgave
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.spesialist.domain.Fødselsnummer
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class VurderÅpenGosysoppgaveTest : ApplicationTest() {
    private val skjæringstidspunkt = godkjenningsbehovData.skjæringstidspunkt

    private val oppgaveService = mockk<OppgaveService>(relaxed = true)

    private fun command(
        harTildeltOppgave: Boolean = false,
        vedtaksperiodeId: UUID = vedtaksperiode1.id.value,
    ) = VurderÅpenGosysoppgave(
        vedtaksperiodeId = vedtaksperiodeId,
        harTildeltOppgave = harTildeltOppgave,
        oppgaveService = oppgaveService,
        skjæringstidspunkt = godkjenningsbehovData.skjæringstidspunkt,
        fødselsnummer = Fødselsnummer(godkjenningsbehovData.fødselsnummer),
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
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), person.id.value, 0, false))
        assertTrue(command().resume(context, sessionContext, outbox))
        assertEquals(1, persisterteÅpneGosysOppgaver().size)
    }

    @Test
    fun `Lagrer ikke varsel ved ingen åpne oppgaver og deaktiverer eventuelt eksisterende varsel`() {
        behandling1.nyttVarsel("SB_EX_1")
        behandling1.assertAntallVarsler(1)
        commandContext().let { commandContext ->
            commandContext.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), person.id.value, 0, false))
            assertTrue(command().resume(commandContext, sessionContext, outbox))
        }
        assertEquals(1, persisterteÅpneGosysOppgaver().size)
        behandling1.assertAntallVarsler(1)
        behandling1.assertHarVarsel("SB_EX_1", Varsel.Status.INAKTIV)
        verify(exactly = 1) { oppgaveService.fjernGosysEgenskap(any()) }
    }

    @Test
    fun `Deaktiverer ikke varsel dersom oppgave er tildelt`() {
        behandling1.nyttVarsel("SB_EX_1")
        val context = commandContext()
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), person.id.value, 0, false))
        assertTrue(command(harTildeltOppgave = true).resume(context, sessionContext, outbox))
        behandling1.assertAntallVarsler(1)
        behandling1.assertHarVarsel("SB_EX_1", Varsel.Status.AKTIV)
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
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), person.id.value, null, true))
        assertTrue(command().resume(context, sessionContext, outbox))
        assertEquals(1, persisterteÅpneGosysOppgaver().size)
        behandling1.assertAntallVarsler(1)
        behandling1.assertHarVarsel("SB_EX_3", Varsel.Status.AKTIV)
    }

    @Test
    fun `Legger ikke til egenskap for gosys dersom det er andre varsler på perioden`() {
        behandling1.nyttVarsel("SB_EX_4")
        val context = commandContext()
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), person.id.value, 1, false))
        command().resume(context, sessionContext, outbox)
        verify(exactly = 0) { oppgaveService.leggTilGosysEgenskap(any()) }
    }

    @Test
    fun `Legger ikke til egenskap for gosys dersom det er andre varsler på andre overlappende perioder`() {
        behandling1.nyttVarsel("SB_EX_4")
        val context = commandContext()
        context.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), person.id.value, 1, false))
        command(vedtaksperiodeId = vedtaksperiode2.id.value).resume(context, sessionContext, outbox)
        verify(exactly = 0) { oppgaveService.leggTilGosysEgenskap(any()) }
    }

    private fun lagrerVarselVedÅpneOppgaver(
        harTildeltOppgave: Boolean,
        commandContext: CommandContext,
    ) {
        val forventetAntallFørDenneOppgaven = persisterteÅpneGosysOppgaver().size + 1
        commandContext.add(ÅpneGosysOppgaverløsning(LocalDateTime.now(), person.id.value, 1, false))
        assertTrue(command(harTildeltOppgave).resume(commandContext, sessionContext, outbox))
        assertEquals(forventetAntallFørDenneOppgaven, persisterteÅpneGosysOppgaver().size)
        behandling1.assertAntallVarsler(1)
        behandling1.assertHarVarsel("SB_EX_1", Varsel.Status.AKTIV)
    }
}

internal fun LegacyBehandling.inspektør(block: BehandlingDto.() -> Unit) {
    this.toDto().block()
}
