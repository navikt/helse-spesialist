package no.nav.helse.spesialist.api.rest.personer.saksbilde

import no.nav.helse.db.OppgaveDao
import no.nav.helse.spesialist.api.rest.personer.saksbilde.OppgaveMapper.tilEgenskaperForVisning
import no.nav.helse.spesialist.api.rest.saksbilde.ApiOppgaveegenskap
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
