package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.testfixtures.mutation.opphevStansMutation
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OpphevStansMutationHandlerTest : AbstractGraphQLApiTest() {
    @Test
    fun `opphev stans`() {
        val body = runQuery(opphevStansMutation(FØDSELSNUMMER, "EN_BEGRUNNELSE"))
        assertTrue(body["data"]["opphevStans"].asBoolean())
    }
}

