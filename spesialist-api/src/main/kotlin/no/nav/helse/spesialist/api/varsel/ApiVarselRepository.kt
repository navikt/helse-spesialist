package no.nav.helse.spesialist.api.varsel

import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.spesialist.api.graphql.schema.VarselDTO
import no.nav.helse.spesialist.api.varsel.Varsel.Companion.antallIkkeVurderte
import no.nav.helse.spesialist.api.varsel.Varsel.Companion.toDto

class ApiVarselRepository(dataSource: DataSource) {

    private val varselDao = ApiVarselDao(dataSource)

    internal fun finnVarslerFor(vedtaksperiodeId: UUID, utbetalingId: UUID): List<VarselDTO> {
        return varselDao.finnVarslerFor(vedtaksperiodeId, utbetalingId).toDto()
    }

    fun ikkeVurderteVarslerFor(oppgaveId: Long): Int {
        val alleVarsler = varselDao.finnVarslerFor(oppgaveId) ?: throw IllegalArgumentException("Ugyldig oppgaveId")
        return alleVarsler.antallIkkeVurderte()
    }

    fun settStatusVurdert(
        generasjonId: UUID,
        definisjonId: UUID,
        varselkode: String,
        ident: String,
    ): Int {
        return varselDao.settStatusVurdert(generasjonId, definisjonId, varselkode, ident)
    }

    fun settStatusAktiv(
        generasjonId: UUID,
        varselkode: String,
        ident: String
    ): Int {
        return varselDao.settStatusAktiv(generasjonId, varselkode, ident)
    }

}