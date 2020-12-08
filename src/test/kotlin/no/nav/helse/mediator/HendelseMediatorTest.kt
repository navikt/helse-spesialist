package no.nav.helse.mediator

import io.mockk.mockk
import no.nav.helse.mediator.api.AnnulleringDto
import no.nav.helse.mediator.api.GodkjenningDTO
import no.nav.helse.mediator.api.TilbakerullingDTO
import no.nav.helse.mediator.api.TilbakerullingMedSlettingDTO
import no.nav.helse.mediator.api.modell.Saksbehandler
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

    @Test
    fun `publiserer annullering på rapid`() {
        val oid = UUID.randomUUID()
        val navn = "Siri Saksbehandler"
        mediator.håndter(
            annulleringDto = AnnulleringDto(
                aktørId = "X999999",
                fødselsnummer = "12345612345",
                organisasjonsnummer = "12",
                fagsystemId = "foo",
                saksbehandlerIdent = "X999999"
            ), Saksbehandler("siri.saksbehandler@nav.no", oid, "X999999", navn)
        )
        assertEquals("annullering", testRapid.inspektør.field(0, "@event_name").asText())
        assertEquals("siri.saksbehandler@nav.no", testRapid.inspektør.field(0, "saksbehandler")["epostaddresse"].asText())
        assertEquals("X999999", testRapid.inspektør.field(0, "saksbehandler")["ident"].asText())
        assertEquals(oid.toString(), testRapid.inspektør.field(0, "saksbehandler")["oid"].asText())
        assertEquals(navn, testRapid.inspektør.field(0, "saksbehandler")["navn"].asText())
    }
}
