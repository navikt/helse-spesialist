package no.nav.helse.spesialist.api.rest

import no.nav.helse.spesialist.api.OpphevStansIntegrationTest
import no.nav.helse.spesialist.domain.Saksbehandler
import org.junit.jupiter.api.Assertions.assertEquals

class OpphevStansHttpApiIntegrationTest : OpphevStansIntegrationTest() {
    override fun postAndAssertSuccess(
        saksbehandler: Saksbehandler,
        fødselsnummer: String,
        begrunnelse: String
    ) {
        val response = integrationTestFixture.post(
            "/api/opphevstans",
            body = """{ "fodselsnummer": "$fødselsnummer", "begrunnelse": "$begrunnelse" }""",
            saksbehandler = saksbehandler,
        )

        assertEquals(200, response.status)
        assertEquals("true", response.bodyAsText)
    }
}
