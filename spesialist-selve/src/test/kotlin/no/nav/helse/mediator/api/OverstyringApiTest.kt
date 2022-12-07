package no.nav.helse.mediator.api

import AbstractE2ETest
import ToggleHelpers.enable
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.TestRapidHelpers.hendelser
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.Testdata.ORGNR_GHOST
import no.nav.helse.Testdata.SAKSBEHANDLER_EPOST
import no.nav.helse.Testdata.SAKSBEHANDLER_IDENT
import no.nav.helse.Testdata.SAKSBEHANDLER_NAVN
import no.nav.helse.Testdata.SAKSBEHANDLER_OID
import no.nav.helse.azureAdAppAuthentication
import no.nav.helse.januar
import no.nav.helse.mediator.Toggle
import no.nav.helse.mediator.api.AbstractApiTest.Companion.authentication
import no.nav.helse.mediator.api.AbstractApiTest.Companion.azureAdConfig
import no.nav.helse.rapids_rivers.asLocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class OverstyringApiTest : AbstractE2ETest() {

    @Test
    fun `overstyr tidslinje`() {
        with(TestApplicationEngine()) {
            setUpApplication()
            val overstyring = OverstyrTidslinjeDTO(
                organisasjonsnummer = ORGNR,
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØR,
                begrunnelse = "en begrunnelse",
                dager = listOf(
                    OverstyrTidslinjeDTO.OverstyringdagDTO(dato = 10.januar, type = "Feriedag", fraType = "Sykedag", grad = null, fraGrad = 100)
                )
            )

            val response = runBlocking {
                client.post("/api/overstyr/dager") {
                    header(HttpHeaders.ContentType, "application/json")
                    authentication(
                        oid = SAKSBEHANDLER_OID,
                        epost = SAKSBEHANDLER_EPOST,
                        navn = SAKSBEHANDLER_NAVN,
                        ident = SAKSBEHANDLER_IDENT
                    )
                    setBody(objectMapper.writeValueAsString(overstyring))
                }
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(1, testRapid.inspektør.hendelser("saksbehandler_overstyrer_tidslinje").size)
        }
    }

    @Test
    fun `overstyr arbeidsforhold`() {
        with(TestApplicationEngine()) {
            setUpApplication()
            settOppBruker(orgnummereMedRelevanteArbeidsforhold = listOf(ORGNR_GHOST))
            val overstyring = OverstyrArbeidsforholdDto(
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØR,
                skjæringstidspunkt = 1.januar,
                overstyrteArbeidsforhold = listOf(
                    OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt(
                        orgnummer = ORGNR_GHOST,
                        deaktivert = true,
                        begrunnelse = "en begrunnelse",
                        forklaring = "en forklaring"
                    )
                )
            )

            val response = runBlocking {
                client.post("/api/overstyr/arbeidsforhold") {
                    header(HttpHeaders.ContentType, "application/json")
                    authentication(
                        oid = SAKSBEHANDLER_OID,
                        epost = SAKSBEHANDLER_EPOST,
                        navn = SAKSBEHANDLER_NAVN,
                        ident = SAKSBEHANDLER_IDENT
                    )
                    setBody(objectMapper.writeValueAsString(overstyring))
                }
            }

            assertEquals(HttpStatusCode.OK, response.status)

            assertEquals(1, testRapid.inspektør.hendelser("saksbehandler_overstyrer_arbeidsforhold").size)
            val event = testRapid.inspektør.hendelser("saksbehandler_overstyrer_arbeidsforhold").first()

            assertNotNull(event["@id"].asText())
            assertEquals(FØDSELSNUMMER, event["fødselsnummer"].asText())
            assertEquals(SAKSBEHANDLER_OID, event["saksbehandlerOid"].asText().let { UUID.fromString(it) })
            assertEquals(SAKSBEHANDLER_NAVN, event["saksbehandlerNavn"].asText())
            assertEquals(SAKSBEHANDLER_IDENT, event["saksbehandlerIdent"].asText())
            assertEquals(SAKSBEHANDLER_EPOST, event["saksbehandlerEpost"].asText())
            assertEquals(1.januar, event["skjæringstidspunkt"].asLocalDate())

            val overstyrtArbeidsforhold = event["overstyrteArbeidsforhold"].toList().single()
            assertEquals("en begrunnelse", overstyrtArbeidsforhold["begrunnelse"].asText())
            assertEquals("en forklaring", overstyrtArbeidsforhold["forklaring"].asText())
            assertEquals(ORGNR_GHOST, overstyrtArbeidsforhold["orgnummer"].asText())
            assertEquals(false, overstyrtArbeidsforhold["orgnummer"].asBoolean())
        }
    }

    @Test
    fun `overstyr inntekt`() {
        Toggle.Refusjonsendringer.enable()
        with(TestApplicationEngine()) {
            setUpApplication()

            val json = """
                {
                    "organisasjonsnummer": $ORGNR,
                    "fødselsnummer": $FØDSELSNUMMER,
                    "aktørId": $AKTØR,
                    "begrunnelse": "en begrunnelse",
                    "forklaring": "en forklaring",
                    "månedligInntekt": 25000.0,
                    "fraMånedligInntekt": 25001.0,
                    "skjæringstidspunkt": "2018-01-01",
                    "subsumsjon": {
                        "paragraf": "8-28",
                        "ledd": "3",
                        "bokstav": null
                    },
                    "refusjonsopplysninger": [
                        {
                        "fom": "2018-01-01",
                        "tom": "2018-01-31",
                        "beløp": 25000.0
                        },
                        {
                        "fom": "2018-02-01",
                        "tom": null,
                        "beløp": 24000.0
                        }
                    ]
                }
            """.trimIndent()

            val response = runBlocking {
                client.post("/api/overstyr/inntekt") {
                    header(HttpHeaders.ContentType, "application/json")
                    authentication(
                        oid = SAKSBEHANDLER_OID,
                        epost = SAKSBEHANDLER_EPOST,
                        navn = SAKSBEHANDLER_NAVN,
                        ident = SAKSBEHANDLER_IDENT
                    )
                    setBody(json)
                }
            }

            assertEquals(HttpStatusCode.OK, response.status)

            assertEquals(1, testRapid.inspektør.hendelser("saksbehandler_overstyrer_inntekt").size)
            val event = testRapid.inspektør.hendelser("saksbehandler_overstyrer_inntekt").first()

            assertNotNull(event["@id"].asText())
            assertEquals(FØDSELSNUMMER, event["fødselsnummer"].asText())
            assertEquals(SAKSBEHANDLER_OID, event["saksbehandlerOid"].asText().let { UUID.fromString(it) })
            assertEquals(SAKSBEHANDLER_NAVN, event["saksbehandlerNavn"].asText())
            assertEquals(SAKSBEHANDLER_IDENT, event["saksbehandlerIdent"].asText())
            assertEquals(SAKSBEHANDLER_EPOST, event["saksbehandlerEpost"].asText())
            assertEquals(ORGNR, event["organisasjonsnummer"].asText())
            assertEquals("en begrunnelse", event["begrunnelse"].asText())
            assertEquals("en forklaring", event["forklaring"].asText())
            assertEquals(25000.0, event["månedligInntekt"].asDouble())
            assertEquals(1.januar, event["skjæringstidspunkt"].asLocalDate())
            assertEquals("8-28", event["subsumsjon"]["paragraf"].asText())
            assertEquals("3", event["subsumsjon"]["ledd"].asText())
            assertNull(event["subsumsjon"]["bokstav"])
            assertEquals(2, event["refusjonsopplysninger"].size())
            assertEquals("2018-01-01", event["refusjonsopplysninger"].first()["fom"].asText())
            assertEquals("2018-01-31", event["refusjonsopplysninger"].first()["tom"].asText())
            assertEquals(25000.0, event["refusjonsopplysninger"].first()["beløp"].asDouble())
            assertNull(event["fraRefusjonsopplysninger"])
        }
    }

    private fun TestApplicationEngine.setUpApplication() {
        application.install(ContentNegotiation) {
            register(
                ContentType.Application.Json,
                JacksonConverter(objectMapper)
            )
        }
        application.azureAdAppAuthentication(azureAdConfig)
        application.routing {
            authenticate("oidc") {
                overstyringApi(hendelseMediator)
            }
        }
    }
}
