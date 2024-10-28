package no.nav.helse.db.overstyring

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface OverstyringForDatabase {
    val eksternHendelseId: UUID
    val fødselsnummer: String
    val opprettet: LocalDateTime
    val vedtaksperiodeId: UUID
}

data class OverstyrtTidslinjeForDatabase(
    val id: UUID,
    val aktørId: String,
    override val fødselsnummer: String,
    val organisasjonsnummer: String,
    val dager: List<OverstyrtTidslinjedagForDatabase>,
    val begrunnelse: String,
    override val opprettet: LocalDateTime,
    override val vedtaksperiodeId: UUID,
) : OverstyringForDatabase {
    override val eksternHendelseId: UUID = id
}

data class OverstyrtTidslinjedagForDatabase(
    val dato: LocalDate,
    val type: String,
    val fraType: String,
    val grad: Int?,
    val fraGrad: Int?,
    val lovhjemmel: LovhjemmelForDatabase?,
)

data class OverstyrtInntektOgRefusjonForDatabase(
    val id: UUID,
    val aktørId: String,
    override val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<OverstyrtArbeidsgiverForDatabase>,
    override val opprettet: LocalDateTime,
    override val vedtaksperiodeId: UUID,
) : OverstyringForDatabase {
    override val eksternHendelseId: UUID = id
}

data class OverstyrtArbeidsgiverForDatabase(
    val organisasjonsnummer: String,
    val månedligInntekt: Double,
    val fraMånedligInntekt: Double,
    val refusjonsopplysninger: List<RefusjonselementForDatabase>?,
    val fraRefusjonsopplysninger: List<RefusjonselementForDatabase>?,
    val begrunnelse: String,
    val forklaring: String,
    val lovhjemmel: LovhjemmelForDatabase?,
    val fom: LocalDate?,
    val tom: LocalDate?,
)

data class RefusjonselementForDatabase(
    val fom: LocalDate,
    val tom: LocalDate? = null,
    val beløp: Double,
)

data class OverstyrtArbeidsforholdForDatabase(
    val id: UUID,
    override val fødselsnummer: String,
    val aktørId: String,
    val skjæringstidspunkt: LocalDate,
    val overstyrteArbeidsforhold: List<ArbeidsforholdForDatabase>,
    override val opprettet: LocalDateTime,
    override val vedtaksperiodeId: UUID,
) : OverstyringForDatabase {
    override val eksternHendelseId: UUID = id
}

data class ArbeidsforholdForDatabase(
    val organisasjonsnummer: String,
    val deaktivert: Boolean,
    val begrunnelse: String,
    val forklaring: String,
)
