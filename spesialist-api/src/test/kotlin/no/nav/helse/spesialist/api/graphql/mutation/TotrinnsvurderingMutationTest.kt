package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TotrinnsvurderingMutationTest : AbstractGraphQLApiTest() {

    @Test
    fun `send oppgave til godkjenning`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val oppgaveRef = finnOppgaveIdFor(PERIODE.id)

        val body = runQuery(
            """
            mutation TotrinnsvurderingMutation {
                sendTilGodkjenning(oppgavereferanse: "$oppgaveRef", avslag: null)
            }
        """
        )

        assertTrue(body["data"]["sendTilGodkjenning"].asBoolean())
    }

    @Test
    fun `send oppgave i retur`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val oppgaveRef = finnOppgaveIdFor(PERIODE.id)

        val body = runQuery(
            """
            mutation TotrinnsvurderingMutation {
                sendIRetur(oppgavereferanse: "$oppgaveRef", notatTekst: "Retur")
            }
        """
        )

        assertTrue(body["data"]["sendIRetur"].asBoolean())
    }
}
