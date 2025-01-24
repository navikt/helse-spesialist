package no.nav.helse.db.api

import no.nav.helse.spesialist.api.graphql.schema.VarselDTO
import no.nav.helse.spesialist.api.varsel.Varsel
import no.nav.helse.spesialist.api.varsel.Varsel.Companion.toDto
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.AKTIV
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.GODKJENT
import no.nav.helse.spesialist.api.vedtak.Vedtaksperiode
import java.util.UUID
import javax.sql.DataSource

class PgVarselApiRepository internal constructor(dataSource: DataSource) : VarselApiRepository {
    private val varselDao = PgVarselApiDao(dataSource)
    private val generasjonDao = PgGenerasjonApiDao(dataSource)

    override fun finnVarslerSomIkkeErInaktiveFor(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<VarselDTO> {
        return varselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId).toDto()
    }

    override fun finnVarslerSomIkkeErInaktiveForSisteGenerasjon(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<VarselDTO> {
        return varselDao.finnVarslerSomIkkeErInaktiveForSisteGenerasjon(vedtaksperiodeId, utbetalingId).toDto()
    }

    override fun finnVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<VarselDTO> {
        return varselDao.finnVarslerForUberegnetPeriode(vedtaksperiodeId).toDto()
    }

    override fun finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<VarselDTO> {
        return varselDao.finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId).toDto()
    }

    override fun godkjennVarslerFor(oppgaveId: Long) {
        val vedtaksperioder = sammenhengendePerioder(oppgaveId)
        varselDao.godkjennVarslerFor(vedtaksperioder.map { it.vedtaksperiodeId() })
    }

    override fun vurderVarselFor(
        varselId: UUID,
        gjeldendeStatus: Varsel.Varselstatus,
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
    ): VarselDTO? {
        return varselDao.settStatusVurdert(generasjonId, definisjonId, varselkode, ident)?.toDto()
    }

    override fun settStatusAktiv(
        generasjonId: UUID,
        varselkode: String,
        ident: String,
    ): VarselDTO? {
        return varselDao.settStatusAktiv(generasjonId, varselkode, ident)?.toDto()
    }

    override fun perioderSomSkalViseVarsler(oppgaveId: Long?): Set<UUID> {
        if (oppgaveId == null) return emptySet()
        return sammenhengendePerioder(oppgaveId).map { it.vedtaksperiodeId() }.toSet()
    }

    private fun sammenhengendePerioder(oppgaveId: Long): Set<Vedtaksperiode> {
        val vedtakMedOppgave = generasjonDao.gjeldendeGenerasjonFor(oppgaveId)
        val alleVedtakForPersonen = generasjonDao.gjeldendeGenerasjonerForPerson(oppgaveId)
        val sammenhengendePerioder = alleVedtakForPersonen.tidligereEnnOgSammenhengende(vedtakMedOppgave)
        return setOf(vedtakMedOppgave) + sammenhengendePerioder
    }

    private fun Set<Vedtaksperiode>.tidligereEnnOgSammenhengende(periode: Vedtaksperiode): Set<Vedtaksperiode> {
        return this.filter { other ->
            other.tidligereEnnOgSammenhengende(periode)
        }.toSet()
    }
}
