package no.nav.helse.spesialist.api.graphql

import no.nav.helse.db.OppgaveDao
import no.nav.helse.spesialist.api.graphql.OppgaveMapper.tilEgenskaperForVisning
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveegenskap
import java.util.UUID

class ApiOppgaveService(
    private val oppgaveDao: OppgaveDao,
) {
    fun hentEgenskaper(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): List<ApiOppgaveegenskap> {
        val egenskaper =
            oppgaveDao.finnEgenskaper(
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
            )

        return egenskaper?.tilEgenskaperForVisning() ?: emptyList()
    }
}
