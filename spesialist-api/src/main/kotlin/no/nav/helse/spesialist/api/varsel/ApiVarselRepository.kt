package no.nav.helse.spesialist.api.varsel

import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.spesialist.api.graphql.schema.VarselDTO
import no.nav.helse.spesialist.api.varsel.Varsel.Companion.antallIkkeVurderte
import no.nav.helse.spesialist.api.varsel.Varsel.Companion.toDto
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.AKTIV
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.GODKJENT
import no.nav.helse.spesialist.api.vedtak.ApiVedtak
import no.nav.helse.spesialist.api.vedtak.ApiVedtakDao

class ApiVarselRepository(dataSource: DataSource) {

    private val varselDao = ApiVarselDao(dataSource)
    private val vedtakDao = ApiVedtakDao(dataSource)

    internal fun finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId: UUID, utbetalingId: UUID): Set<VarselDTO> {
        return varselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId).toDto()
    }
    internal fun finnVarslerSomIkkeErInaktiveForSisteGenerasjon(vedtaksperiodeId: UUID, utbetalingId: UUID): Set<VarselDTO> {
        return varselDao.finnVarslerSomIkkeErInaktiveForSisteGenerasjon(vedtaksperiodeId, utbetalingId).toDto()
    }

    internal fun finnVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<VarselDTO> {
        return varselDao.finnVarslerForUberegnetPeriode(vedtaksperiodeId).toDto()
    }

    internal fun finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId: UUID): Set<VarselDTO> {
        return varselDao.finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId).toDto()
    }

    fun ikkeVurderteVarslerFor(oppgaveId: Long): Int {
        val vedtaksperioder = sammenhengendePerioder(oppgaveId)
        val alleVarsler = varselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperioder.map { it.vedtaksperiodeId() })
        return alleVarsler.antallIkkeVurderte()
    }

    fun godkjennVarslerFor(oppgaveId: Long) {
        val vedtaksperioder = sammenhengendePerioder(oppgaveId)
        varselDao.godkjennVarslerFor(vedtaksperioder.map { it.vedtaksperiodeId() })
    }

    internal fun erAktiv(varselkode: String, generasjonId: UUID): Boolean? {
        val varselstatus = varselDao.finnStatusFor(varselkode, generasjonId) ?: return null
        return varselstatus == AKTIV
    }

    internal fun erGodkjent(varselkode: String, generasjonId: UUID): Boolean? {
        val varselstatus = varselDao.finnStatusFor(varselkode, generasjonId) ?: return null
        return varselstatus == GODKJENT
    }

    internal fun settStatusVurdert(
        generasjonId: UUID,
        definisjonId: UUID,
        varselkode: String,
        ident: String,
    ): VarselDTO? {
        return varselDao.settStatusVurdert(generasjonId, definisjonId, varselkode, ident)?.toDto()
    }

    internal fun settStatusAktiv(
        generasjonId: UUID,
        varselkode: String,
        ident: String
    ): VarselDTO? {
        return varselDao.settStatusAktiv(generasjonId, varselkode, ident)?.toDto()
    }

    internal fun perioderSomSkalViseVarsler(oppgaveId: Long?): Set<UUID> {
        if (oppgaveId == null) return emptySet()
        return sammenhengendePerioder(oppgaveId).map { it.vedtaksperiodeId() }.toSet()
    }

    private fun sammenhengendePerioder(oppgaveId: Long): Set<ApiVedtak> {
        val vedtakMedOppgave = vedtakDao.vedtakFor(oppgaveId)
        val alleVedtakForPersonen = vedtakDao.alleVedtakForPerson(oppgaveId)
        val sammenhengendePerioder = alleVedtakForPersonen.tidligereEnnOgSammenhengende(vedtakMedOppgave)
        return setOf(vedtakMedOppgave) + sammenhengendePerioder
    }

    private fun Set<ApiVedtak>.tidligereEnnOgSammenhengende(periode: ApiVedtak): Set<ApiVedtak> {
        return this.filter { other ->
            other.tidligereEnnOgSammenhengende(periode)
        }.toSet()
    }

}