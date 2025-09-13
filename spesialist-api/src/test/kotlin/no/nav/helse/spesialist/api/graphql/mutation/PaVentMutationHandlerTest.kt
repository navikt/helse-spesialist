package no.nav.helse.spesialist.api.graphql.mutation

import io.mockk.every
import no.nav.helse.TestRunner.runQuery
import no.nav.helse.spesialist.api.testfixtures.lagSaksbehandler
import no.nav.helse.spesialist.api.testfixtures.mutation.fjernPåVentMutation
import no.nav.helse.spesialist.api.testfixtures.mutation.leggPåVentMutation
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.random.Random.Default.nextLong

internal class PaVentMutationHandlerTest {

    @Test
    fun `legger på vent`() {
        val saksbehandler = lagSaksbehandler()
        runQuery(
            saksbehandler = saksbehandler,
            whenever = leggPåVentMutation(nextLong(), """Dette er et notat""", 1 jan 2024, true),
            then = { _, body, _ ->
                assertEquals(saksbehandler.id().value.toString(), body["data"]["leggPaVent"]["oid"].asText())
            }
        )
    }

    @Test
    fun `legger på vent med årsaker`() {
        val saksbehandler = lagSaksbehandler()

        runQuery(
            saksbehandler = saksbehandler,
            whenever = leggPåVentMutation(
                nextLong(),
                "Dette er et notat",
                1 jan 2024,
                true,
                arsaker = mapOf("_key" to "en_key", "arsak" to "en_arsak")
            ),
            then = { _, body, _ ->
                assertEquals(saksbehandler.id().value.toString(), body["data"]["leggPaVent"]["oid"].asText())
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

    @Test
    fun `feilhåndtering for legg på vent`() {
        runQuery(
            given = {
                every {
                    it.saksbehandlerMediator.påVent(any(), any())
                } throws IOException("noe galt skjedde liksom mot databasen")
            },
            whenever = leggPåVentMutation(nextLong(), "tekst", 1 jan 2024, true),
            then = { _, body, _ ->
                assertEquals(500, body["errors"][0]["extensions"]["code"].asInt())
            }
        )
    }
}
