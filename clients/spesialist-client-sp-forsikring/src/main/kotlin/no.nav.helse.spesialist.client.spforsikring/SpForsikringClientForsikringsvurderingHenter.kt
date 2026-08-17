package no.nav.helse.spesialist.client.spforsikring

import com.github.navikt.tbd_libs.access_token.AccessTokenProvider
import io.micrometer.core.instrument.Metrics
import no.nav.helse.mediator.asLocalDate
import no.nav.helse.modell.objectMapper
import no.nav.helse.spesialist.application.Ekskluderingsbegrunnelse
import no.nav.helse.spesialist.application.Ekskluderingsårsak
import no.nav.helse.spesialist.application.EkskludertForsikring
import no.nav.helse.spesialist.application.Folketrygdlovenreferanse
import no.nav.helse.spesialist.application.Forsikring
import no.nav.helse.spesialist.application.Forsikringsvurdering
import no.nav.helse.spesialist.application.ForsikringsvurderingHenter
import no.nav.helse.spesialist.application.KollektivForsikring
import no.nav.helse.spesialist.application.NavKjøptForsikring
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.client.spforsikring.ClientUtils.Companion.retryMedBackoff
import no.nav.helse.spesialist.domain.ForsikringsvurderingId
import no.nav.helse.spesialist.domain.Identitetsnummer
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import tools.jackson.databind.JsonNode
import java.time.Instant
import java.util.UUID

