package no.nav.helse.mediator.api

import AbstractE2ETest
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
import no.nav.helse.januar
import no.nav.helse.mediator.api.AbstractApiTest.Companion.authentication
import no.nav.helse.mediator.api.AbstractApiTest.Companion.azureAdAppConfig
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spesialist.api.azureAdAppAuthentication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
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
    fun `overstyr tidslinje til arbeidsdag`() {
        with(TestApplicationEngine()) {
            setUpApplication()
            val overstyring = OverstyrTidslinjeDTO(
                organisasjonsnummer = ORGNR,
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØR,
                begrunnelse = "en begrunnelse",
                dager = listOf(
                    OverstyrTidslinjeDTO.OverstyringdagDTO(dato = 10.januar, type = "Arbeidsdag", fraType = "Sykedag", grad = null, fraGrad = 100)
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
    fun `overstyr tidslinje fra arbeidsdag`() {
        with(TestApplicationEngine()) {
            setUpApplication()
            val overstyring = OverstyrTidslinjeDTO(
                organisasjonsnummer = ORGNR,
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØR,
                begrunnelse = "en begrunnelse",
                dager = listOf(
                    OverstyrTidslinjeDTO.OverstyringdagDTO(dato = 10.januar, type = "Sykedag", fraType = "Arbeidsdag", grad = null, fraGrad = 100)
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
    fun `overstyr inntekt og refusjon`() {
        with(TestApplicationEngine()) {
            setUpApplication()

            val json = """
                {
                    "fødselsnummer": $FØDSELSNUMMER,
                    "aktørId": $AKTØR,
                    "skjæringstidspunkt": "2018-01-01",
                    "arbeidsgivere": [{
                        "organisasjonsnummer": $ORGNR,
                        "månedligInntekt": 25000.0,
                        "fraMånedligInntekt": 25001.0,
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
                        ],                        
                        "fraRefusjonsopplysninger": [
                            {
                            "fom": "2018-01-01",
                            "tom": "2018-01-31",
                            "beløp": 24000.0
                            },
                            {
                            "fom": "2018-02-01",
                            "tom": null,
                            "beløp": 23000.0
                            }
                        ],
                        "begrunnelse": "en begrunnelse",
                        "forklaring": "en forklaring",
                        "subsumsjon": {
                            "paragraf": "8-28",
                            "ledd": "3",
                            "bokstav": null
                        }
                    },{
                        "organisasjonsnummer": "666",
                        "månedligInntekt": 21000.0,
                        "fraMånedligInntekt": 25001.0,
                        "refusjonsopplysninger": [
                            {
                            "fom": "2018-01-01",
                            "tom": "2018-01-31",
                            "beløp": 21000.0
                            },
                            {
                            "fom": "2018-02-01",
                            "tom": null,
                            "beløp": 22000.0
                            }
                        ],                        
                        "fraRefusjonsopplysninger": [
                            {
                            "fom": "2018-01-01",
                            "tom": "2018-01-31",
                            "beløp": 22000.0
                            },
                            {
                            "fom": "2018-02-01",
                            "tom": null,
                            "beløp": 23000.0
                            }
                        ],
                        "begrunnelse": "en begrunnelse 2",
                        "forklaring": "en forklaring 2",
                        "subsumsjon": {
                            "paragraf": "8-28",
                            "ledd": "3",
                            "bokstav": null
                        }
                    }]
                }
            """.trimIndent()

            val response = runBlocking {
                client.post("/api/overstyr/inntektogrefusjon") {
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

            assertEquals(1, testRapid.inspektør.hendelser("saksbehandler_overstyrer_inntekt_og_refusjon").size)
            val event = testRapid.inspektør.hendelser("saksbehandler_overstyrer_inntekt_og_refusjon").first()

            assertNotNull(event["@id"].asText())
            assertEquals(FØDSELSNUMMER, event["fødselsnummer"].asText())
            assertEquals(SAKSBEHANDLER_OID, event["saksbehandlerOid"].asText().let { UUID.fromString(it) })
            assertEquals(SAKSBEHANDLER_NAVN, event["saksbehandlerNavn"].asText())
            assertEquals(SAKSBEHANDLER_IDENT, event["saksbehandlerIdent"].asText())
            assertEquals(SAKSBEHANDLER_EPOST, event["saksbehandlerEpost"].asText())
            assertEquals(1.januar, event["skjæringstidspunkt"].asLocalDate())
            event["arbeidsgivere"].first().let {
                assertEquals(ORGNR, it["organisasjonsnummer"].asText())
                assertEquals("en begrunnelse", it["begrunnelse"].asText())
                assertEquals("en forklaring", it["forklaring"].asText())
                assertEquals(25000.0, it["månedligInntekt"].asDouble())
                assertEquals("8-28", it["subsumsjon"]["paragraf"].asText())
                assertEquals("3", it["subsumsjon"]["ledd"].asText())
                assertTrue(it["subsumsjon"]["bokstav"].isNull)
                assertEquals(2, it["refusjonsopplysninger"].size())
                assertEquals("2018-01-01", it["refusjonsopplysninger"].first()["fom"].asText())
                assertEquals("2018-01-31", it["refusjonsopplysninger"].first()["tom"].asText())
                assertEquals(25000.0, it["refusjonsopplysninger"].first()["beløp"].asDouble())
                assertEquals(24000.0, it["fraRefusjonsopplysninger"].first()["beløp"].asDouble())
            }
            event["arbeidsgivere"].last().let {
                assertEquals("666", it["organisasjonsnummer"].asText())
                assertEquals("en begrunnelse 2", it["begrunnelse"].asText())
                assertEquals("en forklaring 2", it["forklaring"].asText())
                assertEquals(21000.0, it["månedligInntekt"].asDouble())
                assertEquals("8-28", it["subsumsjon"]["paragraf"].asText())
                assertEquals("3", it["subsumsjon"]["ledd"].asText())
                assertTrue(it["subsumsjon"]["bokstav"].isNull)
                assertEquals(2, it["refusjonsopplysninger"].size())
                assertEquals("2018-01-01", it["refusjonsopplysninger"].first()["fom"].asText())
                assertEquals("2018-01-31", it["refusjonsopplysninger"].first()["tom"].asText())
                assertEquals(21000.0, it["refusjonsopplysninger"].first()["beløp"].asDouble())
                assertEquals(22000.0, it["fraRefusjonsopplysninger"].first()["beløp"].asDouble())
            }
        }
    }

    private fun TestApplicationEngine.setUpApplication() {
        application.install(ContentNegotiation) {
            register(
                ContentType.Application.Json,
                JacksonConverter(objectMapper)
            )
        }
        application.azureAdAppAuthentication(azureAdAppConfig)
        application.routing {
            authenticate("oidc") {
                overstyringApi(hendelseMediator)
            }
        }
    }
}
