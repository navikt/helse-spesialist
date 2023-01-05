package no.nav.helse.spesialist.api.varsel

import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.spesialist.api.graphql.schema.VarselDTO
import no.nav.helse.spesialist.api.varsel.Varsel.Companion.antallIkkeVurderte
import no.nav.helse.spesialist.api.varsel.Varsel.Companion.antallIkkeVurderteEkskludertBesluttervarsler
import no.nav.helse.spesialist.api.varsel.Varsel.Companion.toDto

class ApiVarselRepository(dataSource: DataSource) {

    private val varselDao = ApiVarselDao(dataSource)

    internal fun finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId: UUID, utbetalingId: UUID): List<VarselDTO> {
        return varselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId).toDto()
    }

    fun ikkeVurderteVarslerFor(oppgaveId: Long): Int {
        val alleVarsler = varselDao.finnVarslerSomIkkeErInaktiveFor(oppgaveId)
        return alleVarsler.antallIkkeVurderte()
    }

    fun ikkeVurderteVarslerEkskludertBesluttervarslerFor(oppgaveId: Long): Int {
        val alleVarsler = varselDao.finnVarslerSomIkkeErInaktiveFor(oppgaveId)
        return alleVarsler.antallIkkeVurderteEkskludertBesluttervarsler()
    }

    fun godkjennVarslerFor(oppgaveId: Long) {
        varselDao.godkjennVarslerFor(oppgaveId)
    }

    fun settStatusVurdert(
        generasjonId: UUID,
        definisjonId: UUID,
        varselkode: String,
        ident: String,
    ): Int {
        return varselDao.settStatusVurdert(generasjonId, definisjonId, varselkode, ident)
    }

    fun settStatusVurdertPåBeslutteroppgavevarsler(oppgaveId: Long, ident: String) {
        varselDao.settStatusVurdertPåBeslutteroppgavevarsler(oppgaveId, ident)
    }

    fun settStatusAktiv(
        generasjonId: UUID,
        varselkode: String,
        ident: String
    ): Int {
        return varselDao.settStatusAktiv(generasjonId, varselkode, ident)
    }

}