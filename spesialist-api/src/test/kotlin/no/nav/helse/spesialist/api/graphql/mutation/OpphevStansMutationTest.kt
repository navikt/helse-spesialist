package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OpphevStansMutationTest : AbstractGraphQLApiTest() {
    @Test
    fun `opphev stans`() {
        val body =
            runQuery(
                """
                mutation OpphevStans {
                    opphevStans(
                        fodselsnummer: "$FØDSELSNUMMER",
                        arsak: "EN_ÅRSAK"
                    )
                }
                """.trimIndent(),
            )

        assertTrue(body["data"]["opphevStans"].asBoolean())
    }
}
