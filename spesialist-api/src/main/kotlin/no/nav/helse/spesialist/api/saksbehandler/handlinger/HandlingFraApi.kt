package no.nav.helse.spesialist.api.saksbehandler.handlinger

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.util.UUID

sealed interface HandlingFraApi {
    fun loggnavn(): String
}

@JsonIgnoreProperties
class OverstyrTidslinjeHandlingFraApi(
    val vedtaksperiodeId: UUID,
    val organisasjonsnummer: String,
    val fødselsnummer: String,
    val aktørId: String,
    val begrunnelse: String,
    val dager: List<OverstyrDagFraApi>
): HandlingFraApi {

    override fun loggnavn(): String = "overstyr_tidslinje"

    @JsonIgnoreProperties
    class OverstyrDagFraApi(
        val dato: LocalDate,
        val type: String,
        val fraType: String,
        val grad: Int?,
        val fraGrad: Int?,
        val lovhjemmel: LovhjemmelFraApi?,
    )
}

data class OverstyrInntektOgRefusjonHandlingFraApi(
    val aktørId: String,
    val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<OverstyrArbeidsgiverFraApi>,
) : HandlingFraApi {

    override fun loggnavn(): String = "overstyr_inntekt_og_refusjon"

    data class OverstyrArbeidsgiverFraApi(
        val organisasjonsnummer: String,
        val månedligInntekt: Double,
        val fraMånedligInntekt: Double,
        val refusjonsopplysninger: List<RefusjonselementDto>?,
        val fraRefusjonsopplysninger: List<RefusjonselementDto>?,
        val begrunnelse: String,
        val forklaring: String,
        val subsumsjon: LovhjemmelFraApi?,
        val lovhjemmel: LovhjemmelFraApi?,
    ) {

        data class RefusjonselementDto(
            val fom: LocalDate,
            val tom: LocalDate? = null,
            val beløp: Double,
        )
    }
}

@JsonIgnoreProperties
data class OverstyrArbeidsforholdHandlingFraApi(
    val fødselsnummer: String,
    val aktørId: String,
    val skjæringstidspunkt: LocalDate,
    val overstyrteArbeidsforhold: List<ArbeidsforholdDto>,
) : HandlingFraApi {

    override fun loggnavn(): String = "overstyr_arbeidsforhold"

    @JsonIgnoreProperties
    data class ArbeidsforholdDto(
        val orgnummer: String,
        val deaktivert: Boolean,
        val begrunnelse: String,
        val forklaring: String,
    )
}

data class SkjønnsfastsettSykepengegrunnlagHandlingFraApi(
    val aktørId: String,
    val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<SkjønnsfastsattArbeidsgiverFraApi>,
) : HandlingFraApi {

    override fun loggnavn(): String = "skjønnsfastsett_sykepengegrunnlag"

    data class SkjønnsfastsattArbeidsgiverFraApi(
        val organisasjonsnummer: String,
        val årlig: Double,
        val fraÅrlig: Double,
        val årsak: String,
        val type: SkjønnsfastsettingstypeDto,
        val begrunnelseMal: String?,
        val begrunnelseFritekst: String?,
        val begrunnelseKonklusjon: String?,
        val lovhjemmel: LovhjemmelFraApi?,
        val initierendeVedtaksperiodeId: String?,
    ) {

        enum class SkjønnsfastsettingstypeDto {
            OMREGNET_ÅRSINNTEKT,
            RAPPORTERT_ÅRSINNTEKT,
            ANNET,
        }
    }
}

data class LovhjemmelFraApi(
    val paragraf: String,
    val ledd: String? = null,
    val bokstav: String? = null,
    val lovverk: String,
    val lovverksversjon: String,
)

@JsonIgnoreProperties
data class AnnulleringHandlingFraApi(
    val aktørId: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val fagsystemId: String,
    val begrunnelser: List<String> = emptyList(),
    val kommentar: String?
): HandlingFraApi {
    override fun loggnavn(): String = "annuller_utbetaling"
}