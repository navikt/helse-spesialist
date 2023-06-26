package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.rapids_rivers.asOptionalLocalDateTime
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class NotatMutationTest : AbstractGraphQLApiTest() {

    @Test
    fun `feilregistrerer notat`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val notatId = opprettNotat()

        val body = runQuery(
            """
            mutation FeilregistrerNotat {
                feilregistrerNotat(id: $notatId) {
                    id
                }
            }
        """
        )

        assertEquals(1,body["data"]["feilregistrerNotat"]["id"].asInt())
    }

    @Test
    fun `feilregistrerer kommentar old`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val notatId = opprettNotat()!!
        val kommentarId = opprettKommentar(notatRef = notatId.toInt())

        val body = runQuery(
            """
            mutation FeilregistrerKommentar {
                feilregistrerKommentar(id: $kommentarId)
            }
        """
        )

        assertTrue(body["data"]["feilregistrerKommentar"].asBoolean())
    }
    @Test
    fun `feilregistrerer kommentar`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val notatId = opprettNotat()!!
        val kommentarId = opprettKommentar(notatRef = notatId.toInt())

        val body = runQuery(
            """
            mutation FeilregistrerKommentar {
                feilregistrerKommentarV2(id: $kommentarId) {
                    feilregistrert_tidspunkt
                }
            }
        """
        )

        assertNotNull(body["data"]["feilregistrerKommentarV2"]["feilregistrert_tidspunkt"].asOptionalLocalDateTime())
    }

    @Test
    fun `legger til notat`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val body = runQuery(
            """
            mutation LeggTilNotat {
                leggTilNotat(
                    tekst: "Dette er et notat",
                    type: Generelt,
                    vedtaksperiodeId: "${PERIODE.id}",
                    saksbehandlerOid: "${SAKSBEHANDLER.oid}"
                ) {
                    tekst
                }
            }
        """
        )

        assertEquals("Dette er et notat", body["data"]["leggTilNotat"]["tekst"].asText())
    }

    @Test
    fun `legger til ny kommentar`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val notatId = opprettNotat()

        val body = runQuery(
            """
            mutation LeggTilKommentar {
                leggTilKommentar(notatId: $notatId, tekst: "En kommentar", saksbehandlerident: "${SAKSBEHANDLER.ident}") {
                    tekst
                }
            }
        """
        )

        assertEquals("En kommentar", body["data"]["leggTilKommentar"]["tekst"].asText())
    }

    @Test
    fun `får 404-feil ved oppretting av kommentar dersom notat ikke finnes`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val body = runQuery(
            """
            mutation LeggTilKommentar {
                leggTilKommentar(notatId: 1, tekst: "En kommentar", saksbehandlerident: "${SAKSBEHANDLER.ident}") {
                    id
                }
            }
        """
        )

        assertEquals(404, body["errors"].first()["extensions"]["code"].asInt())
    }

}
