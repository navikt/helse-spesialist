package no.nav.helse.spesialist.db.dao.api

import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.db.api.VarselDbDto
import no.nav.helse.db.api.VedtaksperiodeDbDto
import java.util.UUID
import javax.sql.DataSource

class PgVarselApiRepository internal constructor(
    dataSource: DataSource,
) : VarselApiRepository {
    private val varselDao = PgVarselApiDao(dataSource)
    private val behandlingDao = PgBehandlingApiDao(dataSource)

    override fun finnVarslerSomIkkeErInaktiveFor(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<VarselDbDto> = varselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId)

    override fun finnVarslerSomIkkeErInaktiveForSisteGenerasjon(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<VarselDbDto> = varselDao.finnVarslerSomIkkeErInaktiveForSisteGenerasjon(vedtaksperiodeId, utbetalingId)

    override fun finnVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<VarselDbDto> = varselDao.finnVarslerForUberegnetPeriode(vedtaksperiodeId)

    override fun finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<VarselDbDto> = varselDao.finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId)

    override fun vurderVarselFor(
        varselId: UUID,
        gjeldendeStatus: VarselDbDto.Varselstatus,
        saksbehandlerIdent: String,
    ) {
        varselDao.vurderVarselFor(varselId, gjeldendeStatus, saksbehandlerIdent)
    }

    override fun perioderSomSkalViseVarsler(oppgaveId: Long?): Set<UUID> {
        if (oppgaveId == null) return emptySet()
        return sammenhengendePerioder(oppgaveId).map { it.vedtaksperiodeId }.toSet()
    }

    private fun sammenhengendePerioder(oppgaveId: Long): Set<VedtaksperiodeDbDto> {
        val vedtaksperiodeMedOppgave = behandlingDao.gjeldendeBehandlingFor(oppgaveId)
        val alleVedtaksperioderForPersonen = behandlingDao.gjeldendeBehandlingerForPerson(oppgaveId)
        val sammenhengendePerioder = alleVedtaksperioderForPersonen.tidligereEnnOgSammenhengende(vedtaksperiodeMedOppgave)
        return sammenhengendePerioder + vedtaksperiodeMedOppgave
    }

    private fun Set<VedtaksperiodeDbDto>.tidligereEnnOgSammenhengende(periode: VedtaksperiodeDbDto): Set<VedtaksperiodeDbDto> =
        this
            .filter { other ->
                other.tidligereEnnOgSammenhengende(periode)
            }.toSet()
}
