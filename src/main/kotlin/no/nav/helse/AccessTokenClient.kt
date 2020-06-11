package no.nav.helse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

class AccessTokenClient(
    private val aadAccessTokenUrl: String,
    private val clientId: String,
    private val clientSecret: String,
    private val httpClient: HttpClient
) {

    private val log = LoggerFactory.getLogger(AccessTokenClient::class.java)
    private val mutex = Mutex()

    @Volatile
    private var tokenMap = HashMap<String, AadAccessToken>()

    suspend fun hentAccessToken(resource: String): String {
        val omToMinutter = Instant.now().plusSeconds(120L)
        return mutex.withLock {
            (tokenMap[resource]
                ?.takeUnless { it.expiry.isBefore(omToMinutter) }
                ?: run {
                    log.info("Henter nytt token fra Azure AD")
                    val response: AadAccessToken = try {
                        httpClient.post(aadAccessTokenUrl) {
                            accept(ContentType.Application.Json)
                            method = HttpMethod.Post
                            body = FormDataContent(Parameters.build {
                                append("client_id", clientId)
                                append("scope", "api://$resource/.default")
                                append("grant_type", "client_credentials")
                                append("client_secret", clientSecret)
                            })
                        }
                    } catch (e: Exception) {
                        throw RuntimeException("Klarte ikke hente nytt token fra Azure AD", e)
                    }
                    tokenMap[resource] = response
                    log.debug("Har hentet accesstoken")
                    return@run response
                }).access_token
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class AadAccessToken(
    val access_token: String,
    val expires_in: Duration
) {
    internal val expiry = Instant.now().plus(expires_in)
}
