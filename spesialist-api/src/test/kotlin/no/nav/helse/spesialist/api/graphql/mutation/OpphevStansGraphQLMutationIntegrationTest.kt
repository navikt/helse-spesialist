package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.spesialist.api.OpphevStansIntegrationTest
import no.nav.helse.spesialist.api.testfixtures.mutation.opphevStansMutation
import no.nav.helse.spesialist.domain.Saksbehandler
import org.junit.jupiter.api.Assertions.assertEquals

class OpphevStansGraphQLMutationIntegrationTest : OpphevStansIntegrationTest() {
    override fun postAndAssertSuccess(
        saksbehandler: Saksbehandler,
        fødselsnummer: String,
        begrunnelse: String
    ) {
        val responseJson = integrationTestFixture.executeQuery(
            saksbehandler = saksbehandler,
            query = opphevStansMutation(fødselsnummer, begrunnelse),
        )

        // Then:
        // Sjekk svaret
        assertEquals(true, responseJson.get("data")?.get("opphevStans")?.asBoolean())
    }
}
