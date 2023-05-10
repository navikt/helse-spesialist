package no.nav.helse.spesialist.api.endepunkter

import io.ktor.client.request.*
import io.ktor.http.*
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.spesialist.api.AbstractApiTest
import no.nav.helse.spesialist.api.SaksbehandlerMediator
import no.nav.helse.spesialist.api.utbetaling.AnnulleringDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS

@TestInstance(PER_CLASS)
internal class AnnulleringApiTest : AbstractApiTest() {

    private val SAKSBEHANDLER_OID = UUID.randomUUID()

    private lateinit var saksbehandlerMediator: SaksbehandlerMediator

    @BeforeAll
    fun setupTildeling() {
        saksbehandlerMediator = mockk(relaxed = true)
        setupServer {
            annulleringApi(saksbehandlerMediator)
        }
    }

    @AfterEach
    fun tearDownEach() {
        clearMocks(saksbehandlerMediator)
    }

    @Test
    fun annulleringOk() {
        val clot = slot<AnnulleringDto>()
        val response = runBlocking {
            client.post("/api/annullering") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(mapOf(
                    "aktørId" to "en-aktørid",
                    "fødselsnummer" to "et-fødselsnummer",
                    "organisasjonsnummer" to "et-organisasjonsnummer",
                    "fagsystemId" to "en-fagsystem-id",
                    "saksbehandlerIdent" to "Z999999",
                    "kommentar" to "Russekort",
                    "begrunnelser" to listOf("Ingen liker fisk", "En giraff!!")
                ))
                authentication(SAKSBEHANDLER_OID)
            }
        }
        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")
        verify(exactly = 1) {
            saksbehandlerMediator.håndter(capture(clot), any())
        }
        assertEquals("Russekort", clot.captured.kommentar)
        assertEquals(listOf("Ingen liker fisk", "En giraff!!"), clot.captured.begrunnelser)
    }

    @Test
    fun annulleringTommeVerdier() {
        val clot = slot<AnnulleringDto>()
        val response = runBlocking {
            client.preparePost("/api/annullering") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(mapOf(
                    "aktørId" to "en-aktørid",
                    "fødselsnummer" to "et-fødselsnummer",
                    "organisasjonsnummer" to "et-organisasjonsnummer",
                    "fagsystemId" to "en-fagsystem-id",
                    "saksbehandlerIdent" to "Z999999"
                ))
                authentication(SAKSBEHANDLER_OID)
            }.execute()
        }
        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")
        verify(exactly = 1) {
            saksbehandlerMediator.håndter(capture(clot), any())
        }
        assertEquals(null, clot.captured.kommentar)
        assertEquals(emptyList<String>(), clot.captured.begrunnelser)
    }
}
