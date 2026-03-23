package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.InfotrygdUtbetalinger

interface InfotrygdutbetalingerRepository {
    fun finn(identitetsnummer: Identitetsnummer): InfotrygdUtbetalinger?

    fun lagre(infotrygdUtbetalinger: InfotrygdUtbetalinger)
}
