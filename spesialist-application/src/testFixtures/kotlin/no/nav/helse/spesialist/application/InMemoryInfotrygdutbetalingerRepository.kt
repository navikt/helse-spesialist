package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.InfotrygdUtbetalinger

class InMemoryInfotrygdutbetalingerRepository : InfotrygdutbetalingerRepository,
    AbstractInMemoryRepository<Identitetsnummer, InfotrygdUtbetalinger>() {
    override fun deepCopy(original: InfotrygdUtbetalinger) =
        InfotrygdUtbetalinger.Factory.fraLagring(
            id = original.id,
            data = original.data,
            oppdatert = original.oppdatert,
        )
}
