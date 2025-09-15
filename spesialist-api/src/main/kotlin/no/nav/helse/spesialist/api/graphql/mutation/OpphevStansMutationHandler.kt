package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.modell.saksbehandler.handlinger.HandlingType
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.domain.NotatType

class OpphevStansMutationHandler(
    private val saksbehandlerMediator: SaksbehandlerMediator,
) : OpphevStansMutationSchema {
    override fun opphevStans(
        env: DataFetchingEnvironment,
        fodselsnummer: String,
        begrunnelse: String,
    ): DataFetcherResult<Boolean> {
        saksbehandlerMediator.utførHandling(HandlingType.OPPHEV_STANS, env) { saksbehandler, _, tx, _ ->
            tx.stansAutomatiskBehandlingDao.lagreFraSpeil(fødselsnummer = fodselsnummer)
            tx.notatDao.lagreForOppgaveId(
                oppgaveId =
                    tx.oppgaveDao.finnOppgaveId(fødselsnummer = fodselsnummer)
                        ?: tx.oppgaveDao.finnOppgaveIdUansettStatus(fødselsnummer = fodselsnummer),
                tekst = begrunnelse,
                saksbehandlerOid = saksbehandler.id().value,
                notatType = NotatType.OpphevStans,
                dialogRef = tx.dialogDao.lagre(),
            )
        }
        return byggRespons(true)
    }
}
