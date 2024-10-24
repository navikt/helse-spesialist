package no.nav.helse.mediator

import AbstractE2ETest
import kotliquery.sessionOf
import no.nav.helse.TestRapidHelpers.contextId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MetrikkDaoE2ETest : AbstractE2ETest() {
    private val session = sessionOf(dataSource)
    private val dao = MetrikkDao(session)

    @AfterEach
    fun lukkSession() = session.close()

    @Test
    fun `identifiserer automatisk godkjenning`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistInnvilgerAutomatisk()

        val contextId = inspektør.contextId()
        assertEquals(GodkjenningsbehovUtfall.AutomatiskGodkjent, dao.finnUtfallForGodkjenningsbehov(contextId))

        // sanity check
        assertTrue(erFerdigstilt(sisteGodkjenningsbehovId))
    }

    @Test
    fun `identifiserer automatisk avvisning`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver(enhet = "0393")
        håndterÅpneOppgaverløsning()
        håndterRisikovurderingløsning()

        val contextId = inspektør.contextId()
        assertEquals(GodkjenningsbehovUtfall.AutomatiskAvvist, dao.finnUtfallForGodkjenningsbehov(contextId))

        // sanity check
        assertTrue(erFerdigstilt(sisteGodkjenningsbehovId))
    }

    @Test
    fun `identifiserer avbrutt-utfall`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(kanGodkjennesAutomatisk = true)

        håndterGodkjenningsbehovUtenValidering()

        val contextId = commandContextId(sisteGodkjenningsbehovId)
        assertEquals(GodkjenningsbehovUtfall.Avbrutt, dao.finnUtfallForGodkjenningsbehov(contextId))

        // sanity check
        assertTrue(erFerdigstilt(sisteGodkjenningsbehovId))
    }

    @Test
    fun `identifiserer manuell oppgave`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilÅpneOppgaver()
        håndterÅpneOppgaverløsning(antallÅpneOppgaverIGosys = 1)
        håndterRisikovurderingløsning()

        val contextId = inspektør.contextId()
        assertEquals(GodkjenningsbehovUtfall.ManuellOppgave, dao.finnUtfallForGodkjenningsbehov(contextId))
    }
}
