import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.arbeidsgiver.UtbetalingshistorikkElementDto
import no.nav.helse.objectMapper
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
        val grad: Double?,
        val totalGrad: Double?,
        val begrunnelser: List<String>?
    )

    companion object {
        fun toSpeilMap(utbetalingshistorikk: List<JsonNode>): List<UtbetalingshistorikkElementApiDto> =
            utbetalingshistorikk.map {
                return@map objectMapper.treeToValue(it, UtbetalingshistorikkElementDto::class.java).let { element ->
                    UtbetalingshistorikkElementApiDto(
                        beregningId = element.beregningId,
                        vilkårsgrunnlagHistorikkId = element.vilkårsgrunnlagHistorikkId,
                        tidsstempel = element.tidsstempel,
                        beregnettidslinje = element.beregnettidslinje.map { dag ->
                            Sykdomstidslinjedag(
                                dagen = dag.dagen,
                                type = dag.type,
                                kilde = Sykdomstidslinjedag.Kilde(dag.kilde.type, dag.kilde.kildeId),
                                grad = dag.grad
                            )
                        },
                        hendelsetidslinje = element.hendelsetidslinje.map { dag ->
                            Sykdomstidslinjedag(
                                dagen = dag.dagen,
                                type = dag.type,
                                kilde = Sykdomstidslinjedag.Kilde(dag.kilde.type, dag.kilde.kildeId),
                                grad = dag.grad
                            )
                        },
                        utbetaling = Utbetaling(
                            utbetalingstidslinje = element.utbetaling.utbetalingstidslinje.map { dag ->
                                Utbetalingsdag(
                                    type = dag.type,
                                    inntekt = dag.inntekt,
                                    dato = dag.dato,
                                    utbetaling = dag.utbetaling,
                                    grad = dag.grad,
                                    totalGrad = dag.totalGrad,
                                    begrunnelser = dag.begrunnelser
                                )
                            },
                            type = element.utbetaling.type,
                            status = element.utbetaling.status,
                            gjenståendeSykedager = element.utbetaling.gjenståendeSykedager,
                            forbrukteSykedager = element.utbetaling.forbrukteSykedager,
                            arbeidsgiverNettoBeløp = element.utbetaling.arbeidsgiverNettoBeløp,
                            arbeidsgiverFagsystemId = element.utbetaling.arbeidsgiverFagsystemId,
                            maksdato = element.utbetaling.maksdato,
                            beregningId = element.utbetaling.beregningId,
                            tidsstempel = element.utbetaling.tidsstempel,
                            vurdering = element.utbetaling.vurdering?.let { vurdering ->
                                Utbetaling.Vurdering(
                                    godkjent = vurdering.godkjent,
                                    tidsstempel = vurdering.tidsstempel,
                                    automatisk = vurdering.automatisk,
                                    ident = vurdering.ident
                                )
                            }
                        )
                    )
                }
            }
    }
}
