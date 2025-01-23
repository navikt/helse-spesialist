package no.nav.helse.spesialist.api.graphql.mutation

import io.mockk.every
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.SendTilGodkjenningResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TotrinnsvurderingMutationTest : AbstractGraphQLApiTest() {

    @Test
    fun `send oppgave til godkjenning med V2`() {
        every { saksbehandlerhåndterer.håndterTotrinnsvurdering(any()) }.returns(SendTilGodkjenningResult.Ok)
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val oppgaveRef = finnOppgaveIdFor(PERIODE.id)

        val body = runQuery(
            """
            mutation TotrinnsvurderingMutation {
                sendTilGodkjenningV2(oppgavereferanse: "$oppgaveRef", vedtakUtfall: INNVILGELSE)
            }
        """
        )

        assertEquals(true, body.get("data")?.get("sendTilGodkjenningV2")?.asBoolean()) {
            "Uventet respons: $body"
        }
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
