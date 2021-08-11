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
import no.nav.helse.mediator.FeatureToggle
import no.nav.helse.mediator.api.AbstractApiTest.Companion.authentication
import no.nav.helse.mediator.api.AbstractApiTest.Companion.azureAdConfig
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

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
                authentication(UUID.randomUUID())
                setBody(objectMapper.writeValueAsString(overstyring))
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(1, testRapid.inspektør.hendelser("overstyr_tidslinje").size)
            }
        }
    }

    @Test
    fun `overstyr inntekt`() {
        FeatureToggle.Toggle("overstyr_inntekt").enable()
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
                månedligInntekt = 25000.0,
                skjæringstidspunkt = 1.januar
            )
            with(handleRequest(HttpMethod.Post, "/api/overstyr/inntekt") {
                addHeader(HttpHeaders.ContentType, "application/json")
                authentication(UUID.randomUUID())
                setBody(objectMapper.writeValueAsString(overstyring))
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(1, testRapid.inspektør.hendelser("overstyr_inntekt").size)
            }
        }
        FeatureToggle.Toggle("overstyr_inntekt").disable()
    }

    @Test
    fun `overstyr inntekt er togglet av`() {
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
                månedligInntekt = 25000.0,
                skjæringstidspunkt = 1.januar
            )
            with(handleRequest(HttpMethod.Post, "/api/overstyr/inntekt") {
                addHeader(HttpHeaders.ContentType, "application/json")
                authentication(UUID.randomUUID())
                setBody(objectMapper.writeValueAsString(overstyring))
            }) {
                assertEquals(HttpStatusCode.NotImplemented, response.status())
            }
        }
    }
}
