package no.nav.helse.mediator.api

import AbstractE2ETest
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import no.nav.helse.azureAdAppAuthentication
import no.nav.helse.januar
import no.nav.helse.mediator.api.AbstractApiTest.Companion.authentication
import no.nav.helse.mediator.api.AbstractApiTest.Companion.azureAdConfig
import no.nav.helse.rapids_rivers.asLocalDate
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class OverstyringApiTest : AbstractE2ETest() {

    @Test
    fun `overstyr tidslinje`() {
        withTestApplication({
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            azureAdAppAuthentication(azureAdConfig)
            routing {
                authenticate("oidc") {
                    overstyringApi(hendelseMediator)
                }

            }
        }) {
            val overstyring = OverstyrTidslinjeDTO(
                organisasjonsnummer = ORGNR,
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØR,
                begrunnelse = "en begrunnelse",
                dager = listOf(
                    OverstyrTidslinjeDTO.OverstyringdagDTO(dato = 10.januar, type = "Feriedag", grad = null)
                )
            )
            with(handleRequest(HttpMethod.Post, "/api/overstyr/dager") {
                addHeader(HttpHeaders.ContentType, "application/json")
                authentication(
                    oid = SAKSBEHANDLER_OID,
                    epost = SAKSBEHANDLER_EPOST,
                    navn = SAKSBEHANDLER_NAVN,
                    ident = SAKSBEHANDLER_IDENT
                )
                setBody(objectMapper.writeValueAsString(overstyring))
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(1, testRapid.inspektør.hendelser("overstyr_tidslinje").size)
            }
        }
    }

    @Test
    fun `overstyr arbeidsforhold`() {
        withTestApplication({
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            azureAdAppAuthentication(azureAdConfig)
            routing {
                authenticate("oidc") {
                    overstyringApi(hendelseMediator)
                }
            }
        }) {
            val overstyring = OverstyrArbeidsforholdDto(
                organisasjonsnummer = ORGNR,
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØR,
                skjæringstidspunkt = 1.januar,
                overstyrteArbeidsforhold = listOf(OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt(
                    orgnummer = "6667",
                    deaktivert = true,
                    begrunnelse = "en begrunnelse",
                    forklaring = "en forklaring"
                ))
            )
            with(handleRequest(HttpMethod.Post, "/api/overstyr/arbeidsforhold") {
                addHeader(HttpHeaders.ContentType, "application/json")
                authentication(
                    oid = SAKSBEHANDLER_OID,
                    epost = SAKSBEHANDLER_EPOST,
                    navn = SAKSBEHANDLER_NAVN,
                    ident = SAKSBEHANDLER_IDENT
                )
                setBody(objectMapper.writeValueAsString(overstyring))
            }) {
                assertEquals(HttpStatusCode.OK, response.status())

                assertEquals(1, testRapid.inspektør.hendelser("overstyr_arbeidsforhold").size)
                val event = testRapid.inspektør.hendelser("overstyr_arbeidsforhold").first()

                assertNotNull(event["@id"].asText())
                assertEquals(FØDSELSNUMMER, event["fødselsnummer"].asText())
                assertEquals(SAKSBEHANDLER_OID, event["saksbehandlerOid"].asText().let { UUID.fromString(it) })
                assertEquals(SAKSBEHANDLER_NAVN, event["saksbehandlerNavn"].asText())
                assertEquals(SAKSBEHANDLER_IDENT, event["saksbehandlerIdent"].asText())
                assertEquals(SAKSBEHANDLER_EPOST, event["saksbehandlerEpost"].asText())
                assertEquals(ORGNR, event["organisasjonsnummer"].asText())
                assertEquals(1.januar, event["skjæringstidspunkt"].asLocalDate())
                assertEquals("en begrunnelse", event["overstyrteArbeidsforhold"].toList().single()["begrunnelse"].asText())
                assertEquals("en forklaring", event["overstyrteArbeidsforhold"].toList().single()["forklaring"].asText())
                assertEquals("6667", event["overstyrteArbeidsforhold"].toList().single()["orgnummer"].asText())
                assertEquals(false, event["overstyrteArbeidsforhold"].toList().single()["orgnummer"].asBoolean())
            }
        }
    }

    @Test
    fun `overstyr inntekt`() {
        withTestApplication({
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            azureAdAppAuthentication(azureAdConfig)
            routing {
                authenticate("oidc") {
                    overstyringApi(hendelseMediator)
                }
            }
        }) {
            val overstyring = OverstyrInntektDTO(
                organisasjonsnummer = ORGNR,
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØR,
                begrunnelse = "en begrunnelse",
                forklaring = "en forklaring",
                månedligInntekt = 25000.0,
                skjæringstidspunkt = 1.januar
            )
            with(handleRequest(HttpMethod.Post, "/api/overstyr/inntekt") {
                addHeader(HttpHeaders.ContentType, "application/json")
                authentication(
                    oid = SAKSBEHANDLER_OID,
                    epost = SAKSBEHANDLER_EPOST,
                    navn = SAKSBEHANDLER_NAVN,
                    ident = SAKSBEHANDLER_IDENT
                )
                setBody(objectMapper.writeValueAsString(overstyring))
            }) {
                assertEquals(HttpStatusCode.OK, response.status())

                assertEquals(1, testRapid.inspektør.hendelser("overstyr_inntekt").size)
                val event = testRapid.inspektør.hendelser("overstyr_inntekt").first()

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
            }
        }
    }
}
