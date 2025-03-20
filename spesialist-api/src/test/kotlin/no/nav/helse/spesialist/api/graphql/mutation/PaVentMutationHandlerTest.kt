package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.TestRunner.runQuery
import no.nav.helse.spesialist.api.testfixtures.lagSaksbehandlerFraApi
import no.nav.helse.spesialist.api.testfixtures.mutation.fjernPåVentMutation
import no.nav.helse.spesialist.api.testfixtures.mutation.leggPåVentMutation
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random.Default.nextLong

internal class PaVentMutationHandlerTest {

    @Test
    fun `legger på vent`() {
        val saksbehandler = lagSaksbehandlerFraApi()
        runQuery(
            saksbehandlerFraApi = saksbehandler,
            whenever = leggPåVentMutation(nextLong(), """Dette er et notat""", 1 jan 2024, true),
            then = { _, body, _ ->
                assertEquals(saksbehandler.oid.toString(), body["data"]["leggPaVent"]["oid"].asText())
            }
        )
    }

    @Test
    fun `legger på vent med årsaker`() {
        val saksbehandler = lagSaksbehandlerFraApi()

        runQuery(
            saksbehandlerFraApi = saksbehandler,
            whenever = leggPåVentMutation(
                nextLong(),
                "Dette er et notat",
                1 jan 2024,
                true,
                arsaker = mapOf("_key" to "en_key", "arsak" to "en_arsak")
            ),
            then = { _, body, _ ->
                assertEquals(saksbehandler.oid.toString(), body["data"]["leggPaVent"]["oid"].asText())
            }
        )
    }

    @Test
    fun `fjern fra på vent`() {
        runQuery(
            whenever = fjernPåVentMutation(nextLong()),
            then = { _, body, _ ->
                assertTrue(body["data"]["fjernPaVent"].asBoolean())
            }
        )
    }
}
