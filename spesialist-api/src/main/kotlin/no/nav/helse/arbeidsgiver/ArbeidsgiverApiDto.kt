package no.nav.helse.arbeidsgiver

data class ArbeidsgiverApiDto(
    val organisasjonsnummer: String,
    val navn: String,
    val bransjer: List<String>
)
