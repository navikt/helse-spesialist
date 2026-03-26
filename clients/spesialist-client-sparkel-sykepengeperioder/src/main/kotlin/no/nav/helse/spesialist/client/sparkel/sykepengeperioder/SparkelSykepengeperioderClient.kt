package no.nav.helse.spesialist.client.sparkel.sykepengeperioder

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.micrometer.core.instrument.Metrics
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.Cache
import no.nav.helse.spesialist.application.InfotrygdperiodeHenter
import no.nav.helse.spesialist.application.hentGjennomCache
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Infotrygdperiode
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

class SparkelSykepengeperioderClient(
    private val configuration: ClientSparkelSykepengeperioderModule.Configuration,
    private val accessTokenGenerator: AccessTokenGenerator,
    private val cache: Cache,
) : InfotrygdperiodeHenter {
    private val objectMapper =
        jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    override fun hentFor(
        identitetsnummer: Identitetsnummer,
        fom: LocalDate,
    ): List<Infotrygdperiode> =
        cache.hentGjennomCache(
            namespace = "sparkel-sykepengeperioder-client:infotrygdperioder",
            id = "${identitetsnummer.value}|$fom",
            timeToLive = Duration.ofMinutes(10),
        ) {
            timer.recordCallable {
                hentFraSparkelSykepengeperioder(identitetsnummer, fom)
            }
        }

    private fun hentFraSparkelSykepengeperioder(
        identitetsnummer: Identitetsnummer,
        fom: LocalDate,
    ): List<Infotrygdperiode> {
        val accessToken = accessTokenGenerator.hentAccessToken(configuration.scope)
        val callId = UUID.randomUUID().toString()

        val requestBody =
            objectMapper.writeValueAsString(
                RequestDto(
                    personidentifikatorer = listOf(identitetsnummer.value),
                    fom = fom,
                    tom = LocalDate.now(),
                    inkluderAllePeriodetyper = true,
                ),
            )

        val uri = configuration.apiUrl
        loggInfo("Utfører HTTP POST $uri med header Nav-Call-Id: $callId")
        return Request
            .post(uri)
            .setHeader("Authorization", "Bearer $accessToken")
            .setHeader("X-Request-ID", callId)
            .setHeader("Accept", ContentType.APPLICATION_JSON.mimeType)
            .bodyString(requestBody, ContentType.APPLICATION_JSON)
            .execute()
            .handleResponse { response ->
                val responseBody = EntityUtils.toString(response.entity)
                if (response.code !in 200..299) {
                    responseError("Fikk HTTP ${response.code} tilbake fra sparkel-sykepengeperioder", responseBody)
                }

                val responseDto = objectMapper.readValue<ResponseDto>(responseBody)
                responseDto.utbetaltePerioder.map { it.tilDomene() }
            }
    }

    private fun responseError(
        melding: String,
        response: String,
    ): Nothing {
        loggError(melding, "response" to response)
        error(melding)
    }

    private val timer =
        Metrics.timer(
            "spesialist.client.call.timer",
            "client",
            "sparkel-sykepengeperioder",
            "operation",
            "hent-infotrygdperioder",
        )

    private data class RequestDto(
        val personidentifikatorer: List<String>,
        val fom: LocalDate,
        val tom: LocalDate,
        val inkluderAllePeriodetyper: Boolean,
    )

    private data class ResponseDto(
        val utbetaltePerioder: List<UtbetaltPeriodeDto>,
    )

    private data class UtbetaltPeriodeDto(
        val personidentifikator: String,
        val organisasjonsnummer: String?,
        val fom: LocalDate,
        val tom: LocalDate,
        val grad: Int,
        val dagsats: BigDecimal,
        val type: String,
        val tags: Set<String>,
    ) {
        fun tilDomene(): Infotrygdperiode =
            Infotrygdperiode(
                fom = fom,
                tom = tom,
                type = type,
            )
    }
}
