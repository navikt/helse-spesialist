package no.nav.helse.mediator

import AbstractE2ETest
import io.mockk.mockk
import no.nav.helse.mediator.api.AnnulleringDto
import no.nav.helse.mediator.api.GodkjenningDTO
import no.nav.helse.mediator.api.modell.Saksbehandler
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

internal class HendelseMediatorTest: AbstractE2ETest() {

    //private val testRapid = TestRapid()

    private val mediator = HendelseMediator(
        rapidsConnection = testRapid,
        dataSource = dataSource,
        oppgaveMediator = oppgaveMediator,
        hendelsefabrikk = hendelsefabrikk,
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
    fun `publiserer annullering på rapid`() {
        val oid = UUID.randomUUID()
        val navn = "Siri Saksbehandler"
        val ident = "Z999999"
        mediator.håndter(
            annulleringDto = AnnulleringDto(
                aktørId = "X999999",
                fødselsnummer = "12345612345",
                organisasjonsnummer = "12",
                fagsystemId = "foo",
                saksbehandlerIdent = "X999999",
                begrunnelser = listOf("En", "Toten", "Tre"),
                kommentar = "Nittedal Tistedal Elverum",
            ), Saksbehandler("siri.saksbehandler@nav.no", oid, navn, ident)
        )
        assertEquals("annullering", testRapid.inspektør.field(0, "@event_name").asText())
        assertEquals(
            "siri.saksbehandler@nav.no",
            testRapid.inspektør.field(0, "saksbehandler")["epostaddresse"].asText()
        )
        assertEquals("X999999", testRapid.inspektør.field(0, "saksbehandler")["ident"].asText())
        assertEquals(oid.toString(), testRapid.inspektør.field(0, "saksbehandler")["oid"].asText())
        assertEquals(navn, testRapid.inspektør.field(0, "saksbehandler")["navn"].asText())
        assertEquals("Nittedal Tistedal Elverum", testRapid.inspektør.field(0, "kommentar").asText())
        assertEquals("En", testRapid.inspektør.field(0, "begrunnelser")[0].asText())
    }

    @Test
    fun `publiserer annullering på rapid tomme verdier`() {
        val oid = UUID.randomUUID()
        val navn = "Siri Saksbehandler"
        val ident = "Z999999"
        mediator.håndter(
            annulleringDto = AnnulleringDto(
                aktørId = "X999999",
                fødselsnummer = "12345612345",
                organisasjonsnummer = "12",
                fagsystemId = "foo",
                saksbehandlerIdent = "X999999",
                begrunnelser = listOf(),
                kommentar = null
            ), Saksbehandler("siri.saksbehandler@nav.no", oid, navn, ident)
        )
        assertEquals("annullering", testRapid.inspektør.field(0, "@event_name").asText())
        assertEquals(
            "siri.saksbehandler@nav.no",
            testRapid.inspektør.field(0, "saksbehandler")["epostaddresse"].asText()
        )
        assertEquals("X999999", testRapid.inspektør.field(0, "saksbehandler")["ident"].asText())
        assertEquals(oid.toString(), testRapid.inspektør.field(0, "saksbehandler")["oid"].asText())
        assertEquals(navn, testRapid.inspektør.field(0, "saksbehandler")["navn"].asText())
        assertThrows<IllegalArgumentException> { testRapid.inspektør.field(0, "kommentar") }
        assertEquals(emptyList<String>(), testRapid.inspektør.field(0, "begrunnelser").map { it.asText() })
    }
}
