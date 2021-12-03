package no.nav.helse.modell.vedtak.snapshot

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.helse.AccessTokenClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class SpeilSnapshotRestClientTest {
    private var queuedResponses = mutableListOf<MockRequestHandler>()
    val httpClient = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                val nextCall = assertNotNull(queuedResponses.removeFirstOrNull(), "No more enqueued mock responses")
                nextCall(request)
            }
        }
    }
    val accessTokenClient = mockk<AccessTokenClient>(relaxed = true)
    val snapshotClient = SpeilSnapshotRestClient(httpClient, accessTokenClient, UUID.randomUUID().toString(), 1L)

    @Test
    fun `retries on IO error` () {
        coEvery { accessTokenClient.hentAccessToken(any()) } returns "YEPPERS"
        enqueue(
            { respond("JSON") }
        )
        assertEquals("JSON", snapshotClient.hentSpeilSnapshot("1234567891"))
        coVerify(exactly = 1) { accessTokenClient.hentAccessToken(any()) }
    }

    @Test
    fun `retries 5 times on IO error` () {
        coEvery { accessTokenClient.hentAccessToken(any()) } returns "YEPPERS"
        enqueue(
            { throw IOException() },
            { throw IOException() },
            { throw IOException() },
            { throw IOException() },
            { respond("JSON") }
        )
        assertEquals("JSON", snapshotClient.hentSpeilSnapshot("1234567891"))
        coVerify(exactly = 5) { accessTokenClient.hentAccessToken(any()) }
    }

    @Test
    fun `retries max 5 times` () {
        coEvery { accessTokenClient.hentAccessToken(any()) } returns "YEPPERS"
        enqueue(
            { throw IOException() },
            { throw IOException() },
            { throw IOException() },
            { throw IOException() },
            { throw IOException() }
        )
        assertThrows<IOException> { snapshotClient.hentSpeilSnapshot("1234567891") }
        coVerify(exactly = 5) { accessTokenClient.hentAccessToken(any()) }
    }

    private fun enqueue(vararg responders: MockRequestHandler) {
        queuedResponses = mutableListOf(*responders)
    }
}
