package no.nav.helse.spesialist.api.graphql.query

import io.mockk.every
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.objectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DokumentQueryTest : AbstractGraphQLApiTest() {

    @Test
    fun `Får 400 dersom man gjør oppslag uten dokumentId`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val dokument = runQuery(
            """
            {
                hentSoknad(
                    dokumentId: ""
                    fnr: "$FØDSELSNUMMER"
                ) {
                    sendtNav
                }
            }
        """
        )["errors"].first()

        assertEquals(400, dokument["extensions"]["code"].asInt())
    }

    @Test
    fun `Får 403 dersom man gjør oppslag uten tilgang til person`() {
        val dokument = runQuery(
            """
            {
                hentSoknad(
                    dokumentId: "${UUID.randomUUID()}"
                    fnr: "$FØDSELSNUMMER"
                ) {
                    sendtNav
                }
            }
        """
        )["errors"].first()

        assertEquals(403, dokument["extensions"]["code"].asInt())
    }

    @Test
    fun `Får 408 dersom man ikke har fått søknaden etter 30 retries`() {
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        every { dokumenthåndterer.håndter(any(), any(), any()) } returns objectMapper.createObjectNode()
        val dokument = runQuery(
            """
            {
                hentSoknad(
                    dokumentId: "${UUID.randomUUID()}"
                    fnr: "$FØDSELSNUMMER"
                ) {
                    sendtNav
                }
            }
        """
        )["errors"].first()

        assertEquals(408, dokument["extensions"]["code"].asInt())
    }

    @Test
    fun `hentSoknad query med riktige tilganger og paramtetre returnerer søknad`() {
        val dokumentId = UUID.randomUUID()
        val søknadstidspunkt = LocalDateTime.now().toString()
        val arbeidGjenopptatt = LocalDate.now().toString()
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        every { dokumenthåndterer.håndter(any(), any(), any()) } returns objectMapper.readTree(søknadJson(søknadstidspunkt, arbeidGjenopptatt))
        val dokument = runQuery(
            """
            {
                hentSoknad(
                    dokumentId: "$dokumentId"
                    fnr: "$FØDSELSNUMMER"
                ) {
                    sendtNav, arbeidGjenopptatt, egenmeldingsperioder { 
                        fom,tom
                    }, fravarsperioder {fom, tom, fravarstype
                    }
                }
            }
        """
        )["data"]["hentSoknad"]

        verify(exactly = 1) { dokumenthåndterer.håndter(
            fødselsnummer = FØDSELSNUMMER,
            dokumentId = dokumentId,
            dokumentType = DokumentType.SØKNAD.name
        ) }

        assertEquals(4, dokument.size())
        assertEquals(søknadstidspunkt, dokument["sendtNav"].asText())
        assertEquals(arbeidGjenopptatt, dokument["arbeidGjenopptatt"].asText())
        val egenmeldingsperioderJson = dokument["egenmeldingsperioder"]
        assertEquals(1, egenmeldingsperioderJson.size())
        val fravarsperioderJson = dokument["fravarsperioder"]
        assertEquals(1, fravarsperioderJson.size())
        val egenmeldingsperiodeJson = dokument["egenmeldingsperioder"].single()
        assertEquals("2018-01-01", egenmeldingsperiodeJson["fom"].asText())
        assertEquals("2018-01-31", egenmeldingsperiodeJson["tom"].asText())
        val fravarsperiodeJson = dokument["fravarsperioder"].single()
        assertEquals("2018-01-01", fravarsperiodeJson["fom"].asText())
        assertEquals("2018-01-31", fravarsperiodeJson["tom"].asText())
        assertEquals("FERIE", fravarsperiodeJson["fravarstype"].asText())
    }

    @Language("JSON")
    private fun søknadJson(søknadstidspunkt: String, arbeidGjenopptatt: String) = """{
  "sendtNav": "$søknadstidspunkt",
  "arbeidGjenopptatt": "$arbeidGjenopptatt",
  "egenmeldinger": [
  {
  "fom": "2018-01-01",
  "tom": "2018-01-31"
  }
  ],
  "fravar": [
    {
    "fom": "2018-01-01",
    "tom": "2018-01-31",
    "type": "FERIE"
    }
  ]
}
""".trimIndent()

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