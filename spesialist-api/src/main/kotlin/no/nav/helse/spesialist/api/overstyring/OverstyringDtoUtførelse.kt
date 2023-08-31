package no.nav.helse.spesialist.api.overstyring

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spesialist.api.overstyring.SkjønnsfastsattArbeidsgiverDto.Companion.toMap
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDto

internal enum class Skjønnsfastsettingstype {
    OMREGNET_ÅRSINNTEKT,
    RAPPORTERT_ÅRSINNTEKT,
    ANNET,
}

internal data class SkjønnsfastsattArbeidsgiverDto(
    val organisasjonsnummer: String,
    val årlig: Double,
    val fraÅrlig: Double,
    val årsak: String,
    val type: Skjønnsfastsettingstype,
    val begrunnelseMal: String?,
    val begrunnelseFritekst: String?,
    val begrunnelseKonklusjon: String?,
    val subsumsjon: SubsumsjonDto?,
    val initierendeVedtaksperiodeId: String?,
) {
    fun toMap(): Map<String, Any?> = listOfNotNull(
        "organisasjonsnummer" to organisasjonsnummer,
        "årlig" to årlig,
        "fraÅrlig" to fraÅrlig,
        "årsak" to årsak,
        "type" to type,
        "begrunnelseMal" to begrunnelseMal,
        "begrunnelseFritekst" to begrunnelseFritekst,
        "begrunnelseKonklusjon" to begrunnelseKonklusjon,
        "subsumsjon" to subsumsjon,
        "initierendeVedtaksperiodeId" to initierendeVedtaksperiodeId,
    ).toMap()

    internal companion object {
        fun List<SkjønnsfastsattArbeidsgiverDto>.toMap(): List<Map<String, Any?>> = this.map { it.toMap() }
    }
}

internal data class SkjønnsfastsattSykepengegrunnlagDto(
    val aktørId: String,
    val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<SkjønnsfastsattArbeidsgiverDto>,
) {
    fun somJsonMessage(saksbehandler: SaksbehandlerDto) = JsonMessage.newMessage(
        "saksbehandler_skjonnsfastsetter_sykepengegrunnlag",
        listOfNotNull(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "skjæringstidspunkt" to skjæringstidspunkt,
            "arbeidsgivere" to arbeidsgivere.toMap(),
            "saksbehandlerOid" to saksbehandler.oid,
            "saksbehandlerNavn" to saksbehandler.navn,
            "saksbehandlerIdent" to saksbehandler.ident,
            "saksbehandlerEpost" to saksbehandler.epost,
        ).toMap()
    )
}

@JsonIgnoreProperties
data class OverstyrArbeidsforholdDto(
    val fødselsnummer: String,
    val aktørId: String,
    val skjæringstidspunkt: LocalDate,
    val overstyrteArbeidsforhold: List<ArbeidsforholdOverstyrt>
) {
    @JsonIgnoreProperties
    data class ArbeidsforholdOverstyrt(
        val orgnummer: String,
        val deaktivert: Boolean,
        val begrunnelse: String,
        val forklaring: String
    )

    fun somJsonMessage(saksbehandler: SaksbehandlerDto) = JsonMessage.newMessage(
        "saksbehandler_overstyrer_arbeidsforhold", mapOf(
            "fødselsnummer" to fødselsnummer,
            "aktørId" to aktørId,
            "saksbehandlerOid" to saksbehandler.oid,
            "saksbehandlerNavn" to saksbehandler.navn,
            "saksbehandlerIdent" to saksbehandler.ident,
            "saksbehandlerEpost" to saksbehandler.epost,
            "skjæringstidspunkt" to skjæringstidspunkt,
            "overstyrteArbeidsforhold" to overstyrteArbeidsforhold,
        )
    )
}

data class SubsumsjonDto(
    val paragraf: String,
    val ledd: String? = null,
    val bokstav: String? = null,
)