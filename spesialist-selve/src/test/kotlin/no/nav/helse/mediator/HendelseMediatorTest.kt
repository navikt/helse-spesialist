package no.nav.helse.mediator

import AbstractE2ETest
import java.util.UUID
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.mediator.api.GodkjenningDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class HendelseMediatorTest : AbstractE2ETest() {

    @Test
    fun `oppgave avventer system når saksbehandlerløsning legges på rapid`() {
        val oid = UUID.randomUUID()
        val epost = "epost@nav.no"
        val saksbehandlerIdent = "saksbehandler"
        settOppBruker()
        val oppgavereferanse = oppgaveDao.finnOppgaveId(FØDSELSNUMMER)!!
        hendelseMediator.håndter(GodkjenningDTO(oppgavereferanse, true, saksbehandlerIdent, null, null, null), epost, oid)
        assertTrue(testRapid.inspektør.hendelser("saksbehandler_løsning").isNotEmpty())
        assertEquals("AvventerSystem", testRapid.inspektør.hendelser("oppgave_oppdatert").last()["status"].asText())
    }

    @Test
    fun `oppgave_oppdater inneholder påVent-flagg i noen tilfeller`() {
        fun påVentNode() = testRapid.inspektør.hendelser("oppgave_oppdatert").last()["påVent"]

        settOppBruker()
        val oppgavereferanse = oppgaveDao.finnOppgaveId(FØDSELSNUMMER)!!
        hendelseMediator.sendMeldingOppgaveOppdatert(oppgavereferanse)
        assertNull(påVentNode())

        hendelseMediator.sendMeldingOppgaveOppdatert(oppgavereferanse, påVent = true)
        assertEquals("true", påVentNode().asText())

        hendelseMediator.sendMeldingOppgaveOppdatert(oppgavereferanse, påVent = false)
        assertEquals("false", påVentNode().asText())
    }

}
