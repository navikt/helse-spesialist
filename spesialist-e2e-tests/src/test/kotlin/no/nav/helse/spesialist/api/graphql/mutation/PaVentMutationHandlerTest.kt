package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PaVentMutationHandlerTest : AbstractGraphQLApiTest() {
    @Test
    fun `legger på vent`() {
        val oid = opprettSaksbehandler()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()

        val body =
            runQuery(
                """
                    mutation LeggPaVent {
                        leggPaVent(
                            notatTekst: "Dette er et notat",
                            frist: "2024-01-01",
                            oppgaveId: "1",
                            tildeling: true,
                        ) {
                            oid
                        }
                    }
                """,
            )
        assertEquals(oid.toString(), body["data"]["leggPaVent"]["oid"].asText())
    }

    @Test
    fun `legger på vent med årsaker`() {
        val oid = opprettSaksbehandler()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()

        val body =
            runQuery(
                """
                    mutation LeggPaVent {
                        leggPaVent(
                            notatTekst: "Dette er et notat",
                            frist: "2024-01-01",
                            oppgaveId: "1",
                            tildeling: true,
                            arsaker: {_key: "key", arsak: "arsak"}
                        ) {
                            oid
                        }
                    }
                """,
            )
        assertEquals(oid.toString(), body["data"]["leggPaVent"]["oid"].asText())
    }

    @Test
    fun `fjern fra på vent`() {
        opprettSaksbehandler()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()

        val body =
            runQuery(
                """
                    mutation {
                        fjernPaVent(
                            oppgaveId: "1",
                        )
                    }
                """,
            )
        assertTrue(body["data"]["fjernPaVent"].asBoolean())
    }
}
