package no.nav.helse.spesialist.db.dao.api

import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.db.api.VarselDbDto
import no.nav.helse.db.api.VarselDbDto.Varselstatus.AKTIV
import no.nav.helse.db.api.VarselDbDto.Varselstatus.GODKJENT
import no.nav.helse.db.api.VedtaksperiodeDbDto
import java.util.UUID
import javax.sql.DataSource

class PgVarselApiRepository internal constructor(dataSource: DataSource) : VarselApiRepository {
    private val varselDao = PgVarselApiDao(dataSource)
    private val behandlingDao = PgBehandlingApiDao(dataSource)

    override fun finnVarslerSomIkkeErInaktiveFor(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<VarselDbDto> {
        return varselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId)
    }

    override fun finnVarslerSomIkkeErInaktiveForSisteGenerasjon(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<VarselDbDto> {
        return varselDao.finnVarslerSomIkkeErInaktiveForSisteGenerasjon(vedtaksperiodeId, utbetalingId)
    }

    override fun finnVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<VarselDbDto> {
        return varselDao.finnVarslerForUberegnetPeriode(vedtaksperiodeId)
    }

    override fun finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<VarselDbDto> {
        return varselDao.finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId)
    }

    override fun godkjennVarslerFor(oppgaveId: Long) {
        val vedtaksperioder = sammenhengendePerioder(oppgaveId)
        varselDao.godkjennVarslerFor(vedtaksperioder.map { it.vedtaksperiodeId })
    }

    override fun vurderVarselFor(
        varselId: UUID,
        gjeldendeStatus: VarselDbDto.Varselstatus,
        saksbehandlerIdent: String,
    ) {
        varselDao.vurderVarselFor(varselId, gjeldendeStatus, saksbehandlerIdent)
    }

    override fun erAktiv(
        varselkode: String,
        generasjonId: UUID,
    ): Boolean? {
        val varselstatus = varselDao.finnStatusFor(varselkode, generasjonId) ?: return null
        return varselstatus == AKTIV
    }

    override fun erGodkjent(
        varselkode: String,
        generasjonId: UUID,
    ): Boolean? {
        val varselstatus = varselDao.finnStatusFor(varselkode, generasjonId) ?: return null
        return varselstatus == GODKJENT
    }

    override fun settStatusVurdert(
        generasjonId: UUID,
        definisjonId: UUID,
        varselkode: String,
        ident: String,
    ): VarselDbDto? {
        return varselDao.settStatusVurdert(generasjonId, definisjonId, varselkode, ident)
    }

    override fun settStatusAktiv(
        generasjonId: UUID,
        varselkode: String,
        ident: String,
    ): VarselDbDto? {
        return varselDao.settStatusAktiv(generasjonId, varselkode, ident)
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

    private fun Set<VedtaksperiodeDbDto>.tidligereEnnOgSammenhengende(periode: VedtaksperiodeDbDto): Set<VedtaksperiodeDbDto> {
        return this.filter { other ->
            other.tidligereEnnOgSammenhengende(periode)
        }.toSet()
    }
}
