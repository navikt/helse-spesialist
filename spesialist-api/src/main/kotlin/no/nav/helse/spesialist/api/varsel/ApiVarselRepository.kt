package no.nav.helse.spesialist.api.varsel

import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.spesialist.api.graphql.schema.VarselDTO
import no.nav.helse.spesialist.api.varsel.Varsel.Companion.toDto

class ApiVarselRepository(dataSource: DataSource) {

    private val varselDao = ApiVarselDao(dataSource)

    internal fun finnVarslerFor(vedtaksperiodeId: UUID, utbetalingId: UUID): List<VarselDTO> {
        return varselDao.finnVarslerFor(vedtaksperiodeId, utbetalingId).toDto()
    }

}