package no.nav.helse.modell.vedtak.snapshot

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.AccessTokenClient
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.util.*

class SpeilSnapshotRestClient(
    private val httpClient: HttpClient,
    private val accessTokenClient: AccessTokenClient,
    private val spleisClientId: String,
    private val spleisUrl: URI,
    private val retryInterval: Long = 5000L
) {
    private companion object {
        val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    internal fun hentSpeilSnapshot(fnr: String): String {
        return runBlocking {
            hentSpeilSnapshot(fnr, 5)
        }
    }

    private suspend fun hentSpeilSnapshot(fnr: String, retries: Int): String = try {
        val accessToken = accessTokenClient.hentAccessToken(spleisClientId)
        val callId = UUID.randomUUID().toString()
        sikkerLogg.info(
            "Henter nytt speil-snapshot for {}, {}",
            keyValue("fødselsnummer", fnr),
            keyValue("callId", callId)
        )
        val response: HttpResponse = httpClient.get(spleisUrl.resolve("api/person-snapshot").toURL()) {
            header("Authorization", "Bearer $accessToken")
            header("fnr", fnr)
            header("callId", callId)
            accept(ContentType.Application.Json)
        }
        if (!response.status.isSuccess()) {
            throw RuntimeException("Feil ved henting av speilsnapshot. Statuskode: ${response.status.value}")
        }
        response.receive()
    } catch (e: IOException) {
        sikkerLogg.error(
            "Feil ved henting av snapshot for fødselsnummer: $fnr. Vi prøver ${retries - 1} ganger til.",
            e
        )
        if (retries <= 1) {
            sikkerLogg.error("Gir opp etter 5 forsøk på å hente snapshot for fødselsnummer: $fnr", e)
            throw e
        } else {
            delay(retryInterval)
            hentSpeilSnapshot(fnr, retries - 1)
        }
    } catch (e: RuntimeException) {
        sikkerLogg.error(
            "Feil ved henting av snapshot for fødselsnummer: $fnr. Vi prøver ${retries - 1} ganger til.",
            e
        )
        throw e
    }
}
