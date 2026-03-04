package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.application.InfotrygdutbetalingerRepository
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.InfotrygdUtbetalinger

class HentInfotrygdutbetalingerløsning(
    private val utbetalinger: JsonNode,
) {
    fun oppdater(
        infotrygdutbetalingerRepository: InfotrygdutbetalingerRepository,
        fødselsnummer: String,
    ) {
        infotrygdutbetalingerRepository.lagre(
            InfotrygdUtbetalinger.Factory.ny(
                id = Identitetsnummer.fraString(fødselsnummer),
                data = utbetalinger.toString(),
            ),
        )
    }
}
