package no.nav.helse.spesialist.api.arbeidsgiver

import java.time.LocalDate

data class ArbeidsforholdApiDto(
    val organisasjonsnummer: String,
    val stillingstittel: String,
    val stillingsprosent: Int,
    val startdato: LocalDate,
    val sluttdato: LocalDate?
)