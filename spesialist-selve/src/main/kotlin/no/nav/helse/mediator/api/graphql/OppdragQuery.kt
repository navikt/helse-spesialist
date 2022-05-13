package no.nav.helse.mediator.api.graphql

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.mediator.api.graphql.schema.Oppdrag
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.person.PersonApiDao

class OppdragQuery(
    personApiDao: PersonApiDao,
    egenAnsattDao: EgenAnsattDao,
    private val utbetalingDao: UtbetalingDao,
) : AbstractPersonQuery(personApiDao, egenAnsattDao) {

    fun oppdrag(fnr: String, env: DataFetchingEnvironment): DataFetcherResult<List<Oppdrag>> {
        if (isForbidden(fnr, env)) {
            return DataFetcherResult.newResult<List<Oppdrag>>().error(getForbiddenError(fnr)).build()
        }

        val oppdrag = utbetalingDao.findUtbetalinger(fnr).map { utbetaling ->
            Oppdrag(utbetaling)
        }

        return DataFetcherResult.newResult<List<Oppdrag>>().data(oppdrag).build()
    }

}
