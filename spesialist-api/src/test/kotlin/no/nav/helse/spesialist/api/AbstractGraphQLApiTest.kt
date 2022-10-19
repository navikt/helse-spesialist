package no.nav.helse.spesialist.api

import com.expediagroup.graphql.server.execution.GraphQLRequestHandler
import com.expediagroup.graphql.server.execution.GraphQLServer
import graphql.GraphQL
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import io.mockk.mockk
import java.net.ServerSocket
import java.util.UUID
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkMediator
import no.nav.helse.spesialist.api.graphql.ContextFactory
import no.nav.helse.spesialist.api.graphql.RequestParser
import no.nav.helse.spesialist.api.graphql.SchemaBuilder
import no.nav.helse.spesialist.api.graphql.routes
import no.nav.helse.spesialist.api.oppgave.experimental.OppgaveService
import no.nav.helse.spesialist.api.reservasjon.ReservasjonClient
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ContentNegotiationServer

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal abstract class AbstractGraphQLApiTest : DatabaseIntegrationTest() {

    protected val kode7Saksbehandlergruppe = UUID.randomUUID()
    protected val skjermedePersonerGruppeId = UUID.randomUUID()
    protected val beslutterGruppeId = UUID.randomUUID()
    protected val riskSaksbehandlergruppe = UUID.randomUUID()

    protected val reservasjonClient = mockk<ReservasjonClient>(relaxed = true)
    protected val oppgaveService = mockk<OppgaveService>(relaxed = true)
    protected val behandlingsstatistikkMediator = mockk<BehandlingsstatistikkMediator>(relaxed = true)

    protected lateinit var client: HttpClient

    private lateinit var graphQLServer: GraphQLServer<ApplicationRequest>
    private lateinit var server: TestServerRuntime

    protected fun setupGraphQLServer() {
        val schema = SchemaBuilder(
            personApiDao = personApiDao,
            egenAnsattApiDao = egenAnsattApiDao,
            tildelingDao = tildelingDao,
            arbeidsgiverApiDao = arbeidsgiverApiDao,
            overstyringApiDao = overstyringApiDao,
            risikovurderingApiDao = risikovurderingApiDao,
            varselDao = varselDao,
            utbetalingApiDao = utbetalingApiDao,
            oppgaveApiDao = oppgaveApiDao,
            periodehistorikkDao = periodehistorikkDao,
            notatDao = notatDao,
            snapshotMediator = snapshotMediator,
            reservasjonClient = reservasjonClient,
            oppgaveService = oppgaveService,
            behandlingsstatistikkMediator = behandlingsstatistikkMediator,
        ).build()

        graphQLServer = GraphQLServer(
            requestParser = RequestParser(),
            contextFactory = ContextFactory(
                kode7Saksbehandlergruppe,
                skjermedePersonerGruppeId,
                beslutterGruppeId,
                riskSaksbehandlergruppe
            ),
            requestHandler = GraphQLRequestHandler(
                GraphQL.newGraphQL(schema).build()
            )
        )

        setupHttpServer {
            routes(graphQLServer)
        }
    }

    private fun setupHttpServer(λ: Route.() -> Unit) {
        server = TestServer(λ = λ).start()
        client = server.restClient()
    }

    @AfterAll
    protected fun tearDownHttpServer() {
        server.close()
    }

    companion object {
        internal val jwtStub = JwtStub()
        private val requiredGroup: UUID = UUID.randomUUID()
        internal const val clientId = "client_id"
        internal const val issuer = "https://jwt-provider-domain"
        private const val epostadresse = "sara.saksbehandler@nav.no"

        fun HttpRequestBuilder.authentication(oid: UUID, group: String? = null) {
            header(
                "Authorization",
                "Bearer ${
                    jwtStub.getToken(
                        listOfNotNull(requiredGroup.toString(), group),
                        oid.toString(),
                        epostadresse,
                        clientId,
                        issuer
                    )
                }"
            )
        }

    }

    private class TestServer(
        private val httpPort: Int = ServerSocket(0).use { it.localPort },
        private val λ: Route.() -> Unit,
    ) {
        fun start(): TestServerRuntime {
            return TestServerRuntime(λ, httpPort)
        }

    }

    private class TestServerRuntime(
        build: Route.() -> Unit,
        private val httpPort: Int
    ) : AutoCloseable {
        private val server = createEmbeddedServer(build, httpPort)

        companion object {
            private fun createEmbeddedServer(build: Route.() -> Unit, httpPort: Int) =
                embeddedServer(CIO, port = httpPort) {
                    install(ContentNegotiationServer) {
                        register(
                            ContentType.Application.Json,
                            JacksonConverter(objectMapper)
                        )
                    }
                    val jwkProvider = jwtStub.getJwkProviderMock()
                    val azureConfig =
                        AzureAdAppConfig(
                            clientId = clientId,
                            issuer = issuer,
                            jwkProvider = jwkProvider
                        )
                    azureAdAppAuthentication(azureConfig)
                    routing {
                        authenticate("oidc", build = build)
                    }
                }
        }

        init {
            server.start(wait = false)
        }

        override fun close() {
            server.stop(0, 0)
        }


        fun restClient(): HttpClient {
            return HttpClient {
                defaultRequest {
                    host = "localhost"
                    port = httpPort
                }
                expectSuccess = false
                install(ContentNegotiation) {
                    register(
                        ContentType.Application.Json,
                        JacksonConverter(objectMapper)
                    )
                }
            }
        }
    }

    protected fun queryize(@Language("GraphQL") queryString: String) = queryString.trimIndent()

}
