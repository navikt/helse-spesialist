package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.application.logg.MdcKey
import no.nav.helse.spesialist.application.logg.medMdc
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.Saksbehandler

class StansAutomatiskBehandlingMutationHandler(
    private val sessionFactory: SessionFactory,
) : StansAutomatiskBehandlingMutationSchema {
    override fun stansAutomatiskBehandling(
        env: DataFetchingEnvironment,
        fodselsnummer: String,
        begrunnelse: String,
    ): DataFetcherResult<Boolean> =
        medMdc(MdcKey.IDENTITETSNUMMER to fodselsnummer) {
            val saksbehandler = env.graphQlContext.get<Saksbehandler>(ContextValues.SAKSBEHANDLER)
            sessionFactory.transactionalSessionScope { session ->
                session.lagrePeriodehistorikkForStans(fodselsnummer, saksbehandler, begrunnelse)
                session.stansAutomatiskBehandlingSaksbehandlerDao.lagreStans(fodselsnummer)
            }
            byggRespons(true)
        }

    override fun opphevStansAutomatiskBehandling(
        env: DataFetchingEnvironment,
        fodselsnummer: String,
        begrunnelse: String,
    ): DataFetcherResult<Boolean> =
        medMdc(MdcKey.IDENTITETSNUMMER to fodselsnummer) {
            val saksbehandler = env.graphQlContext.get<Saksbehandler>(ContextValues.SAKSBEHANDLER)
            sessionFactory.transactionalSessionScope { session ->
                session.lagrePeriodehistorikkForOpphevStans(fodselsnummer, saksbehandler, begrunnelse)
                session.stansAutomatiskBehandlingSaksbehandlerDao.opphevStans(fodselsnummer)
            }
            byggRespons(true)
        }

    private fun SessionContext.lagrePeriodehistorikkForStans(
        fødselsnummer: String,
        saksbehandler: Saksbehandler,
        begrunnelse: String,
    ) {
        val oppgaveId = this.oppgaveDao.oppgaveId(fødselsnummer)
        val dialog = Dialog.Factory.ny()
        this.dialogRepository.lagre(dialog)

        val innslag =
            Historikkinnslag.automatiskBehandlingStansetAvSaksbehandler(
                saksbehandler = saksbehandler,
                dialogId = dialog.id(),
                begrunnelse = begrunnelse,
            )
        this.periodehistorikkDao.lagreMedOppgaveId(innslag, oppgaveId)
    }

    private fun SessionContext.lagrePeriodehistorikkForOpphevStans(
        fødselsnummer: String,
        saksbehandler: Saksbehandler,
        begrunnelse: String,
    ) {
        val oppgaveId = this.oppgaveDao.oppgaveId(fødselsnummer)
        val dialog = Dialog.Factory.ny()
        this.dialogRepository.lagre(dialog)

        val innslag =
            Historikkinnslag.opphevStansAvSaksbehandler(
                saksbehandler = saksbehandler,
                dialogId = dialog.id(),
                begrunnelse = begrunnelse,
            )
        this.periodehistorikkDao.lagreMedOppgaveId(innslag, oppgaveId)
    }

    private fun OppgaveDao.oppgaveId(fødselsnummer: String) = this.finnOppgaveId(fødselsnummer) ?: this.finnOppgaveIdUansettStatus(fødselsnummer)
}
