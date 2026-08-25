package no.nav.helse.spesialist.api.rest.andreYtelser

import no.nav.helse.spesialist.api.rest.*
import no.nav.helse.spesialist.api.rest.resources.GraderteAndreYtelserResource
import no.nav.helse.spesialist.api.rest.tilkomneinntekter.finnEllerOpprettTotrinnsvurdering
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.andreytelser.AndreYtelserPeriode.GraderteAndreYtelserPeriode
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelser
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType
import no.nav.helse.spesialist.domain.andreytelser.validerGraderteAndreYtelserPeriode

class PostGraderteAndreYtelserBehandler : PostBehandler<GraderteAndreYtelserResource, ApiLeggTilGraderteAndreYtelserRequest, ApiLeggTilGraderteAndreYtelserResponse, PersonErrorCode> {
    override val tag = Tags.GRADERTE_ANDRE_YTELSER

    override fun behandle(
        resource: GraderteAndreYtelserResource,
        request: ApiLeggTilGraderteAndreYtelserRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiLeggTilGraderteAndreYtelserResponse, PersonErrorCode> =
        kallKontekst.medPerson(
            identitetsnummer = Identitetsnummer.fraString(identitetsnummer = request.fodselsnummer),
        ) { person ->
            behandleForPerson(request, person, kallKontekst)
        }

    private fun behandleForPerson(
        request: ApiLeggTilGraderteAndreYtelserRequest,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiLeggTilGraderteAndreYtelserResponse, PersonErrorCode> {
        validerGraderteAndreYtelserPeriode(
            eksisterendeGraderteAndreYtelser =
                kallKontekst.transaksjon.graderteAndreYtelserRepository.finnAlleForIdentitetsnummer(person.id),
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

        val graderteAndreYtelser =
            GraderteAndreYtelser.ny(
                identitetsnummer = person.id,
                saksbehandlerIdent = kallKontekst.saksbehandler.ident,
                notatTilBeslutter = request.notatTilBeslutter,
                totrinnsvurderingId =
                    finnEllerOpprettTotrinnsvurdering(
                        identitetsnummer = person.id,
                        totrinnsvurderingRepository = kallKontekst.transaksjon.totrinnsvurderingRepository,
                    ).id(),
                graderteAndreYtelserPerioder =
                    request.perioder.map {
                        GraderteAndreYtelserPeriode(
                            periode = Periode(it.fom, it.tom),
                            grad = it.grad,
                        )
                    },
                graderteAndreYtelserType = GraderteAndreYtelserType.valueOf(request.andreYtelseType.name),
            )
        kallKontekst.transaksjon.graderteAndreYtelserRepository.lagre(graderteAndreYtelser)

        kallKontekst.leggTilGraderteAndreYtelserEndringshendelse(
            graderteAndreYtelser = graderteAndreYtelser,
            årsak = "graderte andre ytelser lagt til",
        )

        loggInfo("La til graderte andre ytelser (graderteAndreYtelserId: ${graderteAndreYtelser.id}")

        return RestResponse.OK(ApiLeggTilGraderteAndreYtelserResponse(graderteAndreYtelser.id.value))
    }
}
