package no.nav.helse.spesialist.api.vedtaksperiode

import javax.sql.DataSource
import no.nav.helse.spesialist.api.varsel.ApiVarselDao
import no.nav.helse.spesialist.api.vedtak.ApiGenerasjon
import no.nav.helse.spesialist.api.vedtak.ApiGenerasjonDao

class ApiGenerasjonRepository(dataSource: DataSource) {

    private val varselDao = ApiVarselDao(dataSource)
    private val generasjonDao = ApiGenerasjonDao(dataSource)

    internal fun perioderTilBehandling(oppgaveId: Long): Set<ApiGenerasjon> {
        val periodeTilGodkjenning = generasjonDao.gjeldendeGenerasjonFor(oppgaveId, varselDao::finnVarslerFor)
        val gjeldendeGenerasjonerForPersonen = generasjonDao.gjeldendeGenerasjonerForPerson(oppgaveId, varselDao::finnVarslerFor)
        val sammenhengendePerioder = gjeldendeGenerasjonerForPersonen.tidligereEnnOgSammenhengende(periodeTilGodkjenning)
        return sammenhengendePerioder + periodeTilGodkjenning
    }

    private fun Set<ApiGenerasjon>.tidligereEnnOgSammenhengende(periode: ApiGenerasjon): Set<ApiGenerasjon> {
        return this.filter { other ->
            other.tidligereEnnOgSammenhengende(periode)
        }.toSet()
    }
}