package no.nav.helse.modell.arbeidsgiver

class Arbeidsgiverinformasjonløsning(private val arbeidsgivere: List<ArbeidsgiverDto>) {
    internal fun relevantLøsning(organisasjonsnummer: String) = arbeidsgivere.find { it.orgnummer == organisasjonsnummer }

    data class ArbeidsgiverDto(
        val orgnummer: String,
        val navn: String,
    )
}
