package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode

internal class HentInfotrygdutbetalingerLøsning(internal val utbetalinger: JsonNode) {

    internal fun lagre(personDao: PersonDao) =
        personDao.insertInfotrygdutbetalinger(utbetalinger)

    fun oppdater(personDao: PersonDao, fødselsnummer: String) {
        if (personDao.findInfotrygdutbetalinger(fødselsnummer.toLong()) != null) {
            personDao.updateInfotrygdutbetalinger(fødselsnummer.toLong(), utbetalinger)
        } else {
            val utbetalingRef = personDao.insertInfotrygdutbetalinger(utbetalinger)
            personDao.updateInfotrygdutbetalingerRef(fødselsnummer.toLong(), utbetalingRef)
        }
    }
}
