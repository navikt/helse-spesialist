package no.nav.helse.spesialist.api.overstyring

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class Dagtype { Sykedag, Feriedag, Egenmeldingsdag, Permisjonsdag }

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
)

data class OverstyringDagDto(
    val dato: LocalDate,
    val type: Dagtype,
    val fraType: Dagtype?,
    val grad: Int?,
    val fraGrad: Int?
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

data class OverstyringInntektOgRefusjonDto(
    val hendelseId: UUID,
    val fødselsnummer: String,
    val timestamp: LocalDateTime,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String?,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgiver: List<Arbeidsgiverelement>,
    val ferdigstilt: Boolean,
) {
    data class Arbeidsgiverelement(
        val organisasjonsnummer: String,
        val begrunnelse: String,
        val forklaring: String,
        val månedligInntekt: Double,
        val fraMånedligInntekt: Double?,
        val refusjonsopplysninger: List<Refusjonselement>?,
        val fraRefusjonsopplysninger: List<Refusjonselement>?,
    )
    data class Refusjonselement(
        val fom: LocalDate,
        val tom: LocalDate?,
        val beløp: Double,
    )
}

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
