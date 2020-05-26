package no.nav.helse.api

import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.basicauthAuthenticaiton
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.objectMapper
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

internal class AdminApiKtTest {

    private val mediator: SpleisbehovMediator = mockk(relaxed = true)

    @Test
    fun `oppslag på rollback`() {
        withTestApplication({
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            basicauthAuthenticaiton("hunter2")
            adminApi(mediator)
        }) {
            with(handleRequest(HttpMethod.Post, "/admin/rollback") {
                val userpass = Base64.getEncoder().encodeToString("admin:hunter2".toByteArray())
                addHeader(HttpHeaders.Authorization, "Basic $userpass")
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody("""[{ "fødselsnummer": "fnr", "aktørId": "aktørId", "personVersjon": 4 }]""")
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                verify { mediator.rollbackPerson(any()) }
            }
        }
    }

    @Test
    fun `oppslag på rollback_delete`() {
        withTestApplication({
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            basicauthAuthenticaiton("hunter2")
            adminApi(mediator)
        }) {
            with(handleRequest(HttpMethod.Post, "/admin/rollback_delete") {
                val userpass = Base64.getEncoder().encodeToString("admin:hunter2".toByteArray())
                addHeader(HttpHeaders.Authorization, "Basic $userpass")
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody("""[{ "fødselsnummer": "fnr", "aktørId": "aktørId" }]""")
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                verify { mediator.rollbackDeletePerson(any()) }
            }
        }
    }

    @Test
    fun `unau🅱️horized uten autentisering`() {
        withTestApplication({
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            basicauthAuthenticaiton("hunter2")
            adminApi(mediator)
        }) {
            with(handleRequest(HttpMethod.Post, "/admin/rollback_delete") {
                val userpass = Base64.getEncoder().encodeToString("admin:🅱️".toByteArray())
                addHeader(HttpHeaders.Authorization, "Basic $userpass")
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody("""[{ "fødselsnummer": "fnr", "aktørId": "aktørId" }]""")
            }) {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
                verify(exactly = 0) { mediator.rollbackDeletePerson(any()) }
            }
        }
    }
}
