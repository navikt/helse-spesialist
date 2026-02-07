package no.nav.helse.spesialist.client.entraid

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.Either
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spesialist.application.tilgangskontroll.Brukerrollehenter
import no.nav.helse.spesialist.application.tilgangskontroll.TilgangsgrupperTilBrukerroller
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import java.util.UUID

class MsGraphTilgangsgruppehenter(
    private val accessTokenGenerator: AccessTokenGenerator,
    private val tilgangsgrupperTilBrukerroller: TilgangsgrupperTilBrukerroller,
    private val msGraphUrl: String,
) : Brukerrollehenter {
    private val objectMapper =
        jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    override fun hentBrukerroller(saksbehandlerOid: SaksbehandlerOid): Either<Set<Brukerrolle>, Brukerrollehenter.Feil> {
        loggInfo("Henter tilgangsgrupper for saksbehandler", saksbehandlerOid.toString())
        return Request
            .post(msGraphUrl + "/v1.0/users/${saksbehandlerOid.value}/checkMemberGroups")
            .setHeader(
                "Authorization",
                "Bearer ${accessTokenGenerator.hentAccessToken("https://graph.microsoft.com/.default")}",
            ).setHeader("Accept", ContentType.APPLICATION_JSON.mimeType)
            .bodyString(
                objectMapper.writeValueAsString(
                    mapOf("groupIds" to tilgangsgrupperTilBrukerroller.alleUuider().map(UUID::toString)),
                ),
                ContentType.APPLICATION_JSON,
            ).execute()
            .handleResponse { response ->
                val responseStatus = response.code
                val responseBody = EntityUtils.toString(response.entity)
                if (responseStatus !in 200..299) {
                    teamLogs.warn("Fikk kode $responseStatus fra MS Graph: $responseBody")
                    if (responseStatus == 404) {
                        val errorCode = objectMapper.readTree(responseBody)["error"]["code"].asText()
                        if (errorCode == "Request_ResourceNotFound") {
                            return@handleResponse Either.Failure(Brukerrollehenter.Feil.SaksbehandlerFinnesIkke)
                        }
                    }
                    error("Fikk HTTP-kode $responseStatus fra MS Graph. Se sikkerlogg for detaljer.")
                }

                val grupper =
                    objectMapper
                        .readTree(responseBody)["value"]
                        .map(JsonNode::asText)
                        .map(UUID::fromString)
                logg.debug("Hentet ${grupper.size} grupper fra MS")
                val uuider = grupper.toSet()
                val brukerroller = tilgangsgrupperTilBrukerroller.finnBrukerrollerFraTilgangsgrupper(uuider)
                Either.Success(brukerroller)
            }
    }
}
