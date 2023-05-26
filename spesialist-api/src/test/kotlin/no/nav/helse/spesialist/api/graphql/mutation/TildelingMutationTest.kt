package no.nav.helse.spesialist.api.graphql.mutation

import java.util.UUID
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TildelingMutationTest : AbstractGraphQLApiTest() {
    @Test
    fun `oppretter tildeling`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)

        val body = runQuery(
            """
                mutation OpprettTildeling {
                    opprettTildeling(
                        oppgaveId: "$oppgaveId",
                    ) {
                        navn, oid, epost, reservert
                    }
                }
            """
        )

        assertEquals(SAKSBEHANDLER.oid, UUID.fromString(body["data"]["opprettTildeling"]["oid"].asText()))
    }

    @Test
    fun `kan ikke tildele allerede tildelt oppgave`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)
        tildelOppgave(oppgaveId, SAKSBEHANDLER.oid)

        val body = runQuery(
            """
                mutation OpprettTildeling {
                    opprettTildeling(
                        oppgaveId: "$oppgaveId",
                    ) {
                        navn, oid, epost, reservert
                    }
                }
            """
        )

        assertEquals(409, body["errors"].first()["extensions"]["code"]["value"].asInt())
    }

    @Test
    fun `kan fjerne tildeling`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)
        tildelOppgave(oppgaveId, SAKSBEHANDLER.oid)

        val body = runQuery(
            """
                mutation FjernTildeling {
                    fjernTildeling(
                        oppgaveId: "$oppgaveId"
                    )
                }
            """
        )

        assertTrue(body["data"]["fjernTildeling"].booleanValue())
    }

    @Test
    fun `returnerer false hvis oppgaven ikke er tildelt`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)

        val body = runQuery(
            """
                mutation FjernTildeling {
                    fjernTildeling(
                        oppgaveId: "$oppgaveId"
                    )
                }
            """
        )

        assertFalse(body["data"]["fjernTildeling"].booleanValue())
    }

    @Test
    fun `returnerer false hvis oppgaven ikke finnes`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val oppgaveId = finnOppgaveIdFor(PERIODE.id)
        tildelOppgave(oppgaveId, SAKSBEHANDLER.oid)

        val body = runQuery(
            """
                mutation FjernTildeling {
                    fjernTildeling(
                        oppgaveId: "999"
                    )
                }
            """
        )

        assertFalse(body["data"]["fjernTildeling"].booleanValue())
    }
}
