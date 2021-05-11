package no.nav.helse.arbeidsgiver

import com.fasterxml.jackson.databind.JsonNode
import java.util.*

data class ArbeidsgiverDto(
    val organisasjonsnummer: String,
    val id: UUID,
    val vedtaksperioder: List<JsonNode>,
    val utbetalingshistorikk: List<JsonNode>? = emptyList()
)
