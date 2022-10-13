package no.nav.helse.spesialist.api.utbetaling

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
enum class Utbetalingsstatus {
    GODKJENT,
    SENDT,
    OVERFØRT,
    UTBETALING_FEILET,
    UTBETALT,
    ANNULLERT,
    IKKE_UTBETALT,
    FORKASTET,
    IKKE_GODKJENT,
    GODKJENT_UTEN_UTBETALING,
    NY,
}

data class UtbetalingApiDto(
    val id: UUID,
    val type: String,
    val status: Utbetalingsstatus,
    val arbeidsgiveroppdrag: OppdragApiDto?,
    val personoppdrag: OppdragApiDto?,
    val annullertAvSaksbehandler: AnnullertAvSaksbehandlerApiDto?,
    val totalbeløp: Int?
)

data class AnnullertAvSaksbehandlerApiDto(
    val annullertTidspunkt: LocalDateTime,
    val saksbehandlerNavn: String
)

data class OppdragApiDto(
    val fagsystemId: String,
    val utbetalingslinjer: List<UtbetalingslinjeApiDto>,
    val mottaker: String // Fødselsnummer eller organisasjonsnummer
)

data class UtbetalingslinjeApiDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val totalbeløp: Int?
)
