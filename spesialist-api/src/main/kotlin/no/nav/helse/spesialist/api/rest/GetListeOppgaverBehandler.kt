package no.nav.helse.spesialist.api.rest

import no.nav.helse.spesialist.api.rest.resources.ListeOppgaver
import no.nav.helse.spesialist.application.logg.loggInfo

class GetListeOppgaverBehandler : GetBehandler<ListeOppgaver, ApiOppgaveProjeksjonSide, ApiGetOppgaverErrorCode> {
    override val tag = Tags.OPPGAVER

    override fun behandle(
        resource: ListeOppgaver,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiOppgaveProjeksjonSide, ApiGetOppgaverErrorCode> {
        val oppgaver =
            kallKontekst.transaksjon.oppgaveRepository
                .finnListeOppgaveProjeksjoner(
                    sidetall = resource.sidetall?.takeUnless { it < 1 } ?: 1,
                    sidestørrelse = resource.sidestoerrelse?.takeUnless { it < 1 } ?: 10,
                ).tilApiType(kallKontekst.transaksjon)

        loggInfo("Hentet ${oppgaver.elementer.size} liste-oppgaver (av totalt ${oppgaver.totaltAntall})")

        return RestResponse.OK(oppgaver)
    }
}
