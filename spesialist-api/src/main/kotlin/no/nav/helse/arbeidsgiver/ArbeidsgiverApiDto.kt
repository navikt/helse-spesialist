package no.nav.helse.arbeidsgiver

import UtbetalingshistorikkElementApiDto
import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.overstyring.OverstyringApiDto
import java.time.LocalDate
import java.util.*

data class ArbeidsforholdApiDto(
    val organisasjonsnummer: String,
    val stillingstittel: String,
    val stillingsprosent: Int,
    val startdato: LocalDate,
    val sluttdato: LocalDate?
)

data class ArbeidsgiverApiDto(
    val organisasjonsnummer: String,
    val navn: String,
    val id: UUID,
    val vedtaksperioder: List<JsonNode>,
    val overstyringer: List<OverstyringApiDto>,
    val bransjer: List<String>,
    val utbetalingshistorikk: List<UtbetalingshistorikkElementApiDto>,
    val generasjoner: JsonNode?
)
