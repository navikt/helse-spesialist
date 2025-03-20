package no.nav.helse.spesialist.api.graphql.mutation

import io.mockk.every
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.SendIReturResult
import no.nav.helse.spesialist.api.SendTilGodkjenningResult
import no.nav.helse.spesialist.api.testfixtures.mutation.sendIReturMutation
import no.nav.helse.spesialist.api.testfixtures.mutation.sendTilbeslutterMutation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TotrinnsvurderingMutationHandlerTest : AbstractGraphQLApiTest() {

    @Test
    fun `send oppgave til godkjenning med V2`() {
        every { saksbehandlerMediator.håndterTotrinnsvurdering(any(), any(), any()) }.returns(
            SendTilGodkjenningResult.Ok
        )
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val oppgaveRef = finnOppgaveIdFor(PERIODE.id)

        val body = runQuery(sendTilbeslutterMutation(oppgaveRef))

        assertEquals(true, body.get("data")?.get("sendTilGodkjenningV2")?.asBoolean()) {
            "Uventet respons: $body"
        }
    }

    @Test
    fun `send oppgave i retur`() {
        every { saksbehandlerMediator.sendIRetur(any(), any(), any()) }.returns(
            SendIReturResult.Ok
        )

        val body = runQuery(
            sendIReturMutation(1, """Retur""")
        )

        assertTrue(body["data"]["sendIRetur"].asBoolean()) { body.toString() }
    }
}
