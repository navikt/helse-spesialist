package no.nav.helse.person

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.arbeidsgiver.ArbeidsgiverDto
import java.time.LocalDate

data class PersonDto(
    val aktørId: String,
    val fødselsnummer: String,
    val dødsdato: LocalDate?,
    val arbeidsgivere: List<ArbeidsgiverDto>,
    val inntektsgrunnlag: JsonNode
)
