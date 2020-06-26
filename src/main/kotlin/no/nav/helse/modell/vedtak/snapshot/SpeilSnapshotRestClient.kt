package no.nav.helse.modell.vedtak.snapshot

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import kotlinx.coroutines.runBlocking
import no.nav.helse.AccessTokenClient

internal class SpeilSnapshotRestClient(
    private val httpClient: HttpClient,
    private val accessTokenClient: AccessTokenClient,
    private val spleisClientId: String
) {
    internal fun hentSpeilSpapshot(fnr: String): String {
        return runBlocking {
            val accessToken = accessTokenClient.hentAccessToken(spleisClientId)
            httpClient.get<HttpStatement>("http://spleis-api.tbd.svc.nais.local/api/person-snapshot") {
                header("Authorization", "Bearer $accessToken")
                header("fnr", fnr)
                accept(ContentType.Application.Json)
            }.receive<String>()
        }
    }
}
