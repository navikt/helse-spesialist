package no.nav.helse.spesialist.api.vedtaksperiode

import javax.sql.DataSource
import no.nav.helse.spesialist.api.varsel.ApiVarselDao
import no.nav.helse.spesialist.api.vedtak.ApiVedtak
import no.nav.helse.spesialist.api.vedtak.ApiVedtakDao

class ApiGenerasjonRepository(dataSource: DataSource) {

    private val varselDao = ApiVarselDao(dataSource)
    private val vedtakDao = ApiVedtakDao(dataSource)

    internal fun perioderTilBehandling(oppgaveId: Long): Set<ApiVedtak> {
        val periodeTilGodkjenning = vedtakDao.vedtakFor(oppgaveId, varselDao::finnVarslerFor)
        val alleVedtakForPersonen = vedtakDao.alleVedtakForPerson(oppgaveId, varselDao::finnVarslerFor)
        val sammenhengendePerioder = alleVedtakForPersonen.tidligereEnnOgSammenhengende(periodeTilGodkjenning)
        return sammenhengendePerioder + periodeTilGodkjenning
    }

    private fun Set<ApiVedtak>.tidligereEnnOgSammenhengende(periode: ApiVedtak): Set<ApiVedtak> {
        return this.filter { other ->
            other.tidligereEnnOgSammenhengende(periode)
        }.toSet()
    }
}