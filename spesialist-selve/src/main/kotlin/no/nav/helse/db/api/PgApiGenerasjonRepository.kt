package no.nav.helse.db.api

import no.nav.helse.spesialist.api.vedtak.Vedtaksperiode
import javax.sql.DataSource

class PgApiGenerasjonRepository(dataSource: DataSource) : ApiGenerasjonRepository {
    private val varselDao = PgApiVarselDao(dataSource)
    private val generasjonDao = PgGenerasjonDao(dataSource)

    override fun perioderTilBehandling(oppgaveId: Long): Set<Vedtaksperiode> {
        val periodeTilGodkjenning = generasjonDao.gjeldendeGenerasjonFor(oppgaveId, varselDao::finnVarslerFor)
        val gjeldendeGenerasjonerForPersonen = generasjonDao.gjeldendeGenerasjonerForPerson(oppgaveId, varselDao::finnVarslerFor)
        val sammenhengendePerioder = gjeldendeGenerasjonerForPersonen.tidligereEnnOgSammenhengende(periodeTilGodkjenning)
        return sammenhengendePerioder + periodeTilGodkjenning
    }

    override fun periodeTilGodkjenning(oppgaveId: Long): Vedtaksperiode {
        return generasjonDao.gjeldendeGenerasjonFor(oppgaveId, varselDao::finnVarslerFor)
    }

    private fun Set<Vedtaksperiode>.tidligereEnnOgSammenhengende(periode: Vedtaksperiode): Set<Vedtaksperiode> {
        return this.filter { other ->
            other.tidligereEnnOgSammenhengende(periode)
        }.toSet()
    }
}
