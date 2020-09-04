package no.nav.helse.modell.arbeidsgiver

internal class ArbeidsgiverLøsning(internal val navn: String) {
    internal fun oppdater(arbeidsgiverDao: ArbeidsgiverDao, orgnummer: String) =
        arbeidsgiverDao.updateNavn(orgnummer, navn)
}
