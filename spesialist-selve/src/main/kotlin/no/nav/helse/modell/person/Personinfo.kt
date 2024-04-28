package no.nav.helse.modell.person

import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.typer.Kjønn
import java.time.LocalDate

internal class HentPersoninfoløsninger(private val løsninger: List<HentPersoninfoløsning>) {
    internal fun opprett(dao: ArbeidsgiverDao) {
        løsninger.forEach { it.lagre(dao) }
    }
}

internal class HentPersoninfoløsning(
    private val ident: String,
    private val fornavn: String,
    private val mellomnavn: String?,
    private val etternavn: String,
    private val fødselsdato: LocalDate,
    private val kjønn: Kjønn,
    private val adressebeskyttelse: Adressebeskyttelse,
) {
    internal fun lagre(personDao: PersonDao): Long =
        personDao.insertPersoninfo(fornavn, mellomnavn, etternavn, fødselsdato, kjønn, adressebeskyttelse)

    internal fun lagre(dao: ArbeidsgiverDao) {
        dao.upsertNavn(ident, "$fornavn $etternavn")
        dao.upsertBransjer(ident, listOf(BRANSJE_PRIVATPERSON))
    }

    internal fun oppdater(
        personDao: PersonDao,
        fødselsnummer: String,
    ) = personDao.upsertPersoninfo(
        fødselsnummer = fødselsnummer,
        fornavn = fornavn,
        mellomnavn = mellomnavn,
        etternavn = etternavn,
        fødselsdato = fødselsdato,
        kjønn = kjønn,
        adressebeskyttelse = adressebeskyttelse,
    )

    private companion object {
        private const val BRANSJE_PRIVATPERSON = "Privatperson"
    }
}
