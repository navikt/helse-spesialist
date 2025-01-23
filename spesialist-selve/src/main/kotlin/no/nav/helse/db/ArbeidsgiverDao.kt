package no.nav.helse.db

interface ArbeidsgiverDao {
    fun findArbeidsgiverByOrgnummer(organisasjonsnummer: String): Long?

    fun insertMinimalArbeidsgiver(organisasjonsnummer: String)

    fun upsertNavn(
        orgnummer: String,
        navn: String,
    )

    fun upsertBransjer(
        orgnummer: String,
        bransjer: List<String>,
    )
}
