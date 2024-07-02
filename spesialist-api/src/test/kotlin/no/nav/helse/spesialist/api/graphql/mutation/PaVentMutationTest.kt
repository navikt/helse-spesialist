package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PaVentMutationTest : AbstractGraphQLApiTest() {
    @Test
    fun `legger på vent`() {
        val oid = opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val body =
            runQuery(
                """
                    mutation LeggPaVent {
                        leggPaVent(
                            notatTekst: "Dette er et notat",
                            notatType: PaaVent,
                            frist: "2024-01-01",
                            oppgaveId: "1",
                            tildeling: true,
                            begrunnelse: null
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
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

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
