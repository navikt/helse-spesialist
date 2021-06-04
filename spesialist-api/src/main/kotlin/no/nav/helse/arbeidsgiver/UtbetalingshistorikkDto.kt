package no.nav.helse.arbeidsgiver

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class UtbetalingshistorikkElementDto(
    val beregningId: UUID,
    val beregnettidslinje: List<SykdomstidslinjedagDto>,
    val hendelsetidslinje: List<SykdomstidslinjedagDto>,
    val utbetaling: UtbetalingDto,
    val tidsstempel: LocalDateTime
)

data class SykdomstidslinjedagDto(
    val dagen: LocalDate,
    val type: String,
    val kilde: KildeDTO,
    val grad: Double? = null
) {
    data class KildeDTO(
        val type: String,
        val kildeId: UUID
    )
}

data class UtbetalingDto(
    val type: String,
    val status: String,
    val gjenståendeSykedager: Int?,
    val forbrukteSykedager: Int?,
    val arbeidsgiverNettoBeløp: Int,
    val arbeidsgiverFagsystemId: String,
    val maksdato: LocalDate,
    val beregningId: UUID,
    val utbetalingstidslinje: List<UtbetalingsdagDto>,
    val tidsstempel: LocalDateTime,
    val vurdering: VurderingDto?
) {
    data class VurderingDto(
        val godkjent: Boolean,
        val tidsstempel: LocalDateTime,
        val automatisk: Boolean,
        val ident: String
    )
}

data class UtbetalingsdagDto(
    val type: String,
    val inntekt: Int,
    val dato: LocalDate
)
