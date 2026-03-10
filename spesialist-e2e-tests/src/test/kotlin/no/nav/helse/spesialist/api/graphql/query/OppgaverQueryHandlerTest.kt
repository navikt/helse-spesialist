package no.nav.helse.spesialist.api.graphql.query

import io.mockk.every
import io.mockk.verify
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallOppgaver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OppgaverQueryHandlerTest : AbstractGraphQLApiTest() {


    @Test
    fun `antallOppgaver returnerer antall oppgaver`() {
        every {
            apiOppgaveService.antallOppgaver(any())
        } returns ApiAntallOppgaver(antallMineSaker = 2, antallMineSakerPaVent = 1)

        val body = runQuery(
            """
            {
                antallOppgaver {
                    antallMineSaker
                    antallMineSakerPaVent
                }
            }
            """.trimIndent(),
        )

        val antallMineSaker = body["data"]["antallOppgaver"]["antallMineSaker"].asInt()
        val antallMineSakerPåVent = body["data"]["antallOppgaver"]["antallMineSakerPaVent"].asInt()

        verify(exactly = 1) {
            apiOppgaveService.antallOppgaver(
                saksbehandler = any(),
            )
        }
        assertEquals(2, antallMineSaker)
        assertEquals(1, antallMineSakerPåVent)
    }
}
