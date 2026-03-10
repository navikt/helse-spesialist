package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import no.nav.helse.spesialist.api.rest.resources.ListeOppgaver
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class GetListeOppgaverBehandler : GetBehandler<ListeOppgaver, ApiOppgaveProjeksjonSide, ApiGetOppgaverErrorCode> {
    override val påkrevdTilgang = Tilgang.Les

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

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("ListeOppgaver")
        }
    }
}
