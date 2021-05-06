package no.nav.helse.mediator.api

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.server.testing.*
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.basicAuthentication
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.objectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

internal class AdminApiKtTest {

    private val mediator: HendelseMediator = mockk(relaxed = true)

    @Test
    fun `API-et kan formidle en melding til rapid-en`() {
        withTestApplication({
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            basicAuthentication("hunter2")
            adminApi(mediator)
        }) {
            with(handleRequest(HttpMethod.Post, "/admin/send") {
                val userpass = Base64.getEncoder().encodeToString("admin:hunter2".toByteArray())
                addHeader(HttpHeaders.Authorization, "Basic $userpass")
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody("""[{ "fødselsnummer": "fnr", "aktørId": "aktørId" }]""")
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                verify(exactly = 1) { mediator.sendMeldingPåTopic(any()) }
            }
        }
    }

    @Test
    fun `unau🅱️horized uten autentisering`() {
        withTestApplication({
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            basicAuthentication("hunter2")
            adminApi(mediator)
        }) {
            with(handleRequest(HttpMethod.Post, "/admin/send") {
                val userpass = Base64.getEncoder().encodeToString("admin:🅱️".toByteArray())
                addHeader(HttpHeaders.Authorization, "Basic $userpass")
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody("""[{ "fødselsnummer": "fnr", "aktørId": "aktørId" }]""")
            }) {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
                verify(exactly = 0) { mediator.sendMeldingPåTopic(any()) }
            }
        }
    }

    @AfterEach
    fun setupAfterEach() {
        clearMocks(mediator)
    }
}
