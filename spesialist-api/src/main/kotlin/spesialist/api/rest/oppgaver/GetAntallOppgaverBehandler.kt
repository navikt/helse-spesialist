package no.nav.helse.spesialist.api.rest.oppgaver

import no.nav.helse.spesialist.api.rest.ApiAntallOppgaver
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.AntallOppgaver

class GetAntallOppgaverBehandler : GetBehandler<AntallOppgaver, ApiAntallOppgaver, ApiGetAntallOppgaverErrorCode> {
    override val tag = Tags.OPPGAVER

    override fun behandle(
        resource: AntallOppgaver,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiAntallOppgaver, ApiGetAntallOppgaverErrorCode> {
        val antallOppgaver =
            kallKontekst.transaksjon.oppgaveRepository.finnAntallOppgaverProjeksjon(
                saksbehandlersOid = kallKontekst.saksbehandler.id,
            )

        return RestResponse.OK(
            ApiAntallOppgaver(
                antallMineSaker = antallOppgaver.antallMineSaker,
                antallMineSakerPåVent = antallOppgaver.antallMineSakerPåVent,
            ),
        )
    }
}

enum class ApiGetAntallOppgaverErrorCode : ApiErrorCode
