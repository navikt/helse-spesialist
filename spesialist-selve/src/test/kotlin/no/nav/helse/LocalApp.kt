package no.nav.helse

import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.serializer.GraphQLClientSerializer
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import no.nav.helse.bootstrap.Environment
import no.nav.helse.bootstrap.SpesialistApp
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.graphql.schema.Reservasjon
import no.nav.helse.spesialist.api.reservasjon.ReservasjonClient
import no.nav.helse.spesialist.api.snapshot.ISnapshotClient
import no.nav.helse.spleis.graphql.HentSnapshot
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.testcontainers.containers.PostgreSQLContainer
import java.util.UUID

fun main() {
    val mockOAuth2Server = MockOAuth2Server()
    mockOAuth2Server.start()
    val clientId = "en-client-id"
    val issuerId = "LocalTestIssuer"
    val token =
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
        ).serialize()
    println(token)
    val azureConfig =
        AzureConfig(
            clientId = clientId,
            issuerUrl = mockOAuth2Server.issuerUrl(issuerId).toString(),
            jwkProviderUri = mockOAuth2Server.jwksUrl(issuerId).toString(),
            tokenEndpoint = mockOAuth2Server.tokenEndpointUrl(issuerId).toString(),
        )

    val spesialistApp =
        SpesialistApp(
            env = Environment(database.envvars + mapOf("LOKAL_UTVIKLING" to "true")),
            gruppekontroll = gruppekontroll,
            snapshotClient = snapshotClient,
            azureConfig = azureConfig,
            tilgangsgrupper = tilgangsgrupper,
            reservasjonClient = reservasjonClient,
            versjonAvKode = "versjon_1",
        ) {
            TestRapid()
        }

    val server =
        embeddedServer(CIO, port = 4321) {
            spesialistApp.ktorApp(this)
            routing {
                get("/local-token") {
                    return@get call.respond(message = token)
                }
            }
        }
    spesialistApp.start()
    server.start(wait = true)
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
    }
