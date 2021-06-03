import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import no.nav.helse.arbeidsgiver.UtbetalingshistorikkElementDto
import no.nav.helse.objectMapper
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class UtbetalingshistorikkElementApiDto(
    val beregningId: UUID,
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
        val dato: LocalDate
    )

    companion object {
        private val log = LoggerFactory.getLogger(UtbetalingshistorikkElementApiDto::class.java)

        fun toSpeilMap(utbetalingshistorikk: List<JsonNode>): List<UtbetalingshistorikkElementApiDto> = try {
            utbetalingshistorikk.map {
                return@map objectMapper.treeToValue(it, UtbetalingshistorikkElementDto::class.java)
                    .let { element ->
                        UtbetalingshistorikkElementApiDto(
                            beregningId = element.beregningId,
                            tidsstempel = element.tidsstempel,
                            beregnettidslinje = element.beregnettidslinje.map { dag ->
                                Sykdomstidslinjedag(
                                    dag.dagen,
                                    dag.type,
                                    Sykdomstidslinjedag.Kilde(dag.kilde.type, dag.kilde.kildeId),
                                    dag.grad
                                )
                            },
                            hendelsetidslinje = element.hendelsetidslinje.map { dag ->
                                Sykdomstidslinjedag(
                                    dag.dagen,
                                    dag.type,
                                    Sykdomstidslinjedag.Kilde(dag.kilde.type, dag.kilde.kildeId),
                                    dag.grad
                                )
                            },
                            utbetaling = Utbetaling(
                                utbetalingstidslinje = element.utbetaling.utbetalingstidslinje.map { dag ->
                                    Utbetalingsdag(
                                        dag.type,
                                        dag.inntekt,
                                        dag.dato
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
        } catch (e: JsonParseException) {
            log.info("Ufullstendig sykdomshistorikk. Dette er forventet da vi har bygget ut historikkobjektet gradvis, men de burde bli borte etterhvert som tiden går")
            emptyList()
        } catch (e: MissingKotlinParameterException) {
            log.info("Ufullstendig sykdomshistorikk. Dette er forventet da vi har bygget ut historikkobjektet gradvis, men de burde bli borte etterhvert som tiden går")
            emptyList()
        }
    }
}
