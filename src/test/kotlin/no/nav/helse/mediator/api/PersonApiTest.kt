package no.nav.helse.mediator.api

import AbstractE2ETest
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.helse.modell.vedtak.WarningDto
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.snapshot
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.net.ServerSocket
import java.util.*

@TestInstance(PER_CLASS)
internal class PersonApiTest : AbstractE2ETest() {

    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val ORGNR = "222222222"
        private val SNAPSHOTV1 = snapshot(AKTØR, UNG_PERSON_FNR_2018, ORGNR, VEDTAKSPERIODE_ID)
    }

    @Test
    fun `Patcher tomme warnings`() {
        every { restClient.hentSpeilSpapshot(UNG_PERSON_FNR_2018) } returns SNAPSHOTV1
        val godkjenningsmeldingId = sendGodkjenningsbehov(
            ORGNR,
            VEDTAKSPERIODE_ID,
            warnings = listOf("Denne skulle ikke vært tom", "Ikke denne heller")
        )
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )

        vedtakDao.leggTilWarnings(VEDTAKSPERIODE_ID, listOf(WarningDto("", WarningKilde.Spleis)))

        val (status, response) = runBlocking {
            client.get<HttpResponse>("/api/person/aktorId/$AKTØR").let { it.status to it.readText() }
        }
        assertEquals(HttpStatusCode.OK, status)
        assertEquals(
            listOf("Denne skulle ikke vært tom", "Ikke denne heller"),
            vedtakDao.finnWarnings(VEDTAKSPERIODE_ID).map { it.melding })
        assertEquals(
            listOf("Denne skulle ikke vært tom", "Ikke denne heller"),
            objectMapper.readTree(response)["arbeidsgivere"].flatMap { it["vedtaksperioder"] }
                .flatMap { it["varsler"] }.map { it.asText() })
    }

    private lateinit var server: ApplicationEngine

    private val httpPort = ServerSocket(0).use { it.localPort }
    private val client = HttpClient {
        defaultRequest {
            host = "localhost"
            port = httpPort

        }
        expectSuccess = false
        install(JsonFeature) {
            serializer = JacksonSerializer(jackson = objectMapper)
        }
    }

    @BeforeAll
    fun setup() {

        server = embeddedServer(Netty, port = httpPort) {
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            routing {
                vedtaksperiodeApi(vedtaksperiodeMediator, mockk())
            }
        }.also {
            it.start(wait = false)
        }
    }

    @AfterAll
    fun tearDown() {
        server.stop(1000, 1000)
    }
}
