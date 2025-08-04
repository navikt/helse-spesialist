package no.nav.helse.spesialist.db.dao.api

import no.nav.helse.db.api.BehandlingApiRepository
import no.nav.helse.db.api.VedtaksperiodeDbDto
import javax.sql.DataSource

class PgBehandlingApiRepository internal constructor(
    dataSource: DataSource,
) : BehandlingApiRepository {
    private val varselDao = PgVarselApiDao(dataSource)
    private val behandlingDao = PgBehandlingApiDao(dataSource)

    override fun perioderTilBehandling(oppgaveId: Long): Set<VedtaksperiodeDbDto> {
        val periodeTilGodkjenning = behandlingDao.gjeldendeBehandlingFor(oppgaveId, varselDao::finnVarslerFor)
        val gjeldendeBehandlingerForPersonen = behandlingDao.gjeldendeBehandlingerForPerson(oppgaveId, varselDao::finnVarslerFor)
        val sammenhengendePerioder = gjeldendeBehandlingerForPersonen.tidligereEnnOgSammenhengende(periodeTilGodkjenning)
        return sammenhengendePerioder + periodeTilGodkjenning
    }

    override fun periodeTilGodkjenning(oppgaveId: Long): VedtaksperiodeDbDto = behandlingDao.gjeldendeBehandlingFor(oppgaveId, varselDao::finnVarslerFor)

    private fun Set<VedtaksperiodeDbDto>.tidligereEnnOgSammenhengende(periode: VedtaksperiodeDbDto): Set<VedtaksperiodeDbDto> =
        this
            .filter { other ->
                other.tidligereEnnOgSammenhengende(periode)
            }.toSet()
}
