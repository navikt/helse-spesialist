package no.nav.helse.overstyring

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

enum class Dagtype { Sykedag, Feriedag, Egenmeldingsdag, Permisjonsdag }

data class OverstyringDto(
    val hendelseId: UUID,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val begrunnelse: String,
    val timestamp: LocalDateTime,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String?,
    val overstyrteDager: List<OverstyringDagDto>
)

data class OverstyringDagDto(
    val dato: LocalDate,
    val type: Dagtype,
    val grad: Int?
)

data class OverstyringInntektDto(
    val hendelseId: UUID,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val begrunnelse: String,
    val timestamp: LocalDateTime,
    val saksbehandlerNavn: String,
    val saksbehandlerIdent: String?,
    val måndeligInntekt: Double,
    val skjæringstidspunkt: LocalDate
)
