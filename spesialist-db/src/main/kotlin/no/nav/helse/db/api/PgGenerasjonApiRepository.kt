package no.nav.helse.db.api

import javax.sql.DataSource

class PgGenerasjonApiRepository internal constructor(dataSource: DataSource) : GenerasjonApiRepository {
    private val varselDao = PgVarselApiDao(dataSource)
    private val generasjonDao = PgGenerasjonApiDao(dataSource)

    override fun perioderTilBehandling(oppgaveId: Long): Set<VedtaksperiodeDbDto> {
        val periodeTilGodkjenning = generasjonDao.gjeldendeGenerasjonFor(oppgaveId, varselDao::finnVarslerFor)
        val gjeldendeGenerasjonerForPersonen = generasjonDao.gjeldendeGenerasjonerForPerson(oppgaveId, varselDao::finnVarslerFor)
        val sammenhengendePerioder = gjeldendeGenerasjonerForPersonen.tidligereEnnOgSammenhengende(periodeTilGodkjenning)
        return sammenhengendePerioder + periodeTilGodkjenning
    }

    override fun periodeTilGodkjenning(oppgaveId: Long): VedtaksperiodeDbDto {
        return generasjonDao.gjeldendeGenerasjonFor(oppgaveId, varselDao::finnVarslerFor)
    }

    private fun Set<VedtaksperiodeDbDto>.tidligereEnnOgSammenhengende(periode: VedtaksperiodeDbDto): Set<VedtaksperiodeDbDto> {
        return this.filter { other ->
            other.tidligereEnnOgSammenhengende(periode)
        }.toSet()
    }
}
