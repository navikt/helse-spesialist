package no.nav.helse.spesialist.client.speed

import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.modell.vedtaksperiode.objectMapper
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.PersoninfoHenter
import no.nav.helse.spesialist.application.logg.loggDebug
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.client.speed.dto.PersonRequest
import no.nav.helse.spesialist.client.speed.dto.PersonResponse
import no.nav.helse.spesialist.domain.Personinfo
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import java.util.UUID

class SpeedClientPersoninfoHenter(
    private val configuration: ClientSpeedModule.Configuration,
    private val accessTokenGenerator: AccessTokenGenerator,
    private val environmentToggles: EnvironmentToggles,
) : PersoninfoHenter {
    override fun hentPersoninfo(ident: String): Personinfo? {
        val accessToken =
            if (environmentToggles.devGcp) {
                "whatever-token"
            } else {
                accessTokenGenerator.hentAccessToken(configuration.scope)
            }
        val uri = "${configuration.apiUrl}/api/person"
        loggDebug("Utfører HTTP POST $uri")

        val requestBody = objectMapper.writeValueAsString(PersonRequest(ident = ident))

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
                        val dto = objectMapper.readValue(responseBody, PersonResponse::class.java)
                        dto.tilPersoninfo()
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

    private fun PersonResponse.tilPersoninfo() =
        Personinfo(
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            fødselsdato = fødselsdato,
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
