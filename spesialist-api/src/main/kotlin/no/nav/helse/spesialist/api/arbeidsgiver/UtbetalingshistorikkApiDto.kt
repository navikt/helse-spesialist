package no.nav.helse.spesialist.api.arbeidsgiver

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class UtbetalingshistorikkElementApiDto(
    val beregningId: UUID,
    val vilkårsgrunnlagHistorikkId: UUID,
    val beregnettidslinje: List<Sykdomstidslinjedag>,
    val hendelsetidslinje: List<Sykdomstidslinjedag>,
    val utbetaling: Utbetaling,
    val tidsstempel: LocalDateTime
) {
    data class Sykdomstidslinjedag(
        val dagen: LocalDate,
        val type: String,
        val kilde: Kilde,
        val grad: Double? = null
    ) {
        data class Kilde(
            val type: String,
            val kildeId: UUID
        )
    }

    data class Utbetaling(
        val type: String,
        val status: String,
        val gjenståendeSykedager: Int?,
        val forbrukteSykedager: Int?,
        val arbeidsgiverNettoBeløp: Int,
        val arbeidsgiverFagsystemId: String,
        val personNettoBeløp: Int,
        val personFagsystemId: String,
        val maksdato: LocalDate,
        val beregningId: UUID,
        val utbetalingstidslinje: List<Utbetalingsdag>,
        val tidsstempel: LocalDateTime,
        val vurdering: Vurdering?
    ) {
        data class Vurdering(
            val godkjent: Boolean,
            val tidsstempel: LocalDateTime,
            val automatisk: Boolean,
            val ident: String
        )
    }

    data class Utbetalingsdag(
        val type: String,
        val inntekt: Int,
        val dato: LocalDate,
        val utbetaling: Int?,
        val personbeløp: Int,
        val arbeidsgiverbeløp: Int,
        val refusjonsbeløp: Int?,
        val grad: Double?,
        val totalGrad: Double?,
        val begrunnelser: List<String>?
    )

}
