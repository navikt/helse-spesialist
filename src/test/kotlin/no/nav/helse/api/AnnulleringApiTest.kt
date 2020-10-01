package no.nav.helse.api

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.helse.annullering.AnnulleringMediator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.test.assertTrue

class AnnulleringApiTest : AbstractApiTest() {

    private val SAKSBEHANDLER_OID = UUID.randomUUID()

    private lateinit var annulleringMediator: AnnulleringMediator

    @BeforeAll
    fun setupTildeling() {
        annulleringMediator = mockk(relaxed = true)
        setupServer {
            annulleringApi(annulleringMediator)
        }
    }

    @AfterEach
    fun tearDownEach() {
        clearMocks(annulleringMediator)
    }

    @Test
    fun annulleringOk() {
        val response = runBlocking {
            client.post<HttpResponse>("/api/v1/annullering") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = mapOf(
                    "aktørId" to "en-aktørid",
                    "fødselsnummer" to "et-fødselsnummer",
                    "organisasjonsnummer" to "et-organisasjonsnummer",
                    "dager" to listOf<LocalDate>(
                        LocalDate.of(2020, 1, 1),
                        LocalDate.of(2020, 1, 2),
                        LocalDate.of(2020, 1, 3),
                        LocalDate.of(2020, 1, 4),
                        LocalDate.of(2020, 1, 5),
                        LocalDate.of(2020, 1, 6),
                        LocalDate.of(2020, 1, 7)
                    )
                )
                authentication(SAKSBEHANDLER_OID)
            }
        }
        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")
        verify(exactly = 1) {
            annulleringMediator.håndter(any(), SAKSBEHANDLER_OID, epostadresse)
        }
    }

}
