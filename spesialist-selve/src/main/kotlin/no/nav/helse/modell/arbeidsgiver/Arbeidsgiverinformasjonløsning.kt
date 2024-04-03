package no.nav.helse.modell.arbeidsgiver

internal class Arbeidsgiverinformasjonløsning(private val arbeidsgivere: List<ArbeidsgiverDto>) {
    internal fun opprett(arbeidsgiverDao: ArbeidsgiverDao) {
        arbeidsgivere.forEach {
            arbeidsgiverDao.insertArbeidsgiver(it.orgnummer, it.navn, it.bransjer)
        }
    }

    internal fun oppdater(arbeidsgiverDao: ArbeidsgiverDao) {
        arbeidsgivere.forEach {
            arbeidsgiverDao.upsertNavn(it.orgnummer, it.navn)
            arbeidsgiverDao.upsertBransjer(it.orgnummer, it.bransjer)
        }
    }

    internal fun harSvarForAlle(orgnumre: List<String>) = arbeidsgivere.map { it.orgnummer }.containsAll(orgnumre)

    internal data class ArbeidsgiverDto(
        val orgnummer: String,
        val navn: String,
        val bransjer: List<String>,
    )
}
