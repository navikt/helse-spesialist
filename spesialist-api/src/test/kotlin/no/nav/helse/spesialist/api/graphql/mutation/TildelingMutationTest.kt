package no.nav.helse.spesialist.api.graphql.mutation

import com.fasterxml.jackson.module.kotlin.contains
import java.util.UUID
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import org.junit.jupiter.api.Assertions.assertEquals
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
                        saksbehandlerreferanse: "${SAKSBEHANDLER.oid}",
                        epostadresse: "${SAKSBEHANDLER.epost}",
                        navn: "${SAKSBEHANDLER.navn}",
                        ident: "${SAKSBEHANDLER.ident}"
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
                        saksbehandlerreferanse: "${SAKSBEHANDLER.oid}",
                        epostadresse: "${SAKSBEHANDLER.epost}",
                        navn: "${SAKSBEHANDLER.navn}",
                        ident: "${SAKSBEHANDLER.ident}"
                    ) {
                        navn, oid, epost, reservert
                    }
                }
            """
        )

        assertTrue(body.contains("errors"))
    }
}
