package no.nav.helse.modell.arbeidsgiver

internal class Arbeidsgiverinformasjonløsning(private val arbeidsgivere: List<ArbeidsgiverDto>) {
    internal fun relevantLøsning(organisasjonsnummer: String) = arbeidsgivere.find { it.orgnummer == organisasjonsnummer }

    internal data class ArbeidsgiverDto(
        val orgnummer: String,
        val navn: String,
        val bransjer: List<String>,
    )
}
