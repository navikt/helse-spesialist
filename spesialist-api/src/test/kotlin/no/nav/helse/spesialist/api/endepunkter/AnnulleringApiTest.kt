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
import no.nav.helse.spesialist.api.JwtStub
import no.nav.helse.spesialist.api.SaksbehandlerMediator
import no.nav.helse.spesialist.api.utbetaling.AnnulleringDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AnnulleringApiTest {

    private val SAKSBEHANDLER_OID = UUID.randomUUID()

    private val saksbehandlerMediator = mockk<SaksbehandlerMediator>(relaxed = true)
    private val jwtStub = JwtStub()
    private val apiTesting = ApiTesting(jwtStub) {
        annulleringApi(saksbehandlerMediator)
    }

    private fun HttpRequestBuilder.auth(oid: UUID, group: String? = null) = apiTesting.authentication(oid, group, this)

    @Test
    fun annulleringOk() {
        val slot = slot<AnnulleringDto>()
        apiTesting.spesialistApi { client ->
            val response = client.post("/api/annullering") {
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
            assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")
            verify(exactly = 1) {
                saksbehandlerMediator.håndter(capture(slot), any())
            }
            assertEquals("Russekort", slot.captured.kommentar)
            assertEquals(listOf("Ingen liker fisk", "En giraff!!"), slot.captured.begrunnelser)
        }
    }

    @Test
    fun annulleringTommeVerdier() {
        val slot = slot<AnnulleringDto>()
        apiTesting.spesialistApi { client ->
            val response = client.post("/api/annullering") {
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
            assertTrue(
                response.status.isSuccess(),
                "HTTP response burde returnere en OK-verdi, fikk ${response.status}"
            )
            verify(exactly = 1) {
                saksbehandlerMediator.håndter(capture(slot), any())
            }
            assertEquals(null, slot.captured.kommentar)
            assertEquals(emptyList<String>(), slot.captured.begrunnelser)
        }
    }
}
