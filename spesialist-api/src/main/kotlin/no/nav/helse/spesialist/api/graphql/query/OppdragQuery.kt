package no.nav.helse.spesialist.api.graphql.query

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.graphql.schema.Oppdrag
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.utbetaling.UtbetalingApiDao

class OppdragQuery(
    personApiDao: PersonApiDao,
    egenAnsattApiDao: EgenAnsattApiDao,
    private val utbetalingApiDao: UtbetalingApiDao,
) : AbstractPersonQuery(personApiDao, egenAnsattApiDao) {
    fun oppdrag(
        fnr: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<List<Oppdrag>> {
        if (!personApiDao.finnesPersonMedFødselsnummer(fnr)) {
            return DataFetcherResult.newResult<List<Oppdrag>>().error(getNotFoundError(fnr)).build()
        }

        if (isForbidden(fnr, env)) {
            return DataFetcherResult.newResult<List<Oppdrag>>().error(getForbiddenError(fnr)).build()
        }

        val oppdrag =
            utbetalingApiDao.findUtbetalinger(fnr).map { utbetaling ->
                Oppdrag(utbetaling)
            }

        return DataFetcherResult.newResult<List<Oppdrag>>().data(oppdrag).build()
    }
}
