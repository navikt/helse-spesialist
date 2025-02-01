package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLDeprecated
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.generator.annotations.GraphQLName
import io.ktor.utils.io.core.toByteArray
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@GraphQLName("Arbeidsforhold")
data class ApiArbeidsforhold(
    val stillingstittel: String,
    val stillingsprosent: Int,
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
)

@GraphQLName("Generasjon")
data class ApiGenerasjon(
    val id: UUID,
    val perioder: List<Periode>,
)

@GraphQLName("ArbeidsgiverInntekterFraAOrdningen")
data class ApiArbeidsgiverInntekterFraAOrdningen(
    val skjaeringstidspunkt: String,
    val inntekter: List<InntektFraAOrdningen>,
)

@GraphQLName("Overstyring")
interface ApiOverstyring {
    val hendelseId: UUID
    val timestamp: LocalDateTime
    val saksbehandler: Saksbehandler
    val ferdigstilt: Boolean
    val vedtaksperiodeId: UUID
}

@GraphQLName("Dagoverstyring")
data class ApiDagoverstyring(
    override val hendelseId: UUID,
    override val timestamp: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    override val ferdigstilt: Boolean,
    val dager: List<ApiOverstyrtDag>,
    val begrunnelse: String,
    override val vedtaksperiodeId: UUID,
) : ApiOverstyring {
    @GraphQLName("OverstyrtDag")
    data class ApiOverstyrtDag(
        val dato: LocalDate,
        val type: ApiDagtype,
        val fraType: ApiDagtype?,
        val grad: Int?,
        val fraGrad: Int?,
    )
}

@GraphQLName("Inntektoverstyring")
data class ApiInntektoverstyring(
    override val hendelseId: UUID,
    override val timestamp: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    override val ferdigstilt: Boolean,
    val inntekt: ApiOverstyrtInntekt,
    override val vedtaksperiodeId: UUID,
) : ApiOverstyring {
    @GraphQLName("OverstyrtInntekt")
    data class ApiOverstyrtInntekt(
        val forklaring: String,
        val begrunnelse: String,
        val manedligInntekt: Double,
        val fraManedligInntekt: Double?,
        val skjaeringstidspunkt: LocalDate,
        val refusjonsopplysninger: List<ApiRefusjonsopplysning>?,
        val fraRefusjonsopplysninger: List<ApiRefusjonsopplysning>?,
    )

    @GraphQLName("Refusjonsopplysning")
    data class ApiRefusjonsopplysning(
        val fom: LocalDate,
        val tom: LocalDate?,
        val belop: Double,
    )
}

@GraphQLName("MinimumSykdomsgradOverstyring")
data class ApiMinimumSykdomsgradOverstyring(
    override val hendelseId: UUID,
    override val timestamp: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    override val ferdigstilt: Boolean,
    val minimumSykdomsgrad: ApiOverstyrtMinimumSykdomsgrad,
    override val vedtaksperiodeId: UUID,
) : ApiOverstyring {
    @GraphQLName("OverstyrtMinimumSykdomsgrad")
    data class ApiOverstyrtMinimumSykdomsgrad(
        val perioderVurdertOk: List<ApiOverstyrtMinimumSykdomsgradPeriode>,
        val perioderVurdertIkkeOk: List<ApiOverstyrtMinimumSykdomsgradPeriode>,
        val begrunnelse: String,
        @GraphQLDeprecated("Bruk vedtaksperiodeId i stedet")
        val initierendeVedtaksperiodeId: UUID,
    )

    @GraphQLName("OverstyrtMinimumSykdomsgradPeriode")
    data class ApiOverstyrtMinimumSykdomsgradPeriode(
        val fom: LocalDate,
        val tom: LocalDate,
    )
}

@GraphQLName("Sykepengegrunnlagskjonnsfastsetting")
data class ApiSykepengegrunnlagskjonnsfastsetting(
    override val hendelseId: UUID,
    override val timestamp: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    override val ferdigstilt: Boolean,
    val skjonnsfastsatt: ApiSkjonnsfastsattSykepengegrunnlag,
    override val vedtaksperiodeId: UUID,
) : ApiOverstyring {
    @GraphQLName("SkjonnsfastsattSykepengegrunnlag")
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
}

@GraphQLName("Arbeidsforholdoverstyring")
data class ApiArbeidsforholdoverstyring(
    override val hendelseId: UUID,
    override val timestamp: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    override val ferdigstilt: Boolean,
    val deaktivert: Boolean,
    val skjaeringstidspunkt: LocalDate,
    val forklaring: String,
    val begrunnelse: String,
    override val vedtaksperiodeId: UUID,
) : ApiOverstyring

@GraphQLName("GhostPeriode")
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

@GraphQLName("NyttInntektsforholdPeriode")
data class ApiNyttInntektsforholdPeriode(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val organisasjonsnummer: String,
    val skjaeringstidspunkt: LocalDate,
    val dagligBelop: Double,
    val manedligBelop: Double,
)

@GraphQLIgnore
interface ArbeidsgiverSchema {
    fun organisasjonsnummer(): String

    fun navn(): String

    fun bransjer(): List<String>

    fun ghostPerioder(): List<ApiGhostPeriode>

    fun nyeInntektsforholdPerioder(): List<ApiNyttInntektsforholdPeriode>

    fun generasjoner(): List<ApiGenerasjon>

    fun overstyringer(): List<ApiOverstyring>

    fun arbeidsforhold(): List<ApiArbeidsforhold>

    fun inntekterFraAordningen(): List<ApiArbeidsgiverInntekterFraAOrdningen>
}

@GraphQLName("Arbeidsgiver")
class ApiArbeidsgiver(private val resolver: ArbeidsgiverSchema) : ArbeidsgiverSchema by resolver
