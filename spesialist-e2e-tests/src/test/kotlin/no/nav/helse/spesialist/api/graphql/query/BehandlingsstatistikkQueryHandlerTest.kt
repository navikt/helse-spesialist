package no.nav.helse.spesialist.api.graphql.query

import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BehandlingsstatistikkQueryHandlerTest : AbstractGraphQLApiTest() {

    @Test
    fun `henter behandlingsstatistikk`() {
        val body = runQuery(
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
        val behandlingsstatistikk = body["data"]["behandlingsstatistikk"]["enArbeidsgiver"]

        assertEquals(0, behandlingsstatistikk["automatisk"].asInt())
        assertEquals(0, behandlingsstatistikk["manuelt"].asInt())
        assertEquals(0, behandlingsstatistikk["tilgjengelig"].asInt())
    }

}