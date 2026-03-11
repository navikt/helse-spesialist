package no.nav.helse.spesialist.client.speed

import io.micrometer.core.instrument.Metrics
import no.nav.helse.modell.vedtaksperiode.objectMapper
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.Cache
import no.nav.helse.spesialist.application.PersoninfoHenter
import no.nav.helse.spesialist.application.hentGjennomCache
import no.nav.helse.spesialist.application.logg.loggDebug
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.client.speed.dto.PersonRequest
import no.nav.helse.spesialist.client.speed.dto.PersonResponse
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Personinfo
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import java.time.Duration
import java.util.UUID

class SpeedClientPersoninfoHenter(
    private val configuration: ClientSpeedModule.Configuration,
    private val accessTokenGenerator: AccessTokenGenerator,
    private val cache: Cache,
) : PersoninfoHenter {
    override fun hentPersoninfo(identitetsnummer: Identitetsnummer): Personinfo? =
        cache
            .hentGjennomCache<PersonResponse?>(key = "speed-client:person:${identitetsnummer.value}", timeToLive = Duration.ofHours(1)) {
                timer.recordCallable { hentFraSpeed(identitetsnummer) }
            }?.tilPersoninfo()

    private fun hentFraSpeed(identitetsnummer: Identitetsnummer): PersonResponse? {
        val accessToken = accessTokenGenerator.hentAccessToken(configuration.scope)

        val uri = "${configuration.apiUrl}/api/person"
        loggDebug("Utfører HTTP POST $uri")

        val requestBody = objectMapper.writeValueAsString(PersonRequest(ident = identitetsnummer.value))

        return Request
            .post(uri)
            .setHeader("Authorization", "Bearer $accessToken")
            .setHeader("Accept", ContentType.APPLICATION_JSON.mimeType)
            .setHeader("callId", UUID.randomUUID().toString())
            .bodyString(requestBody, ContentType.APPLICATION_JSON)
            .execute()
            .handleResponse { response ->
                when (response.code) {
                    200 -> {
                        val responseBody = EntityUtils.toString(response.entity)
                        objectMapper.readValue(responseBody, PersonResponse::class.java)
                    }

                    404 -> null

                    in 500..599 -> {
                        val responseBody = EntityUtils.toString(response.entity).orEmpty()
                        error("Serverfeil fra Speed: ${response.code}, body=$responseBody")
                    }

                    else -> {
                        val responseBody = EntityUtils.toString(response.entity).orEmpty()
                        loggError("Feil ved henting av personinfo: status=${response.code}, body=$responseBody")
                        error("Feil fra Speed: ${response.code}")
                    }
                }
            }
    }

    private val timer =
        Metrics.timer(
            "spesialist.client.call.timer",
            "client",
            "speed",
            "operation",
            "hent-personinfo",
        )

    private fun PersonResponse.tilPersoninfo() =
        Personinfo(
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            fødselsdato = fødselsdato,
            dødsdato = dødsdato,
            kjønn =
                when (kjønn) {
                    PersonResponse.Kjønn.MANN -> Personinfo.Kjønn.Mann
                    PersonResponse.Kjønn.KVINNE -> Personinfo.Kjønn.Kvinne
                    PersonResponse.Kjønn.UKJENT -> Personinfo.Kjønn.Ukjent
                },
            adressebeskyttelse =
                when (adressebeskyttelse) {
                    PersonResponse.Adressebeskyttelse.FORTROLIG -> Personinfo.Adressebeskyttelse.Fortrolig
                    PersonResponse.Adressebeskyttelse.STRENGT_FORTROLIG -> Personinfo.Adressebeskyttelse.StrengtFortrolig
                    PersonResponse.Adressebeskyttelse.STRENGT_FORTROLIG_UTLAND -> Personinfo.Adressebeskyttelse.StrengtFortroligUtland
                    PersonResponse.Adressebeskyttelse.UGRADERT -> Personinfo.Adressebeskyttelse.Ugradert
                },
        )
}
