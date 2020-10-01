package no.nav.helse.annullering

import no.nav.helse.api.AnnulleringDto
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class AnnulleringMediatorTest {

    private val testrapid = TestRapid()
    private val mediator = AnnulleringMediator(testrapid)

    private val fødselsnummer = "et-fødselsnummer"
    private val aktørId = "en-aktørId"
    private val epostadresse = "en-epostadresse"
    private val oid = UUID.randomUUID()

    @Test
    fun `publiserer annullering på rapid`() {
        val dager = listOf(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2020, 1, 2),
            LocalDate.of(2020, 1, 3),
            LocalDate.of(2020, 1, 4)
        )
        mediator.håndter(
            annulleringDto = AnnulleringDto(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = "et-organisasjonsnummer",
                dager = dager
            ),
            oid = oid,
            epostadresse = epostadresse
        )
        Assertions.assertEquals("annuller", testrapid.inspektør.field(0, "@event_name").asText())
        Assertions.assertEquals(fødselsnummer, testrapid.inspektør.field(0, "fødselsnummer").asText())
        Assertions.assertEquals(aktørId, testrapid.inspektør.field(0, "aktørId").asText())
        Assertions.assertEquals(epostadresse, testrapid.inspektør.field(0, "saksbehandlerEpost").asText())
        Assertions.assertEquals(oid.toString(), testrapid.inspektør.field(0, "saksbehandler").asText())
        Assertions.assertEquals(dager, testrapid.inspektør.field(0, "dager").map { it.asLocalDate() })
    }

}
