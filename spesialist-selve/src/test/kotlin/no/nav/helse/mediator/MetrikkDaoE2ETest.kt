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
        spesialistInnvilgerAutomatisk()

        val contextId = inspektør.contextId()
        assertEquals(GodkjenningsbehovUtfall.AutomatiskGodkjent, dao.finnUtfallForGodkjenningsbehov(contextId))
    }

    @Test
    fun `identifiserer automatisk avvisning`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver(fullmakter = listOf())

        val contextId = inspektør.contextId()
        assertEquals(GodkjenningsbehovUtfall.AutomatiskAvvist, dao.finnUtfallForGodkjenningsbehov(contextId))
    }

    @Test
    fun `identifiserer manuell oppgave`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(antall = 1)
        håndterRisikovurderingløsning()
        håndterAutomatiseringStoppetAvVeilederløsning()

        val contextId = inspektør.contextId()
        assertEquals(GodkjenningsbehovUtfall.ManuellOppgave, dao.finnUtfallForGodkjenningsbehov(contextId))
    }
}
