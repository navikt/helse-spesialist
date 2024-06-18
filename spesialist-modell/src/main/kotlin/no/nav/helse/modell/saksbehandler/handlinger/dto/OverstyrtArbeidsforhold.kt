package no.nav.helse.modell.saksbehandler.handlinger.dto

import java.time.LocalDate
import java.util.UUID

data class OverstyrtArbeidsforholdDto(
    val id: UUID,
    val fødselsnummer: String,
    val aktørId: String,
    val skjæringstidspunkt: LocalDate,
    val overstyrteArbeidsforhold: List<ArbeidsforholdDto>,
    val vedtaksperiodeId: UUID,
)

data class ArbeidsforholdDto(
    val organisasjonsnummer: String,
    val deaktivert: Boolean,
    val begrunnelse: String,
    val forklaring: String,
)
