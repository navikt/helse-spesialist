package no.nav.helse.modell.dao

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import kotlinx.coroutines.runBlocking
import no.nav.helse.AccessTokenClient

internal class SpeilSnapshotRestDao(
    private val httpClient: HttpClient,
    private val accessTokenClient: AccessTokenClient,
    private val spleisClientId: String
) {

    internal fun hentSpeilSpapshot(aktørId: String): String {
        return runBlocking {
            val accessToken = accessTokenClient.hentAccessToken(spleisClientId)
            httpClient.get<HttpStatement>("http://spleis-api/api/person/$aktørId") {
                header("Authorization", "Bearer $accessToken")
                accept(ContentType.Application.Json)
            }.let { it.receive<String>() }
        }
    }
}
