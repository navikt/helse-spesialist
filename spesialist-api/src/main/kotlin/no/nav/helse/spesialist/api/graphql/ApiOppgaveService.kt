package no.nav.helse.spesialist.api.graphql

import no.nav.helse.db.OppgaveDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.spesialist.api.graphql.OppgaveMapper.tilApiversjon
import no.nav.helse.spesialist.api.graphql.OppgaveMapper.tilEgenskaperForVisning
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallOppgaver
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveegenskap
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper
import java.util.UUID

class ApiOppgaveService(
    private val oppgaveDao: OppgaveDao,
    private val oppgaveService: OppgaveService,
) {
    fun antallOppgaver(saksbehandler: Saksbehandler): ApiAntallOppgaver {
        val antallOppgaver = oppgaveDao.finnAntallOppgaver(saksbehandlerOid = saksbehandler.id.value)
        return antallOppgaver.tilApiversjon()
    }

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

    fun sendTilBeslutter(
        oppgaveId: Long,
        beslutter: SaksbehandlerWrapper?,
    ) {
        oppgaveService.oppgave(oppgaveId) {
            sendTilBeslutter(beslutter)
        }
    }

    fun sendIRetur(
        oppgaveId: Long,
        opprinneligSaksbehandlerWrapper: SaksbehandlerWrapper,
    ) {
        oppgaveService.oppgave(oppgaveId) {
            sendIRetur(opprinneligSaksbehandlerWrapper)
        }
    }

    fun spleisBehandlingId(oppgaveId: Long): UUID = oppgaveDao.finnSpleisBehandlingId(oppgaveId)

    fun fødselsnummer(oppgaveId: Long): String = oppgaveDao.finnFødselsnummer(oppgaveId)
}
