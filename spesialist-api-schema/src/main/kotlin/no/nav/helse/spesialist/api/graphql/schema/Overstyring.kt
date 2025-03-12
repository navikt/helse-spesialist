package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import java.time.LocalDate
import java.util.UUID

@GraphQLName("TidslinjeOverstyring")
data class ApiTidslinjeOverstyring(
    val vedtaksperiodeId: UUID,
    val organisasjonsnummer: String,
    val fodselsnummer: String,
    val aktorId: String,
    val begrunnelse: String,
    val dager: List<ApiOverstyringDag>,
) : HandlingFraApi

@GraphQLName("InntektOgRefusjonOverstyring")
data class ApiInntektOgRefusjonOverstyring(
    val aktorId: String,
    val fodselsnummer: String,
    val skjaringstidspunkt: LocalDate,
    val arbeidsgivere: List<ApiOverstyringArbeidsgiver>,
    val vedtaksperiodeId: UUID,
) : HandlingFraApi

@GraphQLName("ArbeidsforholdOverstyringHandling")
data class ApiArbeidsforholdOverstyringHandling(
    val fodselsnummer: String,
    val aktorId: String,
    val skjaringstidspunkt: LocalDate,
    val overstyrteArbeidsforhold: List<ApiOverstyringArbeidsforhold>,
    val vedtaksperiodeId: UUID,
) : HandlingFraApi

@GraphQLName("OverstyringArbeidsforhold")
data class ApiOverstyringArbeidsforhold(
    val orgnummer: String,
    val deaktivert: Boolean,
    val begrunnelse: String,
    val forklaring: String,
    val lovhjemmel: ApiLovhjemmel?,
)

@GraphQLName("OverstyringArbeidsgiver")
data class ApiOverstyringArbeidsgiver(
    val organisasjonsnummer: String,
    val manedligInntekt: Double,
    val fraManedligInntekt: Double,
    val refusjonsopplysninger: List<ApiOverstyringRefusjonselement>?,
    val fraRefusjonsopplysninger: List<ApiOverstyringRefusjonselement>?,
    val begrunnelse: String,
    val forklaring: String,
    val lovhjemmel: ApiLovhjemmel?,
    val fom: LocalDate?,
    val tom: LocalDate?,
) {
    @GraphQLName("OverstyringRefusjonselement")
    data class ApiOverstyringRefusjonselement(
        val fom: LocalDate,
        val tom: LocalDate? = null,
        val belop: Double,
    )
}

@GraphQLName("OverstyringDag")
data class ApiOverstyringDag(
    val dato: LocalDate,
    val type: String,
    val fraType: String,
    val grad: Int?,
    val fraGrad: Int?,
    val lovhjemmel: ApiLovhjemmel?,
)

@GraphQLName("TilkommenInntektOverstyring")
data class ApiTilkommenInntektOverstyring(
    val fodselsnummer: String,
    val aktorId: String,
    val vedtaksperiodeId: UUID,
    val begrunnelse: String,
    val lagtTilEllerEndret: List<ApiNyEllerEndretInntekt>,
    val fjernet: List<ApiFjernetInntekt>,
) {
    @GraphQLName("NyEllerEndretInntekt")
    data class ApiNyEllerEndretInntekt(val organisasjonsnummer: String, val perioder: List<ApiPeriodeMedBeløp>) {
        @GraphQLName("PeriodeMedBelop")
        data class ApiPeriodeMedBeløp(val fom: LocalDate, val tom: LocalDate, val periodeBelop: Double)
    }

    @GraphQLName("FjernetInntekt")
    data class ApiFjernetInntekt(val organisasjonsnummer: String, val perioder: List<ApiPeriodeUtenBeløp>) {
        @GraphQLName("PeriodeUtenBelop")
        data class ApiPeriodeUtenBeløp(val fom: LocalDate, val tom: LocalDate)
    }
}
