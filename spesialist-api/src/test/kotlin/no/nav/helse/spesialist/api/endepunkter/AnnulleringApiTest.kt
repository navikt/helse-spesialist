package no.nav.helse.spesialist.api.endepunkter

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.UUID
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.saksbehandler.handlinger.AnnulleringHandling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AnnulleringApiTest {

    private val SAKSBEHANDLER_OID = UUID.randomUUID()

    private val saksbehandlerhåndterer = mockk<Saksbehandlerhåndterer>(relaxed = true)

    private val apiTesting = ApiTesting {
        annulleringApi(saksbehandlerhåndterer)
    }

    private fun HttpRequestBuilder.auth(oid: UUID, group: String? = null) = apiTesting.authentication(oid, group, this)

    @Test
    fun annulleringOk() {
        val slot = slot<AnnulleringHandling>()
        val response = apiTesting.spesialistApi { client ->
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
                auth(UUID.randomUUID())
            }
        }

        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK-verdi, fikk ${response.status}")
        verify(exactly = 1) { saksbehandlerhåndterer.håndter(capture(slot), any()) }
        assertEquals("Russekort", slot.captured.kommentar)
        assertEquals(listOf("Ingen liker fisk", "En giraff!!"), slot.captured.begrunnelser)
    }

    @Test
    fun annulleringTommeVerdier() {
        val slot = slot<AnnulleringHandling>()
        val response = apiTesting.spesialistApi { client ->
            client.post("/api/annullering") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(mapOf(
                    "aktørId" to "en-aktørid",
                    "fødselsnummer" to "et-fødselsnummer",
                    "organisasjonsnummer" to "et-organisasjonsnummer",
                    "fagsystemId" to "en-fagsystem-id",
                    "saksbehandlerIdent" to "Z999999"
                ))
                auth(SAKSBEHANDLER_OID)
            }
        }

        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK-verdi, fikk ${response.status}")
        verify(exactly = 1) { saksbehandlerhåndterer.håndter(capture(slot), any()) }
        assertEquals(null, slot.captured.kommentar)
        assertEquals(emptyList<String>(), slot.captured.begrunnelser)
    }
}
