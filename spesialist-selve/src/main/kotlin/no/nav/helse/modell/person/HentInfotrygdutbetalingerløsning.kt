package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.PersonRepository

internal class HentInfotrygdutbetalingerløsning(private val utbetalinger: JsonNode) {
    fun oppdater(
        personRepository: PersonRepository,
        fødselsnummer: String,
    ) {
        personRepository.upsertInfotrygdutbetalinger(fødselsnummer, utbetalinger)
    }
}
