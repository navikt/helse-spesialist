package no.nav.helse.spesialist.api.rest.andreYtelser

import io.ktor.http.*
import no.nav.helse.spesialist.api.rest.*
import no.nav.helse.spesialist.api.rest.resources.GraderteAndreYtelserResource
import no.nav.helse.spesialist.api.rest.tilkomneinntekter.finnEllerOpprettTotrinnsvurdering
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelser
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserId

class PatchEndreGraderteAndreYtelserBehandler : PatchBehandler<GraderteAndreYtelserResource.Id, ApiPatchEndreGraderteAndreYtelserRequest, ApiPatchEndreGraderteAndreYtelserResponse, ApiPatchEndreGraderteAndreYtelserErrorCode> {
    override val tag = Tags.GRADERTE_ANDRE_YTELSER

    override fun behandle(
        resource: GraderteAndreYtelserResource.Id,
        request: ApiPatchEndreGraderteAndreYtelserRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiPatchEndreGraderteAndreYtelserResponse, ApiPatchEndreGraderteAndreYtelserErrorCode> {
        val eksisterendeGraderteAndreYtelser =
            kallKontekst.transaksjon.graderteAndreYtelserRepository.finn(GraderteAndreYtelserId(resource.graderteAndreYtelserId))
                ?: return RestResponse.Error(ApiPatchEndreGraderteAndreYtelserErrorCode.GRADERTE_ANDRE_YTELSER_IKKE_FUNNET)
        if (eksisterendeGraderteAndreYtelser.fjernet) {
            return RestResponse.Error(ApiPatchEndreGraderteAndreYtelserErrorCode.GRADERTE_ANDRE_YTELSER_ER_FJERNET)
        }
        return kallKontekst.medPerson(
            identitetsnummer = eksisterendeGraderteAndreYtelser.identitetsnummer,
            personIkkeFunnet = { ApiPatchEndreGraderteAndreYtelserErrorCode.PERSON_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiPatchEndreGraderteAndreYtelserErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            behandleForPerson(request, eksisterendeGraderteAndreYtelser, person, kallKontekst)
        }
    }

    private fun behandleForPerson(
        request: ApiPatchEndreGraderteAndreYtelserRequest,
        eksisterendeGraderteAndreYtelser: GraderteAndreYtelser,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiPatchEndreGraderteAndreYtelserResponse, ApiPatchEndreGraderteAndreYtelserErrorCode> {
        val oppdatertePerioder = request.perioder.tilGraderteAndreYtelserPerioder()
        val oppdatertType = request.andreYtelserType.tilDomeneType()

        kallKontekst.validerGraderteAndreYtelserEndring(
            person = person,
            graderteAndreYtelserId = eksisterendeGraderteAndreYtelser.id,
            perioder = oppdatertePerioder,
            type = oppdatertType,
        )

        eksisterendeGraderteAndreYtelser.endreTil(
            graderteAndreYtelserPerioder = oppdatertePerioder,
            graderteAndreYtelserType = oppdatertType,
            saksbehandlerIdent = kallKontekst.saksbehandler.ident,
            notatTilBeslutter = request.notatTilBeslutter,
            totrinnsvurderingId =
                finnEllerOpprettTotrinnsvurdering(
                    identitetsnummer = person.id,
                    totrinnsvurderingRepository = kallKontekst.transaksjon.totrinnsvurderingRepository,
                ).id(),
        )

        // Lagre pathedGraderteAndreYtelser i repository
        kallKontekst.transaksjon.graderteAndreYtelserRepository.lagre(eksisterendeGraderteAndreYtelser)

        kallKontekst.leggTilGraderteAndreYtelserEndringshendelse(
            graderteAndreYtelser = eksisterendeGraderteAndreYtelser,
            årsak = "endring av graderte andre ytelser",
        )

        loggInfo("Endret graderte andre ytelser", "graderteAndreYtelserId" to eksisterendeGraderteAndreYtelser.id)

        return RestResponse.OK(ApiPatchEndreGraderteAndreYtelserResponse(eksisterendeGraderteAndreYtelser.id.value))
    }
}

enum class ApiPatchEndreGraderteAndreYtelserErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    GRADERTE_ANDRE_YTELSER_IKKE_FUNNET("Graderte andre ytelser ikke funnet", HttpStatusCode.BadRequest),
    GRADERTE_ANDRE_YTELSER_ER_FJERNET("Graderte andre ytelser er fjernet og kan ikke endres", HttpStatusCode.Conflict),
    PERSON_IKKE_FUNNET("Person ikke funnet", HttpStatusCode.BadRequest),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
