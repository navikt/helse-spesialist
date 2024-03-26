package no.nav.helse.mediator

import AbstractE2ETest
import no.nav.helse.TestRapidHelpers.contextId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MetrikkDaoE2ETest : AbstractE2ETest() {

    private val dao = MetrikkDao(dataSource)

    @Test
    fun `identifiserer automatisk godkjenning`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        automatiskGodkjent()

        val contextId = inspektør.contextId()
        assertEquals(GodkjenningsbehovUtfall.AutomatiskGodkjent, dao.finnUtfallForGodkjenningsbehov(contextId))
    }

    @Test
    fun `identifiserer automatisk avvisning`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        fremTilÅpneOppgaver(fullmakter = listOf())

        val contextId = inspektør.contextId()
        assertEquals(GodkjenningsbehovUtfall.AutomatiskAvvist, dao.finnUtfallForGodkjenningsbehov(contextId))
    }

    @Test
    fun `identifiserer manuell oppgave`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        fremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(antall = 1)
        håndterRisikovurderingløsning()

        val contextId = inspektør.contextId()
        assertEquals(GodkjenningsbehovUtfall.ManuellOppgave, dao.finnUtfallForGodkjenningsbehov(contextId))
    }
}
