package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.modell.saksbehandler.SaksbehandlerDto
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.domain.Dialog

class StansAutomatiskBehandlingMutationHandler(private val sessionFactory: SessionFactory) :
    StansAutomatiskBehandlingMutationSchema {
    override fun stansAutomatiskBehandling(
        env: DataFetchingEnvironment,
        fodselsnummer: String,
        begrunnelse: String,
    ): DataFetcherResult<Boolean> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(ContextValues.SAKSBEHANDLER)
        sessionFactory.transactionalSessionScope { session ->
            session.lagrePeriodehistorikkForStans(fodselsnummer, saksbehandler, begrunnelse)
            session.stansAutomatiskBehandlingSaksbehandlerDao.lagreStans(fodselsnummer)
        }
        return byggRespons(true)
    }

    override fun opphevStansAutomatiskBehandling(
        env: DataFetchingEnvironment,
        fodselsnummer: String,
        begrunnelse: String,
    ): DataFetcherResult<Boolean> {
        val saksbehandler = env.graphQlContext.get<SaksbehandlerFraApi>(ContextValues.SAKSBEHANDLER)
        sessionFactory.transactionalSessionScope { session ->
            session.lagrePeriodehistorikkForOpphevStans(fodselsnummer, saksbehandler, begrunnelse)
            session.stansAutomatiskBehandlingSaksbehandlerDao.opphevStans(fodselsnummer)
        }
        return byggRespons(true)
    }

    private fun SessionContext.lagrePeriodehistorikkForStans(
        fødselsnummer: String,
        saksbehandler: SaksbehandlerFraApi,
        begrunnelse: String,
    ) {
        val oppgaveId = this.oppgaveDao.oppgaveId(fødselsnummer)
        val dialog = Dialog.Factory.ny()
        this.dialogRepository.lagre(dialog)

        val innslag =
            Historikkinnslag.automatiskBehandlingStansetAvSaksbehandler(
                saksbehandler = saksbehandler.toDto(),
                dialogId = dialog.id(),
                begrunnelse = begrunnelse,
            )
        this.periodehistorikkDao.lagreMedOppgaveId(innslag, oppgaveId)
    }

    private fun SessionContext.lagrePeriodehistorikkForOpphevStans(
        fødselsnummer: String,
        saksbehandler: SaksbehandlerFraApi,
        begrunnelse: String,
    ) {
        val oppgaveId = this.oppgaveDao.oppgaveId(fødselsnummer)
        val dialog = Dialog.Factory.ny()
        this.dialogRepository.lagre(dialog)

        val innslag =
            Historikkinnslag.opphevStansAvSaksbehandler(
                saksbehandler = saksbehandler.toDto(),
                dialogId = dialog.id(),
                begrunnelse = begrunnelse,
            )
        this.periodehistorikkDao.lagreMedOppgaveId(innslag, oppgaveId)
    }

    private fun OppgaveDao.oppgaveId(fødselsnummer: String) =
        this.finnOppgaveId(fødselsnummer) ?: this.finnOppgaveIdUansettStatus(fødselsnummer)

    private fun SaksbehandlerFraApi.toDto(): SaksbehandlerDto =
        SaksbehandlerDto(
            epostadresse = this.epost,
            oid = this.oid,
            navn = this.navn,
            ident = this.ident,
        )
}
