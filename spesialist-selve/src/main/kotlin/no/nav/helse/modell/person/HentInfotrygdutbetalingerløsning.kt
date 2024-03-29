package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode

internal class HentInfotrygdutbetalingerløsning(private val utbetalinger: JsonNode) {

    fun oppdater(personDao: PersonDao, fødselsnummer: String) {
        personDao.upsertInfotrygdutbetalinger(fødselsnummer, utbetalinger)
    }
}
