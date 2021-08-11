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
import no.nav.helse.mediator.api.AbstractApiTest.Companion.authentication
import no.nav.helse.mediator.api.AbstractApiTest.Companion.azureAdConfig
import org.junit.jupiter.api.Test
import java.time.LocalDate
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
            val overstyring = OverstyringDTO(
                organisasjonsnummer = ORGNR,
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØR,
                begrunnelse = "en begrunnelse",
                dager = listOf(
                    OverstyringDTO.OverstyringdagDTO(dato = LocalDate.of(2018, 1, 10), type = "Feriedag", grad = null)
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
}
