package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.testfixtures.mutation.fjernPåVentMutation
import no.nav.helse.spesialist.api.testfixtures.mutation.leggPåVentMutation
import no.nav.helse.spesialist.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PaVentMutationHandlerTest : AbstractGraphQLApiTest() {

    @Test
    fun `legger på vent`() {
        val oid = opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val body =
            runQuery(leggPåVentMutation(1, """Dette er et notat""", 1 jan 2024, true))
        assertEquals(oid.toString(), body["data"]["leggPaVent"]["oid"].asText())
    }

    @Test
    fun `legger på vent med årsaker`() {
        val oid = opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val body =
            runQuery(
                leggPåVentMutation(
                    1,
                    "Dette er et notat",
                    1 jan 2024,
                    true,
                    arsaker = mapOf("_key" to "en_key", "arsak" to "en_arsak")
                )
            )
        assertEquals(oid.toString(), body["data"]["leggPaVent"]["oid"].asText())
    }

    @Test
    fun `fjern fra på vent`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val body = runQuery(fjernPåVentMutation(1))
        assertTrue(body["data"]["fjernPaVent"].asBoolean())
    }
}
