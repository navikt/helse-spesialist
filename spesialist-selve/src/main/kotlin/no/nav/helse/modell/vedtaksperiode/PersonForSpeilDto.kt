package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import no.nav.helse.modell.overstyring.Dagtype
import no.nav.helse.modell.vedtak.snapshot.UtbetalingshistorikkElementDto
import no.nav.helse.objectMapper
import no.nav.helse.person.PersoninfoApiDto
import no.nav.helse.tildeling.TildelingApiDto
import no.nav.helse.vedtaksperiode.EnhetDto
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*


data class PersonForSpeilDto(
    val utbetalinger: List<UtbetalingForSpeilDto>,
    val aktørId: String,
    val fødselsnummer: String,
    val dødsdato: LocalDate?,
    val personinfo: PersoninfoApiDto,
    val arbeidsgivere: List<ArbeidsgiverForSpeilDto>,
    val infotrygdutbetalinger: JsonNode?,
    val enhet: EnhetDto,
    val arbeidsforhold: List<ArbeidsforholdForSpeilDto>,
    val inntektsgrunnlag: JsonNode,
    @Deprecated("erstattes av eget tildelingsobjekt")
    val erPåVent: Boolean,
    val tildeling: TildelingApiDto?
)

data class ArbeidsforholdForSpeilDto(
    val organisasjonsnummer: String,
    val stillingstittel: String,
    val stillingsprosent: Int,
    val startdato: LocalDate,
    val sluttdato: LocalDate?
)

data class ArbeidsgiverForSpeilDto(
    val organisasjonsnummer: String,
    val navn: String,
    val id: UUID,
    val vedtaksperioder: List<JsonNode>,
    val overstyringer: List<OverstyringForSpeilDto>,
    val bransjer: List<String>?,
    val utbetalingshistorikk: List<UtbetalingshistorikkElementForSpeilDto>
)

data class OverstyringForSpeilDto(
    val hendelseId: UUID,
    val begrunnelse: String,
    val timestamp: LocalDateTime,
    val overstyrteDager: List<OverstyringDagForSpeilDto>,
    val saksbehandlerNavn: String
)

data class OverstyringDagForSpeilDto(
    val dato: LocalDate,
    val dagtype: Dagtype,
    val grad: Int?
)

data class RisikovurderingForSpeilDto(
    val funn: List<JsonNode>,
    val kontrollertOk: List<JsonNode>,
) {
    val arbeidsuførhetvurdering: List<String> = funn.map { it["beskrivelse"].asText() }
    val ufullstendig: Boolean = false
}

data class UtbetalingForSpeilDto(
    val type: String,
    val status: String,
    val arbeidsgiverOppdrag: OppdragForSpeilDto,
    val annullertAvSaksbehandler: AnnullertAvSaksbehandlerForSpeilDto?,
    val totalbeløp: Int?
)

data class AnnullertAvSaksbehandlerForSpeilDto(
    val annullertTidspunkt: LocalDateTime,
    val saksbehandlerNavn: String
)

data class OppdragForSpeilDto(
    val organisasjonsnummer: String,
    val fagsystemId: String,
    val utbetalingslinjer: List<UtbetalingslinjeForSpeilDto>
)

data class UtbetalingslinjeForSpeilDto(
    val fom: LocalDate,
    val tom: LocalDate
)

data class UtbetalingshistorikkElementForSpeilDto(
    val beregningId: UUID,
    val beregnettidslinje: List<Sykdomstidslinjedag>,
    val hendelsetidslinje: List<Sykdomstidslinjedag>,
    val utbetalinger: List<Utbetaling>
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
        val maksdato: LocalDate,
        val beregningId: UUID,
        val utbetalingstidslinje: List<Utbetalingsdag>
    )

    data class Utbetalingsdag(
        val type: String,
        val inntekt: Int,
        val dato: LocalDate
    )

    companion object {
        private val log = LoggerFactory.getLogger(UtbetalingshistorikkElementForSpeilDto::class.java)

        fun toSpeilMap(utbetalingshistorikk: List<JsonNode>): List<UtbetalingshistorikkElementForSpeilDto> = try {
            utbetalingshistorikk.map {
                return@map objectMapper.treeToValue(it, UtbetalingshistorikkElementDto::class.java)
                    .let { element ->
                        UtbetalingshistorikkElementForSpeilDto(
                            element.beregningId,
                            element.beregnettidslinje.map { dag ->
                                Sykdomstidslinjedag(
                                    dag.dagen,
                                    dag.type,
                                    Sykdomstidslinjedag.Kilde(dag.kilde.type, dag.kilde.kildeId),
                                    dag.grad
                                )
                            },
                            element.hendelsetidslinje.map { dag ->
                                Sykdomstidslinjedag(
                                    dag.dagen,
                                    dag.type,
                                    Sykdomstidslinjedag.Kilde(dag.kilde.type, dag.kilde.kildeId),
                                    dag.grad
                                )
                            },
                            element.utbetalinger.map { utbetaling ->
                                Utbetaling(
                                    utbetalingstidslinje = utbetaling.utbetalingstidslinje.map { dag -> Utbetalingsdag(dag.type, dag.inntekt, dag.dato)},
                                    type = utbetaling.type,
                                    status = utbetaling.status,
                                    gjenståendeSykedager = utbetaling.gjenståendeSykedager,
                                    forbrukteSykedager = utbetaling.forbrukteSykedager,
                                    arbeidsgiverNettoBeløp = utbetaling.arbeidsgiverNettoBeløp,
                                    maksdato = utbetaling.maksdato,
                                    beregningId = utbetaling.beregningId
                                )
                            })
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
