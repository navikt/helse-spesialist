package no.nav.helse.spesialist.api.rest.andreYtelser

import io.ktor.http.*
import no.nav.helse.spesialist.api.rest.*
import no.nav.helse.spesialist.api.rest.resources.GraderteAndreYtelserResource
import no.nav.helse.spesialist.api.rest.tilkomneinntekter.finnEllerOpprettTotrinnsvurdering
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserId

class PostFjernGraderteAndreYtelserBehandler : PostBehandler<GraderteAndreYtelserResource.Id.Fjern, ApiPostFjernGraderteAndreYtelserRequest, ApiPostFjernGraderteAndreYtelserResponse, ApiPostFjernGraderteAndreYtelserErrorCode> {
    override val tag = Tags.GRADERTE_ANDRE_YTELSER

    override fun behandle(
        resource: GraderteAndreYtelserResource.Id.Fjern,
        request: ApiPostFjernGraderteAndreYtelserRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiPostFjernGraderteAndreYtelserResponse, ApiPostFjernGraderteAndreYtelserErrorCode> {
        val graderteAndreYtelser =
            kallKontekst.transaksjon.graderteAndreYtelserRepository.finn(GraderteAndreYtelserId(resource.parent.graderteAndreYtelserId))
                ?: return RestResponse.Error(ApiPostFjernGraderteAndreYtelserErrorCode.GRADERTE_ANDRE_YTELSER_IKKE_FUNNET)
        if (graderteAndreYtelser.fjernet) {
            return RestResponse.Error(ApiPostFjernGraderteAndreYtelserErrorCode.GRADERTE_ANDRE_YTELSER_ALLEREDE_FJERNET)
        }

        return kallKontekst.medPerson(
            identitetsnummer = graderteAndreYtelser.identitetsnummer,
            personIkkeFunnet = { ApiPostFjernGraderteAndreYtelserErrorCode.PERSON_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiPostFjernGraderteAndreYtelserErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            graderteAndreYtelser.fjern(
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
                årsak = "fjerning av graderte andre ytelser",
            )

            loggInfo("Fjernet graderte andre ytelser", "graderteAndreYtelserId" to graderteAndreYtelser.id)

            RestResponse.OK(ApiPostFjernGraderteAndreYtelserResponse(graderteAndreYtelser.id.value))
        }
    }
}

enum class ApiPostFjernGraderteAndreYtelserErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    GRADERTE_ANDRE_YTELSER_IKKE_FUNNET("Graderte andre ytelser ikke funnet", HttpStatusCode.BadRequest),
    GRADERTE_ANDRE_YTELSER_ALLEREDE_FJERNET("Graderte andre ytelser er allerede fjernet", HttpStatusCode.Conflict),
    PERSON_IKKE_FUNNET("Person ikke funnet", HttpStatusCode.BadRequest),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
