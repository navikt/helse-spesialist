package no.nav.helse.modell.person

import java.time.LocalDate

internal class HentPersoninfoLøsning(
    internal val fornavn: String,
    internal val mellomnavn: String?,
    internal val etternavn: String,
    internal val fødselsdato: LocalDate,
    internal val kjønn: Kjønn
) {

    internal fun lagre(personDao: PersonDao) =
        personDao.insertPersoninfo(fornavn, mellomnavn, etternavn, fødselsdato, kjønn)

    internal fun oppdater(personDao: PersonDao, fødselsnummer: String) =
        personDao.updatePersoninfo(
            fødselsnummer = fødselsnummer.toLong(),
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            fødselsdato = fødselsdato,
            kjønn = kjønn
        )
}

enum class Kjønn { Mann, Kvinne, Ukjent }

internal enum class PersonEgenskap(private val diskresjonskode: String) {
    Kode6("SPSF"), Kode7("SPFO"); // TODO: Hvilke fler egenskaper kan man ha?

    companion object {
        internal fun find(diskresjonskode: String?) = values().firstOrNull { it.diskresjonskode == diskresjonskode }
    }
}
