package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.testfixtures.mutation.abonnerMutation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OpptegnelseMutationHandlerTest: AbstractGraphQLApiTest() {
    @Test
    fun `opprett abonnement`() {
        val body = runQuery(
            abonnerMutation("123")
        )
        assertEquals(true, body["data"]["opprettAbonnement"].asBoolean())
    }

}

