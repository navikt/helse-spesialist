package no.nav.helse.spesialist.api

import io.ktor.client.request.*
import io.ktor.http.*
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import java.util.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.spesialist.api.utbetaling.Annullering
import no.nav.helse.spesialist.api.utbetaling.annulleringApi
import org.junit.jupiter.api.AfterEach
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
        val saksbehandlerOid = UUID.randomUUID()
        val response = runBlocking {
            client.preparePost("/api/annullering") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(mapOf(
                    "aktørId" to "en-aktørid",
                    "fødselsnummer" to "et-fødselsnummer",
                    "organisasjonsnummer" to "et-organisasjonsnummer",
                    "fagsystemId" to "en-fagsystem-id",
                    "saksbehandlerIdent" to "Z999999",
                    "kommentar" to "Russekort",
                    "begrunnelser" to listOf("Ingen liker fisk", "En giraff!!"),
                    "saksbehandlerOid" to saksbehandlerOid
                ))
                authentication(SAKSBEHANDLER_OID)
            }.execute()
        }
        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")
        val forventetAnnullering = Annullering("en-aktørid", "et-fødselsnummer", "et-organisasjonsnummer", "en-fagsystem-id", "Z999999", begrunnelser = listOf("Ingen liker fisk", "En giraff!!"), "Russekort", saksbehandlerOid)
        verify(exactly = 1) {
            saksbehandlerMediator.håndter(forventetAnnullering, any())
        }
    }

    @Test
    fun annulleringTommeVerdier() {
        val saksbehandlerOid = UUID.randomUUID()
        val response = runBlocking {
            client.preparePost("/api/annullering") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(mapOf(
                    "aktørId" to "en-aktørid",
                    "fødselsnummer" to "et-fødselsnummer",
                    "organisasjonsnummer" to "et-organisasjonsnummer",
                    "fagsystemId" to "en-fagsystem-id",
                    "saksbehandlerIdent" to "Z999999",
                    "saksbehandlerOid" to saksbehandlerOid
                ))
                authentication(SAKSBEHANDLER_OID)
            }.execute()
        }
        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")
        val forventetAnnullering = Annullering("en-aktørid", "et-fødselsnummer", "et-organisasjonsnummer", "en-fagsystem-id", "Z999999", begrunnelser = emptyList(), kommentar = null, saksbehandlerOid = saksbehandlerOid)
        verify(exactly = 1) {
            saksbehandlerMediator.håndter(forventetAnnullering, any())
        }
    }
}
