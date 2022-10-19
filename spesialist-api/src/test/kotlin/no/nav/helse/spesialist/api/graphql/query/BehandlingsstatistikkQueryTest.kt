package no.nav.helse.spesialist.api.graphql.query

import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class BehandlingsstatistikkQueryTest : AbstractGraphQLApiTest() {

    @BeforeAll
    fun setup() {
        setupGraphQLServer()
    }

    @Test
    fun `henter behandlingsstatistikk`() {
        val query = queryize(
            """
            {
                behandlingsstatistikk {
                    enArbeidsgiver {
                        automatisk
                        manuelt
                        tilgjengelig
                    }                
                }
            }
        """
        )
        val body = runQuery(query)
        val behandlingsstatistikk = body["data"]["behandlingsstatistikk"]["enArbeidsgiver"]

        assertEquals(0, behandlingsstatistikk["automatisk"].asInt())
        assertEquals(0, behandlingsstatistikk["manuelt"].asInt())
        assertEquals(0, behandlingsstatistikk["tilgjengelig"].asInt())
    }

}