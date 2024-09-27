package no.nav.helse.modell.person

import no.nav.helse.db.PersonRepository

internal class HentEnhetløsning(private val enhetNr: String) {
    internal companion object {
        private val UTLANDSENHETER = setOf("0393", "2101")

        internal fun erEnhetUtland(enhet: String) = enhet in UTLANDSENHETER
    }

    internal fun lagrePerson(
        personDao: PersonDao,
        fødselsnummer: String,
        aktørId: String,
        personinfoId: Long,
        infotrygdutbetalingerId: Long,
    ) = personDao.insertPerson(
        fødselsnummer = fødselsnummer,
        aktørId = aktørId,
        personinfoId = personinfoId,
        enhetId = enhetNr.toInt(),
        infotrygdutbetalingerId = infotrygdutbetalingerId,
    )

    fun oppdater(
        personRepository: PersonRepository,
        fødselsnummer: String,
    ) = personRepository.oppdaterEnhet(fødselsnummer, enhetNr.toInt())
}
