package no.nav.helse.arbeidsgiver

data class ArbeidsgiverDto(
    val organisasjonsnummer: String,
    val navn: String,
    val bransjer: List<String>
)
