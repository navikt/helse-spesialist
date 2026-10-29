@file:UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
    YearMonth::class,
)

package no.nav.helse.spesialist.api.rest.saksbilde

import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import kotlinx.serialization.json.JsonClassDiscriminator
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@Serializable
data class ApiBehandling(
    val id: UUID,
    val perioder: List<ApiPeriode>,
)

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@Serializable
sealed interface ApiOverstyring {
    val hendelseId: UUID
    val timestamp: LocalDateTime
    val saksbehandler: ApiSaksbehandler
    val ferdigstilt: Boolean
    val vedtaksperiodeId: UUID
}

@Serializable
@SerialName("ApiDagoverstyring")
data class ApiDagoverstyring(
    override val hendelseId: UUID,
    override val timestamp: LocalDateTime,
    override val saksbehandler: ApiSaksbehandler,
    override val ferdigstilt: Boolean,
    val dager: List<ApiOverstyrtDag>,
    val begrunnelse: String,
    override val vedtaksperiodeId: UUID,
) : ApiOverstyring

@Serializable
data class ApiOverstyrtDag(
    val dato: LocalDate,
    val type: ApiDagtype,
    val fraType: ApiDagtype?,
    val grad: Int?,
    val fraGrad: Int?,
)

@Serializable
@SerialName("ApiInntektoverstyring")
data class ApiInntektoverstyring(
    override val hendelseId: UUID,
    override val timestamp: LocalDateTime,
    override val saksbehandler: ApiSaksbehandler,
    override val ferdigstilt: Boolean,
    val inntekt: ApiOverstyrtInntekt,
    override val vedtaksperiodeId: UUID,
) : ApiOverstyring

@Serializable
data class ApiOverstyrtInntekt(
    val forklaring: String,
    val begrunnelse: String,
    val manedligInntekt: Double,
    val fraManedligInntekt: Double?,
    val skjaeringstidspunkt: LocalDate,
    val refusjonsopplysninger: List<ApiRefusjonsopplysning>?,
    val fraRefusjonsopplysninger: List<ApiRefusjonsopplysning>?,
)

@Serializable
data class ApiRefusjonsopplysning(
    val fom: LocalDate,
    val tom: LocalDate?,
    val belop: Double,
)

@Serializable
@SerialName("ApiMinimumSykdomsgradOverstyring")
data class ApiMinimumSykdomsgradOverstyring(
    override val hendelseId: UUID,
    override val timestamp: LocalDateTime,
    override val saksbehandler: ApiSaksbehandler,
    override val ferdigstilt: Boolean,
    val minimumSykdomsgrad: ApiOverstyrtMinimumSykdomsgrad,
    override val vedtaksperiodeId: UUID,
) : ApiOverstyring

@Serializable
data class ApiOverstyrtMinimumSykdomsgrad(
    val perioderVurdertOk: List<ApiOverstyrtMinimumSykdomsgradPeriode>,
    val perioderVurdertIkkeOk: List<ApiOverstyrtMinimumSykdomsgradPeriode>,
    val begrunnelse: String,
    val initierendeVedtaksperiodeId: UUID,
)

@Serializable
data class ApiOverstyrtMinimumSykdomsgradPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
)

@Serializable
@SerialName("ApiSykepengegrunnlagskjonnsfastsetting")
data class ApiSykepengegrunnlagskjonnsfastsetting(
    override val hendelseId: UUID,
    override val timestamp: LocalDateTime,
    override val saksbehandler: ApiSaksbehandler,
    override val ferdigstilt: Boolean,
    val skjonnsfastsatt: ApiSkjonnsfastsattSykepengegrunnlag,
    override val vedtaksperiodeId: UUID,
) : ApiOverstyring

@Serializable
data class ApiSkjonnsfastsattSykepengegrunnlag(
    val arsak: String,
    val type: ApiSkjonnsfastsettingstype?,
    val begrunnelse: String?,
    val begrunnelseMal: String?,
    val begrunnelseFritekst: String?,
    val begrunnelseKonklusjon: String?,
    val arlig: Double,
    val fraArlig: Double?,
    val skjaeringstidspunkt: LocalDate,
)

@Serializable
@SerialName("ApiArbeidsforholdoverstyring")
data class ApiArbeidsforholdoverstyring(
    override val hendelseId: UUID,
    override val timestamp: LocalDateTime,
    override val saksbehandler: ApiSaksbehandler,
    override val ferdigstilt: Boolean,
    val deaktivert: Boolean,
    val skjaeringstidspunkt: LocalDate,
    val forklaring: String,
    val begrunnelse: String,
    override val vedtaksperiodeId: UUID,
) : ApiOverstyring

@Serializable
data class ApiGhostPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val skjaeringstidspunkt: LocalDate,
    val vilkarsgrunnlagId: UUID?,
    val deaktivert: Boolean,
    val organisasjonsnummer: String,
) {
    val id = UUID.nameUUIDFromBytes(fom.toString().toByteArray() + organisasjonsnummer.toByteArray()).toString()
}

@Serializable
data class ApiArbeidsgiver(
    val organisasjonsnummer: String,
    val navn: String,
    val ghostPerioder: List<ApiGhostPeriode>,
    val behandlinger: List<ApiBehandling>,
    val overstyringer: List<ApiOverstyring>,
)
