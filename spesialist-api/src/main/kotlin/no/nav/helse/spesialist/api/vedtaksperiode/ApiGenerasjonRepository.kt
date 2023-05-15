package no.nav.helse.spesialist.api.vedtaksperiode

import javax.sql.DataSource
import no.nav.helse.spesialist.api.varsel.ApiVarselDao
import no.nav.helse.spesialist.api.vedtak.GenerasjonDao
import no.nav.helse.spesialist.api.vedtak.Vedtaksperiode

internal class ApiGenerasjonRepository(dataSource: DataSource) {

    private val varselDao = ApiVarselDao(dataSource)
    private val generasjonDao = GenerasjonDao(dataSource)

    internal fun perioderTilBehandling(oppgaveId: Long): Set<Vedtaksperiode> {
        val periodeTilGodkjenning = generasjonDao.gjeldendeGenerasjonFor(oppgaveId, varselDao::finnVarslerFor)
        val gjeldendeGenerasjonerForPersonen = generasjonDao.gjeldendeGenerasjonerForPerson(oppgaveId, varselDao::finnVarslerFor)
        val sammenhengendePerioder = gjeldendeGenerasjonerForPersonen.tidligereEnnOgSammenhengende(periodeTilGodkjenning)
        return sammenhengendePerioder + periodeTilGodkjenning
    }

    private fun Set<Vedtaksperiode>.tidligereEnnOgSammenhengende(periode: Vedtaksperiode): Set<Vedtaksperiode> {
        return this.filter { other ->
            other.tidligereEnnOgSammenhengende(periode)
        }.toSet()
    }
}