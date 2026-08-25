package no.nav.helse.spesialist.api.rest.andreYtelser

import io.ktor.http.*
import no.nav.helse.spesialist.api.rest.*
import no.nav.helse.spesialist.api.rest.resources.GraderteAndreYtelserResource
import no.nav.helse.spesialist.api.rest.tilkomneinntekter.finnEllerOpprettTotrinnsvurdering
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserId

class PostGjenopprettGraderteAndreYtelserBehandler : PostBehandler<GraderteAndreYtelserResource.Id.Gjenopprett, ApiPostGjenopprettGraderteAndreYtelserRequest, ApiPostGjenopprettGraderteAndreYtelserResponse, ApiPostGjenopprettGraderteAndreYtelserErrorCode> {
    override val tag = Tags.GRADERTE_ANDRE_YTELSER

    override fun behandle(
        resource: GraderteAndreYtelserResource.Id.Gjenopprett,
        request: ApiPostGjenopprettGraderteAndreYtelserRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiPostGjenopprettGraderteAndreYtelserResponse, ApiPostGjenopprettGraderteAndreYtelserErrorCode> {
        val graderteAndreYtelser =
            kallKontekst.transaksjon.graderteAndreYtelserRepository.finn(GraderteAndreYtelserId(resource.parent.graderteAndreYtelserId))
                ?: return RestResponse.Error(ApiPostGjenopprettGraderteAndreYtelserErrorCode.GRADERTE_ANDRE_YTELSER_IKKE_FUNNET)
        if (!graderteAndreYtelser.fjernet) {
            return RestResponse.Error(ApiPostGjenopprettGraderteAndreYtelserErrorCode.GRADERTE_ANDRE_YTELSER_ER_IKKE_FJERNET)
        }

        return kallKontekst.medPerson(
            identitetsnummer = graderteAndreYtelser.identitetsnummer,
        ) { person ->
            graderteAndreYtelser.gjenopprett(
                graderteAndreYtelserPerioder = graderteAndreYtelser.perioder,
                graderteAndreYtelserType = graderteAndreYtelser.graderteAndreYtelserType,
                saksbehandlerIdent = kallKontekst.saksbehandler.ident,
                notatTilBeslutter = request.notatTilBeslutter,
                totrinnsvurderingId =
                    finnEllerOpprettTotrinnsvurdering(
                        identitetsnummer = person.id,
                        totrinnsvurderingRepository = kallKontekst.transaksjon.totrinnsvurderingRepository,
                    ).id(),
            )
            kallKontekst.transaksjon.graderteAndreYtelserRepository.lagre(graderteAndreYtelser)
            kallKontekst.leggTilGraderteAndreYtelserEndringshendelse(
                graderteAndreYtelser = graderteAndreYtelser,
                årsak = "gjenoppretting av graderte andre ytelser",
            )

            loggInfo("Gjenopprettet graderte andre ytelser", "graderteAndreYtelserId" to graderteAndreYtelser.id)

            RestResponse.OK(ApiPostGjenopprettGraderteAndreYtelserResponse(graderteAndreYtelser.id.value))
        }
    }
}

enum class ApiPostGjenopprettGraderteAndreYtelserErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    GRADERTE_ANDRE_YTELSER_IKKE_FUNNET("Graderte andre ytelser ikke funnet", HttpStatusCode.BadRequest),
    GRADERTE_ANDRE_YTELSER_ER_IKKE_FJERNET("Graderte andre ytelser er ikke fjernet", HttpStatusCode.Conflict),
}
