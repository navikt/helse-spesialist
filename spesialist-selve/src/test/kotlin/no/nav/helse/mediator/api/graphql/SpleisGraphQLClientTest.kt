package no.nav.helse.mediator.api.graphql

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
internal class SpleisGraphQLClientTest {

    private val client = SpleisGraphQLClient(httpClient = HttpClient(Apache))

    @Test
    fun `henter eldre generasjoner`() {
        runBlocking {
            val result = client.hentEldreGenerasjoner("09047606370")
            this
        }
    }

}
