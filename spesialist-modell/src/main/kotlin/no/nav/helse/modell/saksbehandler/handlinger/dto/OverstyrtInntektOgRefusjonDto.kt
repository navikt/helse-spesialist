package no.nav.helse.modell.saksbehandler.handlinger.dto

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.vilkårsprøving.LovhjemmelDto

data class OverstyrtInntektOgRefusjonDto(
    val id: UUID,
    val aktørId: String,
    val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<OverstyrtArbeidsgiverDto>,
)

data class OverstyrtArbeidsgiverDto(
    val organisasjonsnummer: String,
    val månedligInntekt: Double,
    val fraMånedligInntekt: Double,
    val refusjonsopplysninger: List<RefusjonselementDto>?,
    val fraRefusjonsopplysninger: List<RefusjonselementDto>?,
    val begrunnelse: String,
    val forklaring: String,
    val lovhjemmel: LovhjemmelDto?,
)

data class RefusjonselementDto(
    val fom: LocalDate,
    val tom: LocalDate? = null,
    val beløp: Double
)