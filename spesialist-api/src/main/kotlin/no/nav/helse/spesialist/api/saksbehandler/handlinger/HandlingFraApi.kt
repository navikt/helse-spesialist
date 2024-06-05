package no.nav.helse.spesialist.api.saksbehandler.handlinger

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.util.UUID

interface HandlingFraApi

data class OpphevStans(val fødselsnummer: String, val begrunnelse: String) : HandlingFraApi

data class TildelOppgave(val oppgaveId: Long) : HandlingFraApi

data class AvmeldOppgave(val oppgaveId: Long) : HandlingFraApi

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
    val vedtaksperiodeId: UUID,
    val utbetalingId: UUID,
    val begrunnelser: List<String> = emptyList(),
    val kommentar: String?,
) : HandlingFraApi

data class LeggPåVent(
    val oppgaveId: Long,
    val saksbehandlerOid: UUID,
    val frist: LocalDate,
    val skalTildeles: Boolean,
    val begrunnelse: String?,
) : HandlingFraApi

data class FjernPåVent(
    val oppgaveId: Long,
) : HandlingFraApi
