package no.nav.helse.spesialist.api.saksbehandler.handlinger

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.util.UUID

sealed interface HandlingFraApi

data class TildelOppgave(val oppgaveId: Long): HandlingFraApi
data class AvmeldOppgave(val oppgaveId: Long): HandlingFraApi
data class LeggOppgavePåVent(val oppgaveId: Long): HandlingFraApi
data class FjernOppgaveFraPåVent(val oppgaveId: Long): HandlingFraApi

@JsonIgnoreProperties
class OverstyrTidslinjeHandlingFraApi(
    val vedtaksperiodeId: UUID,
    val organisasjonsnummer: String,
    val fødselsnummer: String,
    val aktørId: String,
    val begrunnelse: String,
    val dager: List<OverstyrDagFraApi>
): HandlingFraApi {

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

    data class OverstyrArbeidsgiverFraApi(
        val organisasjonsnummer: String,
        val månedligInntekt: Double,
        val fraMånedligInntekt: Double,
        val refusjonsopplysninger: List<RefusjonselementFraApi>?,
        val fraRefusjonsopplysninger: List<RefusjonselementFraApi>?,
        val begrunnelse: String,
        val forklaring: String,
        val lovhjemmel: LovhjemmelFraApi?,
    ) {

        data class RefusjonselementFraApi(
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
    val overstyrteArbeidsforhold: List<ArbeidsforholdFraApi>,
) : HandlingFraApi {

    @JsonIgnoreProperties
    data class ArbeidsforholdFraApi(
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
): HandlingFraApi

data class LeggPåVent(
    val oppgaveId: Long,
    val saksbehandlerOid: UUID,
    val frist: LocalDate?,
    val begrunnelse: String?
): HandlingFraApi

data class FjernPåVent(
    val oppgaveId: Long
): HandlingFraApi