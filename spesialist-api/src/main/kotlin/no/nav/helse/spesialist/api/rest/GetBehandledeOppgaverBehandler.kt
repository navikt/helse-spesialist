package no.nav.helse.spesialist.api.rest

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.oppgave.OppgaveRepository.BehandletOppgaveProjeksjon
import no.nav.helse.mediator.oppgave.OppgaveRepository.Side
import no.nav.helse.spesialist.api.rest.resources.BehandledeOppgaver
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer

class GetBehandledeOppgaverBehandler : GetBehandler<BehandledeOppgaver, ApiBehandletOppgaveProjeksjonSide, ApiGetBehandletOppgaverErrorCode> {
    override val tag = Tags.BEHANDLEDE_OPPGAVER

    override fun behandle(
        resource: BehandledeOppgaver,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiBehandletOppgaveProjeksjonSide, ApiGetBehandletOppgaverErrorCode> {
        val oppgaver =
            kallKontekst.transaksjon.oppgaveRepository
                .finnBehandledeOppgaveProjeksjoner(
                    behandletAvOid = kallKontekst.saksbehandler.id.value,
                    fom = resource.fom,
                    tom = resource.tom,
                    sidetall = resource.sidetall,
                    sidestørrelse = resource.sidestoerrelse,
                ).tilApiType(kallKontekst.transaksjon)

        loggInfo("Hentet ${oppgaver.elementer.size} oppgaver (av totalt ${oppgaver.totaltAntall})")

        return RestResponse.OK(oppgaver)
    }

    private fun Side<BehandletOppgaveProjeksjon>.tilApiType(transaksjon: SessionContext) =
        ApiBehandletOppgaveProjeksjonSide(
            totaltAntall = totaltAntall,
            sidetall = sidetall,
            sidestoerrelse = sidestørrelse,
            elementer =
                elementer.map { oppgave: BehandletOppgaveProjeksjon ->
                    oppgave.tilApiType(transaksjon)
                },
        )

    private fun BehandletOppgaveProjeksjon.tilApiType(
        transaksjon: SessionContext,
    ): ApiBehandletOppgaveProjeksjon {
        val personPseudoId =
            transaksjon.personPseudoIdDao.nyPersonPseudoId(
                identitetsnummer =
                    Identitetsnummer.fraString(
                        fødselsnummer,
                    ),
            )
        return ApiBehandletOppgaveProjeksjon(
            id = id,
            personnavn =
                ApiPersonnavn(
                    fornavn = personnavn.fornavn,
                    etternavn = personnavn.etternavn,
                    mellomnavn = personnavn.mellomnavn,
                ),
            beslutter = beslutter,
            personPseudoId = personPseudoId.value,
            ferdigstiltTidspunkt = ferdigstiltTidspunkt,
            saksbehandler = saksbehandler,
        )
    }
}

enum class ApiGetBehandletOppgaverErrorCode : ApiErrorCode
