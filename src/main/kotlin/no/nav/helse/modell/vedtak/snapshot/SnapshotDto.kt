package no.nav.helse.modell.vedtak.snapshot

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.util.*

data class PersonFraSpleisDto(
    val aktørId: String,
    val fødselsnummer: String,
    val dødsdato: LocalDate?,
    val arbeidsgivere: List<ArbeidsgiverFraSpleisDto>,
    val inntektsgrunnlag: JsonNode
)

data class ArbeidsgiverFraSpleisDto(
    val organisasjonsnummer: String,
    val id: UUID,
    val vedtaksperioder: List<JsonNode>,
    val utbetalingshistorikk: List<JsonNode>? = emptyList()
)

data class UtbetalingshistorikkElementDto(
    val beregningId: UUID,
    val beregnettidslinje: List<SykdomstidslinjedagDto>,
    val hendelsetidslinje: List<SykdomstidslinjedagDto>,
    val utbetalinger: List<UtbetalingDto>
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
    val maksdato: LocalDate,
    val beregningId: UUID,
    val utbetalingstidslinje: List<UtbetalingsdagDto>
)

data class UtbetalingsdagDto(
    val type: String,
    val inntekt: Int,
    val dato: LocalDate
)

