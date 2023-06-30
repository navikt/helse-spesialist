package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OpptegnelseMutationTest: AbstractGraphQLApiTest() {
    @Test
    fun `opprett abonnement`() {
        val body = runQuery(
            """mutation Abonner {
                abonner(personidentifikator: "123")
            }"""
        )
        assertEquals(true, body["data"]["abonner"].asBoolean())
    }
}