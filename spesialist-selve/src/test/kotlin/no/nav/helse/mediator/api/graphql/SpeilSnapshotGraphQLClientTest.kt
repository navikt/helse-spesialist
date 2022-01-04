package no.nav.helse.mediator.api.graphql

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.mockk.mockk
import no.nav.helse.AccessTokenClient
import org.junit.jupiter.api.Test

internal class SpeilSnapshotGraphQLClientTest {
    val accessTokenClient = mockk<AccessTokenClient>(relaxed = true)

    private val spleisClient = HttpClient(Apache) {
        install(JsonFeature) { serializer = JacksonSerializer() }
        engine {
            socketTimeout = 30_000
            connectTimeout = 30_000
            connectionRequestTimeout = 40_000
        }
    }

    private val speilSnapshotGraphQLClient = SpeilSnapshotGraphQLClient(
        httpClient = spleisClient,
        accessTokenClient = accessTokenClient,
        spleisClientId = "spleis"
    )

    @Test
    fun `Connection-test` () {
        speilSnapshotGraphQLClient.hentSnapshot(fnr = "25068609936").data?.person?.let { person ->

            true
        }
    }
}
