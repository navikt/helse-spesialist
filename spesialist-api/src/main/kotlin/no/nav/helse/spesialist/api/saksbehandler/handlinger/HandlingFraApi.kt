package no.nav.helse.spesialist.api.saksbehandler.handlinger

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

sealed interface HandlingFraApi {
    fun loggnavn(): String
}

@JsonIgnoreProperties
class OverstyrTidslinjeHandlingFraApi(
    val organisasjonsnummer: String,
    val fødselsnummer: String,
    val aktørId: String,
    val begrunnelse: String,
    val dager: List<OverstyrDagDto>
): HandlingFraApi {

    override fun loggnavn(): String = "overstyr_tidslinje"

    @JsonIgnoreProperties
    class OverstyrDagDto(
        val dato: LocalDate,
        val type: String,
        val fraType: String,
        val grad: Int?,
        val fraGrad: Int?,
        val subsumsjon: SubsumsjonDto?,
    )
}

data class OverstyrInntektOgRefusjonHandlingFraApi(
    val aktørId: String,
    val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<OverstyrArbeidsgiverDto>,
) : HandlingFraApi {

    override fun loggnavn(): String = "overstyr_inntekt_og_refusjon"

    data class OverstyrArbeidsgiverDto(
        val organisasjonsnummer: String,
        val månedligInntekt: Double,
        val fraMånedligInntekt: Double,
        val refusjonsopplysninger: List<RefusjonselementDto>?,
        val fraRefusjonsopplysninger: List<RefusjonselementDto>?,
        val begrunnelse: String,
        val forklaring: String,
        val subsumsjon: SubsumsjonDto?,
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
    val arbeidsgivere: List<SkjønnsfastsattArbeidsgiverDto>,
) : HandlingFraApi {

    override fun loggnavn(): String = "skjønnsfastsett_sykepengegrunnlag"

    data class SkjønnsfastsattArbeidsgiverDto(
        val organisasjonsnummer: String,
        val årlig: Double,
        val fraÅrlig: Double,
        val årsak: String,
        val type: SkjønnsfastsettingstypeDto,
        val begrunnelseMal: String?,
        val begrunnelseFritekst: String?,
        val begrunnelseKonklusjon: String?,
        val subsumsjon: SubsumsjonDto?,
        val initierendeVedtaksperiodeId: String?,
    ) {

        enum class SkjønnsfastsettingstypeDto {
            OMREGNET_ÅRSINNTEKT,
            RAPPORTERT_ÅRSINNTEKT,
            ANNET,
        }
    }
}

data class SubsumsjonDto(
    val paragraf: String,
    val ledd: String? = null,
    val bokstav: String? = null,
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