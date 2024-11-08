package no.nav.helse.spesialist.api.graphql.mutation

import com.github.navikt.tbd_libs.jackson.asLocalDateTimeOrNull
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class NotatMutationTest : AbstractGraphQLApiTest() {
    @Test
    fun `feilregistrerer notat`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val notatId = opprettNotat()

        val body =
            runQuery(
                """
            mutation FeilregistrerNotat {
                feilregistrerNotat(id: $notatId) {
                    id
                }
            }
        """,
            )

        assertEquals(notatId, body["data"]["feilregistrerNotat"]["id"].asLong())
    }

    @Test
    fun `feilregistrerer kommentar`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val notatId = opprettNotat()!!
        val kommentarId = opprettKommentar(notatRef = notatId.toInt())

        val body =
            runQuery(
                """
            mutation FeilregistrerKommentar {
                feilregistrerKommentar(id: $kommentarId) {
                    feilregistrert_tidspunkt
                }
            }
        """,
            )

        assertNotNull(body["data"]["feilregistrerKommentar"]["feilregistrert_tidspunkt"].asLocalDateTimeOrNull())
    }

    @Test
    fun `legger til notat`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())

        val body =
            runQuery(
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
        """,
            )

        assertEquals("Dette er et notat", body["data"]["leggTilNotat"]["tekst"].asText())
    }

    @Test
    fun `legger til ny kommentar`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val notatId = opprettNotat()

        val body =
            runQuery(
                """
            mutation LeggTilKommentar {
                leggTilKommentar(notatId: $notatId, tekst: "En kommentar", saksbehandlerident: "${SAKSBEHANDLER.ident}") {
                    tekst
                }
            }
        """,
            )

        assertEquals("En kommentar", body["data"]["leggTilKommentar"]["tekst"].asText())
    }

    @Test
    fun `legger til kommentar med dialogRef`() {
        opprettSaksbehandler()
        val dialogRef = opprettDialog()

        val body =
            runQuery(
                """
            mutation LeggTilKommentarMedDialogRef {
                leggTilKommentarMedDialogRef(dialogRef: $dialogRef, tekst: "En kommentar", saksbehandlerident: "${SAKSBEHANDLER.ident}") {
                    tekst
                }
            }
        """,
            )

        assertEquals("En kommentar", body["data"]["leggTilKommentarMedDialogRef"]["tekst"].asText())
    }
}
