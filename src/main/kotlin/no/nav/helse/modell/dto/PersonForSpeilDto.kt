package no.nav.helse.modell.dto

import java.util.*


data class PersonForSpeilDto(
    val aktørId: String,
    val fødselsnummer: String,
    val navn: NavnDto,
    val arbeidsgivere: List<ArbeidsgiverForSpeilDto>
)

data class ArbeidsgiverForSpeilDto(
    val organisasjonsnummer: String,
    val navn: String,
    val id: UUID,
    val vedtaksperioder: List<VedtaksperiodeDTOBase>
)
