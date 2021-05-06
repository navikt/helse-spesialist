package no.nav.helse.modell.vedtak.snapshot

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.AccessTokenClient
import java.io.IOException

class SpeilSnapshotRestClient(
    private val httpClient: HttpClient,
    private val accessTokenClient: AccessTokenClient,
    private val spleisClientId: String,
    private val retryInterval: Long = 5000L
) {
    internal fun hentSpeilSpapshot(fnr: String): String {
        return runBlocking {
            hentSpeilSpapshot(fnr, 5)
        }
    }

    private suspend fun hentSpeilSpapshot(fnr: String, retries: Int): String = try {
        val accessToken = accessTokenClient.hentAccessToken(spleisClientId)
        httpClient.get<HttpStatement>("http://spleis-api.tbd.svc.nais.local/api/person-snapshot") {
            header("Authorization", "Bearer $accessToken")
            header("fnr", fnr)
            accept(ContentType.Application.Json)
        }.receive()
    } catch (e: IOException) {
        if (retries <= 1) {
            throw e
        } else {
            delay(retryInterval)
            hentSpeilSpapshot(fnr, retries - 1)
        }
    }
}
