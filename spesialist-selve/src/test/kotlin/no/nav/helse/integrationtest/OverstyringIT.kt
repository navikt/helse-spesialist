package no.nav.helse.integrationtest

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
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
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
import no.nav.helse.mediator.api.AbstractApiTest
import no.nav.helse.mediator.api.AbstractApiTest.Companion.authentication
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto
import no.nav.helse.mediator.api.OverstyrTidslinjeDTO
import no.nav.helse.mediator.api.overstyringApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tester samspillet mellom API og selve, altså "integrasjonen" mellom dem 😀
 */
internal class OverstyringIT : AbstractE2ETest() {

    @Test
    fun `overstyr tidslinje`() {
        with(TestApplicationEngine()) {
            setUpApplication()
            settOppBruker()
            assertOppgaver(1)
            val overstyring = OverstyrTidslinjeDTO(
                organisasjonsnummer = ORGNR,
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØR,
                begrunnelse = "en begrunnelse",
                dager = listOf(
                    OverstyrTidslinjeDTO.OverstyringdagDTO(dato = 10.januar, type = "Feriedag", fraType = "Sykedag", grad = null, fraGrad = 100)
                ),
            )

            val response = runBlocking {
                client.post("/api/overstyr/dager") {
                    header(HttpHeaders.ContentType, "application/json")
                    authentication(
                        oid = SAKSBEHANDLER_OID,
                        epost = SAKSBEHANDLER_EPOST,
                        navn = SAKSBEHANDLER_NAVN,
                        ident = SAKSBEHANDLER_IDENT,
                    )
                    setBody(objectMapper.writeValueAsString(overstyring))
                }
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(1, testRapid.inspektør.hendelser("saksbehandler_overstyrer_tidslinje").size)
            testRapid.sendTestMessage(
                testRapid.inspektør.hendelser("saksbehandler_overstyrer_tidslinje").first().toString()
            )
            assertEquals("Invalidert", oppgaveStatus())
            assertEquals(1, testRapid.inspektør.hendelser("overstyr_tidslinje").size)
        }
    }

    @Test
    fun `overstyr inntekt`() {
        with(TestApplicationEngine()) {
            setUpApplication()
            settOppBruker()

            val json = """
                {
                    "organisasjonsnummer": $ORGNR,
                    "fødselsnummer": $FØDSELSNUMMER,
                    "aktørId": $AKTØR,
                    "begrunnelse": "en begrunnelse",
                    "forklaring": "en forklaring",
                    "månedligInntekt": 25000.0,
                    "fraMånedligInntekt": 25001.0,
                    "skjæringstidspunkt": "2018-01-01"
                }
            """.trimIndent()

            val response = runBlocking {
                client.post("/api/overstyr/inntekt") {
                    header(HttpHeaders.ContentType, "application/json")
                    authentication(
                        oid = SAKSBEHANDLER_OID,
                        epost = SAKSBEHANDLER_EPOST,
                        navn = SAKSBEHANDLER_NAVN,
                        ident = SAKSBEHANDLER_IDENT,
                    )
                    setBody(json)
                }
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(1, testRapid.inspektør.hendelser("saksbehandler_overstyrer_inntekt").size)
            testRapid.sendTestMessage(
                testRapid.inspektør.hendelser("saksbehandler_overstyrer_inntekt").first().toString()
            )
            assertEquals("Invalidert", oppgaveStatus())
            assertEquals(1, testRapid.inspektør.hendelser("overstyr_inntekt").size)
        }
    }

    @Test
    fun `overstyr inntekt med refusjon`() {
        with(TestApplicationEngine()) {
            setUpApplication()
            settOppBruker()

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
                    "refusjonsopplysninger": [
                        {
                            "fom": "2018-01-01",
                            "tom": "2018-01-31",
                            "beløp": 25000.0
                        },
                        {
                            "fom": "2018-02-01",
                            "tom": null,
                            "beløp": 25000.0
                        }
                    ],
                    "fraRefusjonsopplysninger": [
                        {
                            "fom": "2018-01-01",
                            "tom": "2018-01-31",
                            "beløp": 25001.0
                        },
                        {
                            "fom": "2018-02-01",
                            "tom": null,
                            "beløp": 25001.0
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
                        ident = SAKSBEHANDLER_IDENT,
                    )
                    setBody(json)
                }
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(1, testRapid.inspektør.hendelser("saksbehandler_overstyrer_inntekt").size)
            testRapid.sendTestMessage(
                testRapid.inspektør.hendelser("saksbehandler_overstyrer_inntekt").first().toString()
            )

            //TODO: Bruk OverstyringApiDao når denne er oppdatert til å inkludere nye kolonner
            val refusjonsopplysninger = overstyringInntektRefusjonsopplysninger("refusjonsopplysninger")
            val fraRefusjonsopplysninger = overstyringInntektRefusjonsopplysninger("fra_refusjonsopplysninger")

            assertTrue(refusjonsopplysninger?.isNotEmpty() == true)
            assertTrue(fraRefusjonsopplysninger?.isNotEmpty() == true)

            assertEquals("Invalidert", oppgaveStatus())
            assertEquals(1, testRapid.inspektør.hendelser("overstyr_inntekt").size)
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
                        forklaring = "en forklaring",
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
                        ident = SAKSBEHANDLER_IDENT,
                    )
                    setBody(objectMapper.writeValueAsString(overstyring))
                }
            }


            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(1, testRapid.inspektør.hendelser("saksbehandler_overstyrer_arbeidsforhold").size)
            testRapid.sendTestMessage(
                testRapid.inspektør.hendelser("saksbehandler_overstyrer_arbeidsforhold").first().toString()
            )
            assertEquals("Invalidert", oppgaveStatus())
            assertEquals(1, testRapid.inspektør.hendelser("overstyr_arbeidsforhold").size)
        }
    }

    private fun overstyringInntektRefusjonsopplysninger(column: String) =
        sessionOf(dataSource).use { session ->
            session.run(queryOf("SELECT * FROM overstyring_inntekt").map {
                it.stringOrNull(column)
            }.asSingle)
        }

    private fun oppgaveStatus() =
        sessionOf(dataSource).use { session ->
            session.run(queryOf("SELECT * FROM oppgave ORDER BY id DESC").map {
                it.string("status")
            }.asSingle)
        }

    private fun TestApplicationEngine.setUpApplication() {
        application.install(ContentNegotiation) {
            register(
                ContentType.Application.Json,
                JacksonConverter(objectMapper),
            )
        }
        application.azureAdAppAuthentication(AbstractApiTest.azureAdConfig)
        application.routing {
            authenticate("oidc") {
                overstyringApi(hendelseMediator)
            }
        }
    }
}
