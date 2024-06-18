package no.nav.helse.spesialist.api.overstyring

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class Dagtype {
    Sykedag,
    SykedagNav,
    Feriedag,
    Egenmeldingsdag,
    Permisjonsdag,
    Arbeidsdag,
    ArbeidIkkeGjenopptattDag,
    Foreldrepengerdag,
    AAPdag,
    Omsorgspengerdag,
    Pleiepengerdag,
    Svangerskapspengerdag,
    Opplaringspengerdag,
    Dagpengerdag,

    // OBS! Spleis støtter ikke å motta disse dagene. De brukes kun (🤞) til historikkvisning, altså hvilken dag det ble overstyrt _fra_.
    Avvistdag,
    Helg,
}

data class OverstyringTidslinjeDto(
    val hendelseId: UUID,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val begrunnelse: String,
    val timestamp: LocalDateTime,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String?,
    val overstyrteDager: List<OverstyringDagDto>,
    val ferdigstilt: Boolean,
    val vedtaksperiodeId: UUID?,
)

data class OverstyringDagDto(
    val dato: LocalDate,
    val type: Dagtype,
    val fraType: Dagtype?,
    val grad: Int?,
    val fraGrad: Int?,
)

data class OverstyringInntektDto(
    val hendelseId: UUID,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val begrunnelse: String,
    val forklaring: String,
    val timestamp: LocalDateTime,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String?,
    val månedligInntekt: Double,
    val fraMånedligInntekt: Double?,
    val skjæringstidspunkt: LocalDate,
    val refusjonsopplysninger: List<Refusjonselement>?,
    val fraRefusjonsopplysninger: List<Refusjonselement>?,
    val ferdigstilt: Boolean,
) {
    data class Refusjonselement(
        val fom: LocalDate,
        val tom: LocalDate?,
        val beløp: Double,
    )
}

enum class Skjonnsfastsettingstype {
    OMREGNET_ARSINNTEKT,
    RAPPORTERT_ARSINNTEKT,
    ANNET,
}

data class SkjønnsfastsettingSykepengegrunnlagDto(
    val hendelseId: UUID,
    val fødselsnummer: String,
    val timestamp: LocalDateTime,
    val organisasjonsnummer: String,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String?,
    val skjæringstidspunkt: LocalDate,
    val ferdigstilt: Boolean,
    val årlig: Double,
    val fraÅrlig: Double?,
    val årsak: String,
    val type: Skjonnsfastsettingstype,
    val begrunnelse: String,
    val begrunnelseMal: String?,
    val begrunnelseFritekst: String?,
    val begrunnelseKonklusjon: String?,
)

data class OverstyringArbeidsforholdDto(
    val hendelseId: UUID,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val begrunnelse: String,
    val forklaring: String,
    val timestamp: LocalDateTime,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String?,
    val deaktivert: Boolean,
    val skjæringstidspunkt: LocalDate,
    val ferdigstilt: Boolean,
)
