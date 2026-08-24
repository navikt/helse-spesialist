package no.nav.helse.spesialist.api.rest.andreYtelser

import io.ktor.http.*
import no.nav.helse.modell.melding.GraderteAndreYtelserEndringerEvent
import no.nav.helse.spesialist.api.rest.*
import no.nav.helse.spesialist.api.rest.resources.GraderteAndreYtelserResource
import no.nav.helse.spesialist.api.rest.tilkomneinntekter.finnEllerOpprettTotrinnsvurdering
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.andreytelser.AndreYtelserPeriode.GraderteAndreYtelserPeriode
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelser
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserId
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType
import no.nav.helse.spesialist.domain.andreytelser.validerGraderteAndreYtelserPeriode

class PatchGraderteAndreYtelserBehandler : PatchBehandler<GraderteAndreYtelserResource.Id, ApiPatchGraderteAndreYtelserRequest, ApiPatchGraderteAndreYtelserResponse, ApiPatchGraderteAndreYtelserErrorCode> {
    override val tag = Tags.GRADERTE_ANDRE_YTELSER

    override fun behandle(
        resource: GraderteAndreYtelserResource.Id,
        request: ApiPatchGraderteAndreYtelserRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiPatchGraderteAndreYtelserResponse, ApiPatchGraderteAndreYtelserErrorCode> {
        val eksisterendeGraderteAndreYtelser =
            kallKontekst.transaksjon.graderteAndreYtelserRepository.finn(GraderteAndreYtelserId(resource.graderteAndreYtelserId))
                ?: return RestResponse.Error(ApiPatchGraderteAndreYtelserErrorCode.GRADERTE_ANDRE_YTELSER_IKKE_FUNNET)
        return kallKontekst.medPerson(
            identitetsnummer = eksisterendeGraderteAndreYtelser.identitetsnummer,
            personIkkeFunnet = { ApiPatchGraderteAndreYtelserErrorCode.PERSON_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiPatchGraderteAndreYtelserErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            behandleForPerson(request, eksisterendeGraderteAndreYtelser, person, kallKontekst)
        }
    }

    private fun behandleForPerson(
        request: ApiPatchGraderteAndreYtelserRequest,
        eksisterendeGraderteAndreYtelser: GraderteAndreYtelser,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiPatchGraderteAndreYtelserResponse, ApiPatchGraderteAndreYtelserErrorCode> {
        validerGraderteAndreYtelserPeriode(
            eksisterendeGraderteAndreYtelser =
                kallKontekst.transaksjon.graderteAndreYtelserRepository
                    .finnAlleForIdentitetsnummer(person.id)
                    .filterNot { it.id == eksisterendeGraderteAndreYtelser.id },
            nyGraderteAndreYtelserType = GraderteAndreYtelserType.valueOf(request.andreYtelseType.name),
            nyGraderteAndreYtelserPerioder =
                request.perioder.map {
                    GraderteAndreYtelserPeriode(
                        periode = Periode(it.fom, it.tom),
                        grad = it.grad,
                    )
                },
            vedtaksperioder =
                kallKontekst.transaksjon.legacyVedtaksperiodeRepository.finnVedtaksperioder(person.id.value),
        )

        eksisterendeGraderteAndreYtelser.endreTil(
            graderteAndreYtelserPerioder =
                request.perioder.map {
                    GraderteAndreYtelserPeriode(
                        periode = Periode(it.fom, it.tom),
                        grad = it.grad,
                    )
                },
            graderteAndreYtelserType = GraderteAndreYtelserType.valueOf(request.andreYtelseType.name),
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

        kallKontekst.outbox.leggTil(
            identitetsnummer = eksisterendeGraderteAndreYtelser.identitetsnummer,
            hendelse =
                GraderteAndreYtelserEndringerEvent(
                    fødselsnummer = person.id,
                    fom =
                        eksisterendeGraderteAndreYtelser.perioder
                            .minByOrNull { it.periode.fom }!!
                            .periode.fom,
                ),
            årsak = "endring av graderte andre ytelser",
        )

        loggInfo("Endret graderte andre ytelser", "graderteAndreYtelserId" to eksisterendeGraderteAndreYtelser.id)

        return RestResponse.OK(ApiPatchGraderteAndreYtelserResponse(eksisterendeGraderteAndreYtelser.id.value))
    }
}

enum class ApiPatchGraderteAndreYtelserErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    GRADERTE_ANDRE_YTELSER_IKKE_FUNNET("Graderte andre ytelser ikke funnet", HttpStatusCode.BadRequest),
    PERSON_IKKE_FUNNET("Person ikke funnet", HttpStatusCode.BadRequest),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
