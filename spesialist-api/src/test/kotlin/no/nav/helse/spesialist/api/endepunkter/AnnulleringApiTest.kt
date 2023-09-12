package no.nav.helse.spesialist.api.endepunkter

import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.spesialist.api.AbstractApiTest
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
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

    private lateinit var saksbehandlerhåndterer: Saksbehandlerhåndterer

    @BeforeAll
    fun setupTildeling() {
        saksbehandlerhåndterer = mockk(relaxed = true)
        setupServer {
            annulleringApi(saksbehandlerhåndterer)
        }
    }

    @AfterEach
    fun tearDownEach() {
        clearMocks(saksbehandlerhåndterer)
    }

    @Test
    fun annulleringOk() {
        val slot = slot<AnnulleringDto>()
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
            saksbehandlerhåndterer.håndter(capture(slot), any())
        }
        assertEquals("Russekort", slot.captured.kommentar)
        assertEquals(listOf("Ingen liker fisk", "En giraff!!"), slot.captured.begrunnelser)
    }

    @Test
    fun annulleringTommeVerdier() {
        val slot = slot<AnnulleringDto>()
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
            saksbehandlerhåndterer.håndter(capture(slot), any())
        }
        assertEquals(null, slot.captured.kommentar)
        assertEquals(emptyList<String>(), slot.captured.begrunnelser)
    }
}
