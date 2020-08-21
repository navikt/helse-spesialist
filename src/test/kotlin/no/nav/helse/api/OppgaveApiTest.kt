package no.nav.helse.api

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.host
import io.ktor.client.request.port
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import no.nav.helse.*
import no.nav.helse.modell.vedtak.SaksbehandleroppgavereferanseDto
import no.nav.helse.setupDataSourceMedFlyway
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.util.*
import kotlin.test.assertEquals

class OppgaveApiTest {
    private val httpPort = ServerSocket(0).use { it.localPort }
    private val dataSource = setupDataSourceMedFlyway()

    private val jwtStub = JwtStub()
    private val clientId = "client_id"
    private val issuer = "https://jwt-provider-domain"
    private val requiredGroup = "required_group"

    private val oidcDiscovery = OidcDiscovery(token_endpoint = "token_endpoint", jwks_uri = "en_uri", issuer = issuer)
    private val azureConfig = AzureAdAppConfig(clientId = clientId, requiredGroup = requiredGroup)
    private val jwkProvider = jwtStub.getJwkProviderMock()
    private val oppgaveMediator = OppgaveMediator(dataSource)

    private val client = HttpClient {
        defaultRequest {
            host = "localhost"
            port = httpPort
            header(
                "Authorization",
                "Bearer ${jwtStub.getToken(
                    arrayOf(requiredGroup),
                    UUID.randomUUID().toString(),
                    "tbd@nav.no",
                    clientId,
                    issuer
                )}"
            )
        }
        install(JsonFeature) {
            serializer = JacksonSerializer {
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                registerModule(JavaTimeModule())
            }
        }
    }

    private val app = embeddedServer(Netty, port = httpPort) {
        azureAdAppAuthentication(oidcDiscovery, azureConfig, jwkProvider)
        oppgaveApi(oppgaveMediator)
    }.also {
        it.start(wait = false)
    }

    @Disabled
    @Test
    fun `finner oppgave for person via fødselsnummer`() {
        val eventId = UUID.randomUUID()
        val person = TestPerson(dataSource)

        person.sendGodkjenningMessage(eventId)
        person.sendPersoninfo(eventId)

        val referanse = runBlocking {
            client.get<SaksbehandleroppgavereferanseDto>("/api/oppgave") {
                header("fødselsnummer", person.fødselsnummer)
            }
        }

        assertEquals(eventId, referanse.oppgavereferanse)
    }
}
