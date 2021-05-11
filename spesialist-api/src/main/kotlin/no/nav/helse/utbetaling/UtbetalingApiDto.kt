package no.nav.helse.utbetaling

import java.time.LocalDate
import java.time.LocalDateTime

data class UtbetalingApiDto(
    val type: String,
    val status: String,
    val arbeidsgiverOppdrag: OppdragApiDto,
    val annullertAvSaksbehandler: AnnullertAvSaksbehandlerApiDto?,
    val totalbeløp: Int?
)

data class AnnullertAvSaksbehandlerApiDto(
    val annullertTidspunkt: LocalDateTime,
    val saksbehandlerNavn: String
)

data class OppdragApiDto(
    val organisasjonsnummer: String,
    val fagsystemId: String,
    val utbetalingslinjer: List<UtbetalingslinjeApiDto>
)

data class UtbetalingslinjeApiDto(
    val fom: LocalDate,
    val tom: LocalDate
)