class SpForsikringClientForsikringsvurderingHenter(
    private val configuration: ClientSpForsikringModule.Configuration,
    private val accessTokenProvider: AccessTokenProvider,
) : ForsikringsvurderingHenter {
    override fun hent(forsikringsvurderingId: ForsikringsvurderingId): Forsikringsvurdering? {
        val accessToken = accessTokenProvider.machineToken(configuration.scope)
        val callId = UUID.randomUUID().toString()
        val uri = "${configuration.apiUrl}/forsikringsvurderinger/${forsikringsvurderingId.value}"
        loggInfo("Utfører HTTP GET $uri med header Call-Id: $callId")

        return timer.recordCallable {
            retryMedBackoff {
                Request
                    .get(uri)
                    .setHeader("Authorization", "Bearer $accessToken")
                    .setHeader("callId", callId)
                    .setHeader("Accept", ContentType.APPLICATION_JSON.mimeType)
                    .execute()
                    .handleResponse { response ->
                        val responseBody = EntityUtils.toString(response.entity)
                        loggInfo("Mottok svar HTTP ${response.code}-svar fra sp-forsikring", "responseBody" to responseBody)
                        when (response.code) {
                            200 -> {
                                val responseJson = objectMapper.readTree(responseBody)
                                Forsikringsvurdering(
                                    identitetsnummer = Identitetsnummer.fraString(responseJson["identitetsnummer"].asString()),
                                    harForsikring = responseJson["harForsikring"].asBoolean(),
                                    dekning =
                                        responseJson["dekning"]?.takeUnless { it.isNull }?.let { dekning ->
                                            Forsikringsvurdering.Dekning(
                                                grad = dekning["grad"].asInt(),
                                                fraDag = dekning["fraDag"].asInt(),
                                            )
                                        },
                                    ekskluderteForsikringer =
                                        responseJson["ekskluderteForsikringer"]?.takeUnless { it.isNull }?.toList().orEmpty().map { ekskludertForsikring ->
                                            EkskludertForsikring(
                                                virkningsdato = ekskludertForsikring["virkningsdato"].asLocalDate(),
                                                opphørsdato =
                                                    ekskludertForsikring["opphørsdato"]
                                                        ?.takeUnless { it.isNull }
                                                        ?.asLocalDate(),
                                                dekningsgrad = ekskludertForsikring["dekningsgrad"].asInt(),
                                                dekningIVentetid = ekskludertForsikring["dekningIVentetid"].asBoolean(),
                                                navn = ekskludertForsikring["navn"].asString(),
                                                folketrygdlovenreferanse = ekskludertForsikring["folketrygdlovenreferanse"].tilFolketrygdlovenreferanse(),
                                                ekskluderingsårsak =
                                                    when (ekskludertForsikring["ekskluderingsårsak"].asString()) {
                                                        "SKJÆRINGSTIDSPUNKT_INNEN_28_DAGER_FØR_VIRKNINGSDATO" -> Ekskluderingsårsak.SKJÆRINGSTIDSPUNKT_INNEN_28_DAGER_FØR_VIRKNINGSDATO
                                                        "SKJÆRINGSTIDSPUNKT_MER_ENN_28_DAGER_FØR_VIRKNINGSDATO" -> Ekskluderingsårsak.SKJÆRINGSTIDSPUNKT_MER_ENN_28_DAGER_FØR_VIRKNINGSDATO
                                                        "OPPHØRT_PÅ_SKJÆRINGSTIDSPUNKT" -> Ekskluderingsårsak.OPPHØRT_PÅ_SKJÆRINGSTIDSPUNKT
                                                        "ALDRI_BETALT" -> Ekskluderingsårsak.ALDRI_BETALT
                                                        else -> throw IllegalArgumentException("Ukjent ekskluderingsårsak: ${ekskludertForsikring["ekskluderingsårsak"].asString()}")
                                                    },
                                                ekskluderingsbegrunnelse =
                                                    ekskludertForsikring["ekskluderingsbegrunnelse"].let { begrunnelse ->
                                                        Ekskluderingsbegrunnelse(
                                                            forklaring = begrunnelse["forklaring"].asString(),
                                                            folketrygdlovenreferanse =
                                                                begrunnelse["folketrygdlovenreferanse"]
                                                                    ?.takeUnless { it.isNull }
                                                                    ?.tilFolketrygdlovenreferanse(),
                                                        )
                                                    },
                                            )
                                        },
                                    gjeldendeForsikring =
                                        responseJson["gjeldendeForsikring"]?.takeUnless { it.isNull }?.let { gjeldendeForsikring ->
                                            Forsikring(
                                                virkningsdato = gjeldendeForsikring["virkningsdato"].asLocalDate(),
                                                opphørsdato =
                                                    gjeldendeForsikring["opphørsdato"]
                                                        ?.takeUnless { it.isNull }
                                                        ?.asLocalDate(),
                                                dekningsgrad = gjeldendeForsikring["dekningsgrad"].asInt(),
                                                dekningIVentetid = gjeldendeForsikring["dekningIVentetid"].asBoolean(),
                                                navn = gjeldendeForsikring["navn"].asString(),
                                                folketrygdlovenreferanse = gjeldendeForsikring["folketrygdlovenreferanse"].tilFolketrygdlovenreferanse(),
                                            )
                                        },
                                    dataHentetTidspunkt = Instant.parse(responseJson["dataHentetTidspunkt"].asString()),
                                    samletDekning =
                                        responseJson["samletDekning"]?.takeUnless { it.isNull }?.let { samletDekning ->
                                            Forsikringsvurdering.Dekning(
                                                grad = samletDekning["grad"].asInt(),
                                                fraDag = samletDekning["fraDag"].asInt(),
                                            )
                                        },
                                    kollektivForsikring =
                                        responseJson["kollektivForsikring"]?.takeUnless { it.isNull }?.let { kollektivForsikring ->
                                            KollektivForsikring(
                                                navn = kollektivForsikring["navn"].asString(),
                                                dekningFolketrygdlovenreferanse =
                                                    kollektivForsikring["dekningFolketrygdlovenreferanse"].tilFolketrygdlovenreferanse(),
                                                kollektivFolketrygdlovenreferanse =
                                                    kollektivForsikring["kollektivFolketrygdlovenreferanse"].tilFolketrygdlovenreferanse(),
                                            )
                                        },
                                    navKjøpteForsikringer =
                                        responseJson["navKjøpteForsikringer"]
                                            ?.takeUnless { it.isNull }
                                            ?.toList()
                                            .orEmpty()
                                            .map { navKjøptForsikring ->
                                                NavKjøptForsikring(
                                                    navn = navKjøptForsikring["navn"].asString(),
                                                    dekningFolketrygdlovenreferanse =
                                                        navKjøptForsikring["dekningFolketrygdlovenreferanse"].tilFolketrygdlovenreferanse(),
                                                    virkningsdato = navKjøptForsikring["virkningsdato"].asLocalDate(),
                                                    opphørsdato =
                                                        navKjøptForsikring["opphørsdato"]
                                                            ?.takeUnless { it.isNull }
                                                            ?.asLocalDate(),
                                                    konklusjon =
                                                        navKjøptForsikring["konklusjon"].let { konklusjon ->
                                                            NavKjøptForsikring.Konklusjon(
                                                                forklaring = konklusjon["forklaring"].asString(),
                                                                folketrygdlovenreferanse =
                                                                    konklusjon["folketrygdlovenreferanse"]
                                                                        ?.takeUnless { it.isNull }
                                                                        ?.tilFolketrygdlovenreferanse(),
                                                            )
                                                        },
                                                    lagtTilGrunn = navKjøptForsikring["lagtTilGrunn"].asBoolean(),
                                                )
                                            },
                                    vurdertTidspunkt = Instant.parse(responseJson["vurdertTidspunkt"].asString()),
                                )
                            }

                            404 -> {
                                null
                            }

                            in 500..599 -> {
                                throw RetryableException("Serverfeil fra forsikringstjeneste: ${response.code}, body=$responseBody")
                            }

                            else -> {
                                loggError("Feil ved henting av forsikring: status=${response.code}, body=$responseBody")
                                throw RuntimeException("Feil fra forsikringstjeneste: ${response.code}")
                            }
                        }
                    }
            }
        }
    }

    private val timer =
        Metrics.timer(
            "spesialist.client.call.timer",
            "client",
            "sp-forsikring",
            "operation",
            "hent-forsikring",
        )
}

private fun JsonNode.tilFolketrygdlovenreferanse(): Folketrygdlovenreferanse =
    Folketrygdlovenreferanse(
        kapittel = this["kapittel"].asInt(),
        paragrafIKapittel = this["paragrafIKapittel"].asInt(),
        ledd = this["ledd"]?.takeUnless { it.isNull }?.asInt(),
        bokstav = this["bokstav"]?.takeUnless { it.isNull }?.asString()?.single(),
    )
