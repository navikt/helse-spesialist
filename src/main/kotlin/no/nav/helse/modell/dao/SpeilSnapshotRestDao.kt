package no.nav.helse.modell.dao

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import kotlinx.coroutines.runBlocking
import java.util.*

internal class SpeilSnapshotRest(private val httpClient: HttpClient) {
    internal fun hentSpeilSpapshot(vedtaksperiodeId: UUID): String {
        return runBlocking {
            httpClient.get<HttpStatement> {
                header("Authorization", "Bearer token")
                accept(ContentType.Application.Json)
                parameter("vedtaksperiodeId", vedtaksperiodeId)
            }.let { it.receive<String>() }
        }
    }
}
