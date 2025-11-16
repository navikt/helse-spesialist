package no.nav.helse.mediator.oppgave

import no.nav.helse.db.OppgaveDao
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilApiversjon
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilBehandledeOppgaver
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilEgenskaperForVisning
import no.nav.helse.spesialist.api.graphql.schema.ApiAntallOppgaver
import no.nav.helse.spesialist.api.graphql.schema.ApiBehandledeOppgaver
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveegenskap
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper
import java.time.LocalDate
import java.util.UUID

class ApiOppgaveService(
    private val oppgaveDao: OppgaveDao,
    private val oppgaveService: OppgaveService,
) {
    fun antallOppgaver(saksbehandler: Saksbehandler): ApiAntallOppgaver {
        val antallOppgaver = oppgaveDao.finnAntallOppgaver(saksbehandlerOid = saksbehandler.id.value)
        return antallOppgaver.tilApiversjon()
    }

    fun behandledeOppgaver(
        saksbehandler: Saksbehandler,
        offset: Int,
        limit: Int,
        fom: LocalDate,
        tom: LocalDate,
    ): ApiBehandledeOppgaver {
        val behandledeOppgaver =
            oppgaveDao.finnBehandledeOppgaver(
                behandletAvOid = saksbehandler.id.value,
                offset = offset,
                limit = limit,
                fom = fom,
                tom = tom,
            )
        return ApiBehandledeOppgaver(
            oppgaver = behandledeOppgaver.tilBehandledeOppgaver(),
            totaltAntallOppgaver = if (behandledeOppgaver.isEmpty()) 0 else behandledeOppgaver.first().filtrertAntall,
        )
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

    fun venterPåSaksbehandler(oppgaveId: Long): Boolean = oppgaveDao.venterPåSaksbehandler(oppgaveId)

    fun spleisBehandlingId(oppgaveId: Long): UUID = oppgaveDao.finnSpleisBehandlingId(oppgaveId)

    fun fødselsnummer(oppgaveId: Long): String = oppgaveDao.finnFødselsnummer(oppgaveId)
}
