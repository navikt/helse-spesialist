package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.TestRunner.runQuery
import no.nav.helse.spesialist.api.testfixtures.mutation.opphevStansMutation
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OpphevStansMutationHandlerTest {
    @Test
    fun `opphev stans`() {
        runQuery(
            whenever = opphevStansMutation(lagFødselsnummer(), "EN_BEGRUNNELSE"),
            then = { _, body, _ ->
                assertTrue(body["data"]["opphevStans"].asBoolean())
            }
        )
    }
}

