package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.TestRunner.runQuery
import no.nav.helse.spesialist.api.testfixtures.mutation.abonnerMutation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OpptegnelseMutationHandlerTest {
    @Test
    fun `opprett abonnement`() {
        runQuery(
            whenever = abonnerMutation("123"),
            then = { _, body ->
                assertEquals(true, body["data"]["opprettAbonnement"].asBoolean())
            }
        )
    }
}

