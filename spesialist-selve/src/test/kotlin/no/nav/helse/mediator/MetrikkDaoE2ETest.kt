package no.nav.helse.mediator

import AbstractE2ETestV2
import no.nav.helse.TestRapidHelpers.contextId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MetrikkDaoE2ETest : AbstractE2ETestV2() {

    private val sut = MetrikkDao(dataSource)

    @Test
    fun `identifiserer automatisk godkjenning`() {
        automatiskGodkjent()

        val contextId = inspektør.contextId()
        assertEquals(GodkjenningsbehovUtfall.AutomatiskGodkjent, sut.finnUtfallForGodkjenningsbehov(contextId))
    }

    @Test
    fun `identifiserer automatisk avvisning`() {
        fremTilÅpneOppgaver(fullmakter = listOf())

        val contextId = inspektør.contextId()
        assertEquals(GodkjenningsbehovUtfall.AutomatiskAvvist, sut.finnUtfallForGodkjenningsbehov(contextId))
    }

    @Test
    fun `identifiserer manuell oppgave`() {
        fremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(antall = 1)
        håndterRisikovurderingløsning()

        val contextId = inspektør.contextId()
        assertEquals(GodkjenningsbehovUtfall.ManuellOppgave, sut.finnUtfallForGodkjenningsbehov(contextId))
    }
}
