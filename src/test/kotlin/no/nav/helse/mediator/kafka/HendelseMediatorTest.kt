package no.nav.helse.mediator.kafka

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.Oppgavestatus
import no.nav.helse.api.GodkjenningDTO
import no.nav.helse.api.OppgaveMediator
import no.nav.helse.api.TilbakerullingDTO
import no.nav.helse.api.TilbakerullingMedSlettingDTO
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class HendelseMediatorTest {

    private val testRapid = TestRapid()

    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val mediator = HendelseMediator(
        testRapid,
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        oppgaveMediator,
        mockk(relaxed = true)
    )

    @Test
    fun `oppgave avventer system når saksbehandlerløsning legges på rapid`() {
        val oppgavereferanse = 1L
        val oid = UUID.randomUUID()
        val epost = "epost@nav.no"
        val saksbehandlerIdent = "saksbehandler"
        mediator.håndter(GodkjenningDTO(oppgavereferanse, true, saksbehandlerIdent, null, null, null), epost, oid)
        assertEquals("saksbehandler_løsning", testRapid.inspektør.field(0, "@event_name").asText())
        verify(exactly = 1) { oppgaveMediator.oppdater(
            any(),
            any(),
            oppgavereferanse,
            Oppgavestatus.AvventerSystem,
            saksbehandlerIdent,
            oid
        ) }
    }

    @Test
    fun `publiserer tilbakerulling på rapid`() {
        mediator.håndter(TilbakerullingDTO("FNR", "AKTØRID", 1L))
        assertEquals("rollback_person", testRapid.inspektør.field(0, "@event_name").asText())
        assertEquals("FNR", testRapid.inspektør.field(0, "fødselsnummer").asText())
        assertEquals("AKTØRID", testRapid.inspektør.field(0, "aktørId").asText())
        assertEquals(1L, testRapid.inspektør.field(0, "personVersjon").asLong())
    }

    @Test
    fun `publiserer tilbakerulling med sletting på rapid`() {
        mediator.håndter(TilbakerullingMedSlettingDTO("FNR", "AKTØRID"))
        assertEquals("rollback_person_delete", testRapid.inspektør.field(0, "@event_name").asText())
        assertEquals("FNR", testRapid.inspektør.field(0, "fødselsnummer").asText())
        assertEquals("AKTØRID", testRapid.inspektør.field(0, "aktørId").asText())
    }
}
