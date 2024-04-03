package no.nav.helse.modell.arbeidsforhold

import java.time.LocalDate

data class ArbeidsforholdDto(
    val personId: Long,
    val arbeidsgiverId: Long,
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
    val stillingsprosent: Int,
    val stillingstittel: String,
)
