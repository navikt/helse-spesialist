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
import org.junit.jupiter.api.Assertions.assertNull
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
                    sykmeldingSkrevet
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
                    sykmeldingSkrevet
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
                    sykmeldingSkrevet
                }
            }
        """
        )["errors"].first()

        assertEquals(408, dokument["extensions"]["code"].asInt())
    }

    @Test
    fun `hentSoknad query med riktige tilganger og paramtetre returnerer søknad`() {
        val dokumentId = UUID.randomUUID()
        val arbeidGjenopptatt = LocalDate.now().toString()
        val sykmeldingSkrevet = LocalDateTime.now().toString()


        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        every { dokumenthåndterer.håndter(any(), any(), any()) } returns objectMapper.readTree(
            søknadJson(
                arbeidGjenopptatt,
                sykmeldingSkrevet
            )
        )
        val dokument = runQuery(
            """
            {
                hentSoknad(
                    dokumentId: "$dokumentId"
                    fnr: "$FØDSELSNUMMER"
                ) {
                    sykmeldingSkrevet, arbeidGjenopptatt, egenmeldingsdagerFraSykmelding, soknadsperioder {
                    fom, tom, grad, faktiskGrad
                    }
                }
            }
        """
        )["data"]["hentSoknad"]

        verify(exactly = 1) {
            dokumenthåndterer.håndter(
                fødselsnummer = FØDSELSNUMMER,
                dokumentId = dokumentId,
                dokumentType = DokumentType.SØKNAD.name
            )
        }

        assertEquals(4, dokument.size())
        assertEquals(arbeidGjenopptatt, dokument["arbeidGjenopptatt"].asText())
        assertEquals(sykmeldingSkrevet, dokument["sykmeldingSkrevet"].asText())
        assertEquals("2018-01-01", dokument["egenmeldingsdagerFraSykmelding"].first().asText())
        val hentetSoknadsperioder = dokument["soknadsperioder"].single()
        assertEquals("2018-01-01", hentetSoknadsperioder["fom"].asText())
        assertEquals("2018-01-31", hentetSoknadsperioder["tom"].asText())
        assertEquals(100, hentetSoknadsperioder["grad"].asInt())
        assertTrue(hentetSoknadsperioder["faktiskGrad"].isNull)
    }

    @Language("JSON")
    private fun søknadJson(arbeidGjenopptatt: String, sykmeldingSkrevet: String) = """{
  "arbeidGjenopptatt": "$arbeidGjenopptatt",
  "sykmeldingSkrevet": "$sykmeldingSkrevet",
  "egenmeldingsdagerFraSykmelding": ["2018-01-01"],
  "soknadsperioder": [{"fom": "2018-01-01", "tom": "2018-01-31", "grad": 100, "faktiskGrad": null}]
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