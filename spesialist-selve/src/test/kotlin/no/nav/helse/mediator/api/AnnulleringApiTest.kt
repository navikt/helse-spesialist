package no.nav.helse.mediator.api

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.helse.mediator.HendelseMediator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.util.*
import kotlin.test.assertTrue

@TestInstance(PER_CLASS)
internal class AnnulleringApiTest : AbstractApiTest() {

    private val SAKSBEHANDLER_OID = UUID.randomUUID()

    private lateinit var hendelseMediator: HendelseMediator

    @BeforeAll
    fun setupTildeling() {
        hendelseMediator = mockk(relaxed = true)
        setupServer {
            annulleringApi(hendelseMediator)
        }
    }

    @AfterEach
    fun tearDownEach() {
        clearMocks(hendelseMediator)
    }

    @Test
    fun annulleringOk() {
        val response = runBlocking {
            client.post<HttpResponse>("/api/annullering") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = mapOf(
                    "aktørId" to "en-aktørid",
                    "fødselsnummer" to "et-fødselsnummer",
                    "organisasjonsnummer" to "et-organisasjonsnummer",
                    "fagsystemId" to "en-fagsystem-id",
                    "saksbehandlerIdent" to "Z999999"
                )
                authentication(SAKSBEHANDLER_OID)
            }
        }
        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")
        verify(exactly = 1) {
            hendelseMediator.håndter(any<AnnulleringDto>(), any())
        }
    }

}
