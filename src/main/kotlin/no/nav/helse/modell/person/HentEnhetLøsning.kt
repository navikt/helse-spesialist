package no.nav.helse.modell.person

internal class HentEnhetLøsning(internal val enhetNr: String) {

    internal fun lagrePerson(personDao: PersonDao, fødselsnummer: String, aktørId: String, navnId: Int, infotrygdutbetalingerId: Int) =
        personDao.insertPerson(
            fødselsnummer = fødselsnummer.toLong(),
            aktørId = aktørId.toLong(),
            navnId = navnId,
            enhetId = enhetNr.toInt(),
            infotrygdutbetalingerId = infotrygdutbetalingerId
        )

    fun oppdater(personDao: PersonDao, fødselsnummer: String) =
        personDao.updateEnhet(fødselsnummer.toLong(), enhetNr.toInt())
}
