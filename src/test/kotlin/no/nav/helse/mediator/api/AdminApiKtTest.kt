package no.nav.helse.mediator.api

import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.server.testing.*
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.basicAuthentication
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.api.ApiTestUtil.Companion.authentication
import no.nav.helse.objectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

internal class AdminApiKtTest {

    private val mediator: HendelseMediator = mockk(relaxed = true)

    @Test
    fun `oppslag på rollback`() {
        val response = ApiTestUtil.TestMockServer{
            this.install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            this.adminRoute(mediator)
        }.withAuthenticatedServer {client ->
            client.post<HttpResponse>("/admin/rollback") {
                val userpass = Base64.getEncoder().encodeToString("admin:hunter2".toByteArray())
                header(HttpHeaders.Authorization, "Basic $userpass")
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = """[{ "fødselsnummer": "fnr", "aktørId": "aktørId", "personVersjon": 4 }]"""
//                authentication(SAKSBEHANDLER_OID)
            }
        }
        assertEquals(HttpStatusCode.OK, response.status)
                verify(exactly = 1) { mediator.håndter(TilbakerullingDTO("fnr", "aktørId", 4L)) }
//        withTestApplication({
//            this.install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
//            basicAuthentication("hunter2")
//            adminApi(mediator)
//        }) {
//            with(handleRequest(HttpMethod.Post, "/admin/rollback") {
//                val userpass = Base64.getEncoder().encodeToString("admin:hunter2".toByteArray())
//                addHeader(HttpHeaders.Authorization, "Basic $userpass")
//                addHeader(HttpHeaders.ContentType, "application/json")
//                setBody("""[{ "fødselsnummer": "fnr", "aktørId": "aktørId", "personVersjon": 4 }]""")
//            }) {
//                assertEquals(HttpStatusCode.OK, response.status())
//                verify(exactly = 1) { mediator.håndter(TilbakerullingDTO("fnr", "aktørId", 4L)) }
//            }
//        }
    }

    @Test
    fun `oppslag på rollback_delete`() {
        withTestApplication({
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            basicAuthentication("hunter2")
            adminApi(mediator)
        }) {
            with(handleRequest(HttpMethod.Post, "/admin/rollback_delete") {
                val userpass = Base64.getEncoder().encodeToString("admin:hunter2".toByteArray())
                addHeader(HttpHeaders.Authorization, "Basic $userpass")
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody("""[{ "fødselsnummer": "fnr", "aktørId": "aktørId" }]""")
            }) {
                assertEquals(HttpStatusCode.OK, response.status())
                verify(exactly = 1) { mediator.håndter(TilbakerullingMedSlettingDTO("fnr", "aktørId")) }
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
            with(handleRequest(HttpMethod.Post, "/admin/rollback_delete") {
                val userpass = Base64.getEncoder().encodeToString("admin:🅱️".toByteArray())
                addHeader(HttpHeaders.Authorization, "Basic $userpass")
                addHeader(HttpHeaders.ContentType, "application/json")
                setBody("""[{ "fødselsnummer": "fnr", "aktørId": "aktørId" }]""")
            }) {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
                verify(exactly = 0) { mediator.håndter(any<TilbakerullingMedSlettingDTO>()) }
            }
        }
    }

    @AfterEach
    fun setupAfterEach() {
        clearMocks(mediator)
    }
}
