package no.nav.helse.spesialist.api

import java.util.UUID
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.db.AbstractDatabaseTest
import no.nav.helse.spesialist.api.saksbehandler.Saksbehandler
import no.nav.helse.spesialist.api.utbetaling.AnnulleringDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SaksbehandlerMediatorTest: AbstractDatabaseTest() {
    private val testRapid = TestRapid()
    private val mediator = SaksbehandlerMediator(dataSource, testRapid)

    @BeforeEach
    internal fun beforeEach() {
        testRapid.reset()
    }

    @Test
    fun `håndterer annullering`() {
        val oid = UUID.randomUUID()
        val navn = "ET_NAVN"
        mediator.håndter(annullering(), Saksbehandler("epost@nav.no", oid, navn, "EN_IDENT"))

        assertEquals(1, testRapid.inspektør.size)
        val melding = testRapid.inspektør.message(0)
        assertEquals("annullering", melding["@event_name"].asText())

        assertEquals(oid.toString(), melding["saksbehandler"]["oid"].asText())
        assertEquals("epost@nav.no", melding["saksbehandler"]["epostaddresse"].asText())
        assertEquals(navn, melding["saksbehandler"]["navn"].asText())
        assertEquals("EN_IDENT", melding["saksbehandler"]["ident"].asText())

        assertEquals("EN_FAGSYSTEMID", melding["fagsystemId"].asText())
        assertEquals("EN_KOMMENTAR", melding["kommentar"]?.asText())
        assertEquals(1, melding["begrunnelser"].map { it.asText() }.size)
        assertEquals("EN_BEGRUNNELSE", melding["begrunnelser"][0].asText())
    }

    @Test
    fun `håndterer annullering uten kommentar og begrunnelser`() {
        val oid = UUID.randomUUID()
        val navn = "ET_NAVN"
        mediator.håndter(annullering(emptyList(), null), Saksbehandler("epost@nav.no", oid, navn, "EN_IDENT"))

        val melding = testRapid.inspektør.message(0)

        assertEquals("annullering", melding["@event_name"].asText())

        assertEquals(oid.toString(), melding["saksbehandler"]["oid"].asText())
        assertEquals("epost@nav.no", melding["saksbehandler"]["epostaddresse"].asText())
        assertEquals(navn, melding["saksbehandler"]["navn"].asText())
        assertEquals("EN_IDENT", melding["saksbehandler"]["ident"].asText())

        assertEquals("EN_FAGSYSTEMID", melding["fagsystemId"].asText())
        assertEquals(null, melding["kommentar"]?.asText())
        assertEquals(0, melding["begrunnelser"].map { it.asText() }.size)
    }

    private fun annullering(
        begrunnelser: List<String> = listOf("EN_BEGRUNNELSE"),
        kommentar: String? = "EN_KOMMENTAR",
    ) = AnnulleringDto(
        aktørId = "EN_AKTØR",
        fødselsnummer = "ET_FØDSELSNUMMER",
        organisasjonsnummer = "ET_ORGANISASJONSNUMMER",
        fagsystemId = "EN_FAGSYSTEMID",
        saksbehandlerIdent = "EN_IDENT",
        begrunnelser = begrunnelser,
        kommentar = kommentar
    )
}