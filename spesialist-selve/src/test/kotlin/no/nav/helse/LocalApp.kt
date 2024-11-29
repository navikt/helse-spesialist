package no.nav.helse

import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.serializer.GraphQLClientSerializer
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import no.nav.helse.bootstrap.Environment
import no.nav.helse.bootstrap.SpesialistApp
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.bootstrap.Gruppe
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.api.bootstrap.configureStatusPages
import no.nav.helse.spesialist.api.graphql.schema.Reservasjon
import no.nav.helse.spesialist.api.reservasjon.ReservasjonClient
import no.nav.helse.spesialist.api.snapshot.ISnapshotClient
import no.nav.helse.spleis.graphql.HentSnapshot
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.flywaydb.core.Flyway
import org.slf4j.event.Level
import org.testcontainers.containers.PostgreSQLContainer
import java.util.UUID

fun main() {
    LocalApp.start()
}

object LocalApp {

    // Simulerer RapidApp, der SpesialistApp (som trenger RapidsConnection) først gis
    // til RapidApplicationBuildern, og deretter får ut en RapidsConnection, dvs. at RapidsConnection er en
    // lateinit var
    @Suppress("JoinDeclarationAndAssignment")
    private lateinit var testRapid: TestRapid

    private val mockOAuth2Server = MockOAuth2Server().also(MockOAuth2Server::start)
    private val clientId = "en-client-id"
    private val issuerId = "LocalTestIssuer"
    private val token =
        mockOAuth2Server.issueToken(
            issuerId = issuerId,
            audience = clientId,
            claims =
                mapOf(
                    "preferred_username" to "saksbehandler@nav.no",
                    "oid" to "${UUID.randomUUID()}",
                    "name" to "En Saksbehandler",
                    "NAVident" to "X123456",
                ),
        ).serialize().also {
            println("OAuth2-token:")
            println(it)
        }

    private val azureConfig =
        AzureConfig(
            clientId = clientId,
            issuerUrl = mockOAuth2Server.issuerUrl(issuerId).toString(),
            jwkProviderUri = mockOAuth2Server.jwksUrl(issuerId).toString(),
            tokenEndpoint = mockOAuth2Server.tokenEndpointUrl(issuerId).toString(),
        )

    private val spesialistApp =
        SpesialistApp(
            env = Environment(database.envvars + mapOf("LOKAL_UTVIKLING" to "true")),
            gruppekontroll = gruppekontroll,
            snapshotClient = snapshotClient,
            azureConfig = azureConfig,
            tilgangsgrupper = tilgangsgrupper,
            reservasjonClient = reservasjonClient,
            versjonAvKode = "versjon_1",
        ) {
            testRapid
        }

    private val server =
        embeddedServer(CIO, port = 4321) {
            install(CallLogging) {
                level = Level.DEBUG
                filter { call -> call.request.path().startsWith("/") }
            }
            install(CallId) {
                header(HttpHeaders.XRequestId)
                generate { UUID.randomUUID().toString() }
            }
            install(StatusPages) {
                configureStatusPages()
            }
            spesialistApp.ktorApp(this)
            routing {
                get("/local-token") {
                    return@get call.respond(message = token)
                }
                playground()
            }
        }

    init {
        testRapid = TestRapid()
    }

    fun start() {
        spesialistApp.start()
        server.start(wait = true)
    }
}

private val snapshotClient =
    object : ISnapshotClient {
        private val serializer: GraphQLClientSerializer =
            GraphQLClientJacksonSerializer(
                jacksonObjectMapper().disable(
                    FAIL_ON_UNKNOWN_PROPERTIES,
                ),
            )

        override fun hentSnapshot(fnr: String): GraphQLClientResponse<HentSnapshot.Result> {
            return serializer.deserialize(
                rawResponse = "",
                responseType = HentSnapshot(variables = HentSnapshot.Variables(fnr = fnr)).responseType(),
            )
        }
    }

private val reservasjonClient =
    object : ReservasjonClient {
        override suspend fun hentReservasjonsstatus(fnr: String): Reservasjon? {
            return null
        }
    }

private val tilgangsgrupper =
    object : Tilgangsgrupper {
        override val kode7GruppeId: UUID = UUID.randomUUID()
        override val beslutterGruppeId: UUID = UUID.randomUUID()
        override val skjermedePersonerGruppeId: UUID = UUID.randomUUID()
        override val stikkprøveGruppeId: UUID = UUID.randomUUID()
        override val spesialsakGruppeId: UUID = UUID.randomUUID()

        override fun gruppeId(gruppe: Gruppe): UUID {
            return when (gruppe) {
                Gruppe.KODE7 -> kode7GruppeId
                Gruppe.BESLUTTER -> beslutterGruppeId
                Gruppe.SKJERMEDE -> skjermedePersonerGruppeId
                Gruppe.STIKKPRØVE -> stikkprøveGruppeId
                Gruppe.SPESIALSAK -> spesialsakGruppeId
            }
        }
    }

private val gruppekontroll =
    object : Gruppekontroll {
        override suspend fun erIGrupper(
            oid: UUID,
            gruppeIder: List<UUID>,
        ): Boolean {
            return true
        }
    }

private val database =
    object {
        private val postgres =
            PostgreSQLContainer<Nothing>("postgres:14").apply {
                withReuse(true)
                withLabel("app-navn", "spesialist")
                start()

                println("Database: jdbc:postgresql://localhost:$firstMappedPort/test startet opp, credentials: test og test")
            }

        val envvars =
            mapOf(
                "DATABASE_HOST" to "localhost",
                "DATABASE_PORT" to "${postgres.firstMappedPort}",
                "DATABASE_DATABASE" to "test",
                "DATABASE_USERNAME" to "test",
                "DATABASE_PASSWORD" to "test",
            )

        protected val dataSource =
            HikariDataSource(HikariConfig().apply {
                jdbcUrl = postgres.jdbcUrl
                username = postgres.username
                password = postgres.password
                maximumPoolSize = 100
                connectionTimeout = 500
                initializationFailTimeout = 5000
            })

        init {
            Flyway.configure()
                .dataSource(dataSource)
                .placeholders(mapOf("spesialist_oid" to UUID.randomUUID().toString()))
                .load()
                .migrate()
        }
    }

private fun Route.playground() {
    get("playground") {
        call.respondText(buildPlaygroundHtml(), ContentType.Text.Html)
    }
}

private fun buildPlaygroundHtml() = Application::class.java.classLoader
    .getResource("graphql-playground.html")
    ?.readText()
    ?.replace("\${graphQLEndpoint}", "graphql")
    ?.replace("\${subscriptionsEndpoint}", "subscriptions")
    ?: throw IllegalStateException("graphql-playground.html cannot be found in the classpath")
