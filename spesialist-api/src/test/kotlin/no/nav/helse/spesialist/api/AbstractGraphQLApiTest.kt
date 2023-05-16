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
import io.ktor.client.request.post
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
    protected val saksbehandlereMedTilgangTilStikkprøver: List<String> = listOf("EN_IDENT")


    private val reservasjonClient = mockk<ReservasjonClient>(relaxed = true)
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
            utbetalingApiDao = utbetalingApiDao,
            oppgaveApiDao = oppgaveApiDao,
            periodehistorikkDao = periodehistorikkDao,
            notatDao = notatDao,
            totrinnsvurderingApiDao = totrinnsvurderingApiDao,
            snapshotMediator = snapshotMediator,
            reservasjonClient = reservasjonClient,
            behandlingsstatistikkMediator = behandlingsstatistikkMediator,
            tildelingService = tildelingService
        ).build()

        graphQLServer = GraphQLServer(
            requestParser = RequestParser(),
            contextFactory = ContextFactory(
                kode7Saksbehandlergruppe,
                skjermedePersonerGruppeId,
                beslutterGruppeId,
                riskSaksbehandlergruppe,
                saksbehandlereMedTilgangTilStikkprøver
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

        fun HttpRequestBuilder.authentication(navn: String, epost: String, ident: String, oid: String, group: String? = null) {
            header(
                "Authorization",
                "Bearer ${
                    jwtStub.getToken(
                        groups = listOfNotNull(requiredGroup.toString(), group),
                        oid = oid,
                        epostadresse = epost,
                        clientId = clientId,
                        issuer = issuer,
                        navn = navn,
                        navIdent = ident
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
        private val httpPort: Int,
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
            client.post("/graphql") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                authentication(
                    navn = SAKSBEHANDLER.navn,
                    epost = SAKSBEHANDLER.epost,
                    ident = SAKSBEHANDLER.ident,
                    oid = SAKSBEHANDLER.oid.toString(),
                    group = group?.toString())
                setBody(mapOf("query" to query))
            }.body<String>()
        }

        return objectMapper.readTree(response)
    }

}
