package no.nav.helse.modell.arbeidsgiver

data class ArbeidsgiverDto(
    val organisasjonsnummer: String,
    val navn: String,
    val bransjer: List<String>
)
