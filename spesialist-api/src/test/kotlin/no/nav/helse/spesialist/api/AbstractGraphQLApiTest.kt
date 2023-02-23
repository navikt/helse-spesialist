package no.nav.helse.spesialist.api

import com.expediagroup.graphql.server.execution.GraphQLRequestHandler
import com.expediagroup.graphql.server.execution.GraphQLServer
import com.fasterxml.jackson.databind.JsonNode
import graphql.GraphQL
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.mockk.mockk
import java.net.ServerSocket
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.spesialist.api.behandlingsstatistikk.BehandlingsstatistikkMediator
import no.nav.helse.spesialist.api.graphql.ContextFactory
import no.nav.helse.spesialist.api.graphql.RequestParser
import no.nav.helse.spesialist.api.graphql.SchemaBuilder
import no.nav.helse.spesialist.api.graphql.queryHandler
import no.nav.helse.spesialist.api.oppgave.experimental.OppgavePagineringDao
import no.nav.helse.spesialist.api.oppgave.experimental.OppgaveService
import no.nav.helse.spesialist.api.reservasjon.ReservasjonClient
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ContentNegotiationServer

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal abstract class AbstractGraphQLApiTest : DatabaseIntegrationTest() {

    protected val riskSaksbehandlergruppe: UUID = UUID.randomUUID()
    protected val kode7Saksbehandlergruppe: UUID = UUID.randomUUID()
    protected val skjermedePersonerGruppeId: UUID = UUID.randomUUID()
    protected val beslutterGruppeId: UUID = UUID.randomUUID()

    private val oppgavePagineringDao = OppgavePagineringDao(dataSource)

    private val reservasjonClient = mockk<ReservasjonClient>(relaxed = true)
    private val oppgaveService = OppgaveService(oppgavePagineringDao = oppgavePagineringDao)
    private val behandlingsstatistikkMediator = mockk<BehandlingsstatistikkMediator>(relaxed = true)

    private lateinit var client: HttpClient
    private lateinit var graphQLServer: GraphQLServer<ApplicationRequest>
    private lateinit var server: TestServerRuntime

    private fun setupGraphQLServer() {
        val schema = SchemaBuilder(
            personApiDao = personApiDao,
            egenAnsattApiDao = egenAnsattApiDao,
            tildelingDao = tildelingDao,
            arbeidsgiverApiDao = arbeidsgiverApiDao,
            overstyringApiDao = overstyringApiDao,
            risikovurderingApiDao = risikovurderingApiDao,
            varselDao = varselDao,
            varselRepository = apiVarselRepository,
            varselService = varselService,
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
            route("graphql") {
                queryHandler(graphQLServer)
            }
        }
    }

    private fun setupHttpServer(λ: Route.() -> Unit) {
        server = TestServer(λ = λ).start()
        client = server.restClient()
    }

    @BeforeAll
    fun setup() {
        setupGraphQLServer()
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
                            azureConfig = AzureConfig(
                                clientId = clientId,
                                issuer = issuer,
                                jwkProvider = jwkProvider,
                                tokenEndpoint = ""
                            )
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

    protected fun runQuery(@Language("GraphQL") query: String, group: UUID? = null): JsonNode {
        val response = runBlocking {
            val response = client.preparePost("/graphql") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                authentication(UUID.randomUUID(), group = group?.toString())
                setBody(mapOf("query" to query))
            }.execute()
            response.body<String>()
        }

        return objectMapper.readTree(response)
    }

}
