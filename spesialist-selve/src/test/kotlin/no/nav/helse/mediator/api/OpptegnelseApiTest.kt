package no.nav.helse.mediator.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.abonnement.OpptegnelseDto
import no.nav.helse.abonnement.OpptegnelseMediator
import no.nav.helse.abonnement.OpptegnelseType
import no.nav.helse.abonnement.opptegnelseApi
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpptegnelseApiTest : AbstractApiTest() {

    private val opptegnelseMediator = mockk<OpptegnelseMediator>(relaxed = true)
    private val SAKSBEHANDLER_ID = UUID.randomUUID()
    private val AKTØR_ID = 12341234L

    @BeforeAll
    fun setupTildeling() {
        setupServer {
            opptegnelseApi(opptegnelseMediator)
        }
    }

    @Test
    fun oppdateringOk() {
        val response = runBlocking {
            client.preparePost("/api/opptegnelse/abonner/$AKTØR_ID") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                authentication(SAKSBEHANDLER_ID)
            }.execute()
        }
        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")
    }

    @Test
    fun `Kan hente nye oppdateringer for abonnement`() {
        val oppdatering1 = OpptegnelseDto(
            aktørId=11,
            sekvensnummer = 0,
            type= OpptegnelseType.UTBETALING_ANNULLERING_FEILET,
            payload = """{ "test": "1" }""")
        val oppdatering2 = OpptegnelseDto(
            aktørId=12,
            sekvensnummer = 1,
            type= OpptegnelseType.UTBETALING_ANNULLERING_OK,
            payload = """{ "test": "2" }""")
        val oppdatering3 = OpptegnelseDto(
            aktørId=12,
            sekvensnummer = 2,
            type= OpptegnelseType.REVURDERING_FERDIGBEHANDLET,
            payload = """{ "test": "3" }""")
        val expected = listOf(oppdatering1, oppdatering2, oppdatering3)

        every { opptegnelseMediator.hentAbonnerteOpptegnelser(any(), any()) } returns expected

        val response = runBlocking {
            client.prepareGet("/api/opptegnelse/hent/123") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                authentication(SAKSBEHANDLER_ID)
            }.execute()
        }
        assertTrue(response.status.isSuccess(), "HTTP response burde returnere en OK verdi, fikk ${response.status}")

        val actual = runBlocking { response.body<List<OpptegnelseDto>>() }
        assertEquals(expected, actual)
    }

}
