package no.nav.helse.spesialist.api.graphql.query

import java.util.UUID
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class NotatQueryTest : AbstractGraphQLApiTest() {

    @Test
    fun `henter notater`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettNotat("Et notat")
        opprettNotat("Et annet notat")

        val notater = runQuery(
            """
            {
                notater(forPerioder: ["${PERIODE.id}"]) {
                    id
                    notater {
                        id
                        tekst
                        type
                        opprettet
                        vedtaksperiodeId
                        feilregistrert_tidspunkt
                        feilregistrert
                        kommentarer {
                            feilregistrert_tidspunkt
                            opprettet
                            tekst
                            id
                            saksbehandlerident
                        }
                        saksbehandlerEpost
                        saksbehandlerIdent
                        saksbehandlerNavn
                        saksbehandlerOid
                    }
                }
            }
        """
        )["data"]["notater"].first()

        assertEquals(PERIODE.id.toString(), notater["id"].asText())
        assertEquals(2, notater["notater"].size())
        assertTrue(notater["notater"].any { it["tekst"].asText() == "Et notat" })
        assertTrue(notater["notater"].any { it["tekst"].asText() == "Et annet notat" })
    }

    @Test
    fun `henter kun notater for gitte perioder`() {
        val personId = opprettPerson()
        val arbeidsgiverId = opprettArbeidsgiver()
        val førstePeriode = UUID.randomUUID()
        val andrePeriode = UUID.randomUUID()
        opprettSaksbehandler()
        opprettVedtaksperiode(personId, arbeidsgiverId, periode = Periode(førstePeriode, PERIODE.fom, PERIODE.tom))
        opprettVedtaksperiode(personId, arbeidsgiverId, periode = Periode(andrePeriode, PERIODE.fom, PERIODE.tom))
        opprettNotat(tekst = "Et notat", vedtaksperiodeId = førstePeriode)
        opprettNotat(tekst = "Et annet notat", vedtaksperiodeId = førstePeriode)
        opprettNotat(tekst = "Et tredje notat", vedtaksperiodeId = andrePeriode)

        val notater = runQuery(
            """
            {
                notater(forPerioder: ["$førstePeriode"]) {
                    id
                    notater {
                        id
                        tekst
                        type
                        opprettet
                        vedtaksperiodeId
                        feilregistrert_tidspunkt
                        feilregistrert
                        kommentarer {
                            feilregistrert_tidspunkt
                            opprettet
                            tekst
                            id
                            saksbehandlerident
                        }
                        saksbehandlerEpost
                        saksbehandlerIdent
                        saksbehandlerNavn
                        saksbehandlerOid
                    }
                }
            }
        """
        )["data"]["notater"]

        assertEquals(1, notater.size())
        assertEquals(førstePeriode.toString(), notater.first()["id"].asText())
        assertTrue(notater.first()["notater"].none { it["tekst"].asText() == "Et tredje notat" })
    }

}