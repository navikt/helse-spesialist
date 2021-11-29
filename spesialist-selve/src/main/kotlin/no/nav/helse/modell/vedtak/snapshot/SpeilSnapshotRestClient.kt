package no.nav.helse.modell.vedtak.snapshot

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.AccessTokenClient
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*

class SpeilSnapshotRestClient(
    private val httpClient: HttpClient,
    private val accessTokenClient: AccessTokenClient,
    private val spleisClientId: String,
    private val retryInterval: Long = 5000L
) {
    private companion object {
        val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    internal fun hentSpeilSpapshot(fnr: String): String {
        return runBlocking {
            hentSpeilSpapshot(fnr, 5)
        }
    }

    private suspend fun hentSpeilSpapshot(fnr: String, retries: Int): String = try {
        val accessToken = accessTokenClient.hentAccessToken(spleisClientId)
        sikkerLogg.info("Henter nytt speil-snapshot for personnummer: $fnr")
        val response: HttpResponse = httpClient.get("http://spleis-api.tbd.svc.nais.local/api/person-snapshot") {
            header("Authorization", "Bearer $accessToken")
            header("fnr", fnr)
            header("callId", UUID.randomUUID().toString())
            accept(ContentType.Application.Json)
        }
        if (!response.status.isSuccess()) {
            throw RuntimeException("Feil ved henting av speilsnapshot. Statuskode: ${response.status.value}")
        }
        response.receive()
    } catch (e: IOException) {
        sikkerLogg.error("Feil ved henting av snapshot for personnummer: $fnr. Vi prøver ${retries - 1} ganger til.")
        if (retries <= 1) {
            sikkerLogg.error("Gir opp etter 5 forsøk på å hente snapshot for personnummer: $fnr", e)
            throw e
        } else {
            delay(retryInterval)
            hentSpeilSpapshot(fnr, retries - 1)
        }
    } catch (e: RuntimeException) {
        sikkerLogg.error("Feil ved henting av snapshot for personnummer: $fnr. Vi prøver ${retries - 1} ganger til.", e)
        throw e
    }
}
