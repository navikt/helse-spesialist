package no.nav.helse.spesialist.api.graphql.schema

import java.util.UUID

data class TidslinjeOverstyring(
    val vedtaksperiodeId: UUID,
    val organisasjonsnummer: String,
    val fodselsnummer: String,
    val aktorId: String,
    val begrunnelse: String,
    val dager: List<OverstyringDag>,
)

data class InntektOgRefusjonOverstyring(
    val aktorId: String,
    val fodselsnummer: String,
    val skjaringstidspunkt: DateString,
    val arbeidsgivere: List<OverstyringArbeidsgiver>,
    val vedtaksperiodeId: UUID,
)

data class ArbeidsforholdOverstyringHandling(
    val fodselsnummer: String,
    val aktorId: String,
    val skjaringstidspunkt: DateString,
    val overstyrteArbeidsforhold: List<OverstyringArbeidsforhold>,
    val vedtaksperiodeId: UUID,
)

data class OverstyringArbeidsforhold(
    val orgnummer: String,
    val deaktivert: Boolean,
    val begrunnelse: String,
    val forklaring: String,
    val lovhjemmel: Lovhjemmel?,
)

data class OverstyringArbeidsgiver(
    val organisasjonsnummer: String,
    val manedligInntekt: Double,
    val fraManedligInntekt: Double,
    val refusjonsopplysninger: List<OverstyringRefusjonselement>?,
    val fraRefusjonsopplysninger: List<OverstyringRefusjonselement>?,
    val begrunnelse: String,
    val forklaring: String,
    val lovhjemmel: Lovhjemmel?,
) {
    data class OverstyringRefusjonselement(
        val fom: DateString,
        val tom: DateString? = null,
        val belop: Double,
    )
}

data class OverstyringDag(
    val dato: DateString,
    val type: String,
    val fraType: String,
    val grad: Int?,
    val fraGrad: Int?,
    val lovhjemmel: Lovhjemmel?,
)
