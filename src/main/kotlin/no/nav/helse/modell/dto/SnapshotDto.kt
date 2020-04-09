package no.nav.helse.modell.dto

import com.fasterxml.jackson.databind.JsonNode
import java.util.*

data class PersonDTO(
    val aktørId: String,
    val fødselsnummer: String,
    val arbeidsgivere: List<ArbeidsgiverDTO>
)

data class ArbeidsgiverDTO(
    val organisasjonsnummer: String,
    val id: UUID,
    val vedtaksperioder: List<JsonNode>
)

