@file:kotlinx.serialization.UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    Instant::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.rest

import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class ApiSykepengegrunnlagRequest(
    val årsak: String,
    val sykepengegrunnlagstype: ApiSykepengegrunnlagtype,
    val begrunnelseMal: String?,
    val begrunnelseFritekst: String?,
    val begrunnelseKonklusjon: String?,
    val intierendeVedtaksperiodeId: UUID,
) {
    @OptIn(ExperimentalSerializationApi::class)
    @JsonClassDiscriminator("discriminatorType")
    @JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "discriminatorType")
    @Serializable
    sealed interface ApiSykepengegrunnlagtype {
        @Serializable
        @SerialName("ApiSkjønnsfastsatt")
        data class ApiSkjønnsfastsatt(
            val skjønnsfastsettelsestype: ApiSkjønnsfastsettelsestype,
            val skjønnsfastsatteInntekter: List<ApiSkjønnsfastsattInntekt>,
            val lovverksreferanse: ApiLovverksreferanse,
        ) : ApiSykepengegrunnlagtype {
            enum class ApiSkjønnsfastsettelsestype {
                OMREGNET_ÅRSINNTEKT,
                RAPPORTERT_ÅRSINNTEKT,
                ANNET,
            }

            @Serializable
            data class ApiSkjønnsfastsattInntekt(
                val organisasjonsnummer: String,
                val årlig: Double,
                val fraÅrlig: Double,
            )
        }
    }
}
