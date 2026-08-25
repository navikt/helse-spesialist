package no.nav.helse.spesialist.api.rest.vedtaksperioder.overstyring

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiOverstyrTidslinjeRequest
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Vedtaksperioder
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtTidslinje
import no.nav.helse.spesialist.domain.overstyringer.OverstyrtTidslinjedag

class PostOverstyrTidslinjeBehandler : PostBehandler<Vedtaksperioder.VedtaksperiodeId.Overstyringer.Tidslinje, ApiOverstyrTidslinjeRequest, Unit, ApiOverstyrTidslinjeErrorCode> {
    override val tag = Tags.OVERSTYRINGER

    override fun behandle(
        resource: Vedtaksperioder.VedtaksperiodeId.Overstyringer.Tidslinje,
        request: ApiOverstyrTidslinjeRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiOverstyrTidslinjeErrorCode> =
        kallKontekst.medVedtaksperiode(
            vedtaksperiodeId = VedtaksperiodeId(resource.parent.parent.vedtaksperiodeId),
            vedtaksperiodeIkkeFunnet = { ApiOverstyrTidslinjeErrorCode.VEDTAKSPERIODE_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiOverstyrTidslinjeErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { vedtaksperiode, person ->
            behandleForVedtaksperiode(request, vedtaksperiode, person, kallKontekst)
        }

    private fun behandleForVedtaksperiode(
        request: ApiOverstyrTidslinjeRequest,
        vedtaksperiode: Vedtaksperiode,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiOverstyrTidslinjeErrorCode> {
        val overstyring =
            OverstyrtTidslinje.ny(
                saksbehandlerOid = kallKontekst.saksbehandler.id,
                fødselsnummer = person.id.value,
                aktørId = person.aktørId,
                vedtaksperiodeId = vedtaksperiode.id.value,
                organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                dager =
                    request.dager.map {
                        OverstyrtTidslinjedag(
                            dato = it.dato,
                            type = it.type,
                            fraType = it.fraType,
                            grad = it.grad,
                            fraGrad = it.fraGrad,
                            lovhjemmel =
                                it.lovhjemmel?.let { lovhjemmel ->
                                    Lovhjemmel(
                                        paragraf = lovhjemmel.paragraf,
                                        ledd = lovhjemmel.ledd,
                                        bokstav = lovhjemmel.bokstav,
                                        lovverk = lovhjemmel.lovverk,
                                        lovverksversjon = lovhjemmel.lovverksversjon,
                                    )
                                },
                        )
                    },
                begrunnelse = request.begrunnelse,
            )

        teamLogs.info("Reserverer person ${person.id.value} til saksbehandler ${kallKontekst.saksbehandler}")
        kallKontekst.transaksjon.reservasjonDao.reserverPerson(kallKontekst.saksbehandler.id.value, person.id.value)

        val totrinnsvurdering =
            kallKontekst.transaksjon.totrinnsvurderingRepository.finnAktivForPerson(person.id.value)
                ?: Totrinnsvurdering.ny(person.id.value)
        totrinnsvurdering.nyOverstyring(overstyring = overstyring)
        kallKontekst.transaksjon.totrinnsvurderingRepository.lagre(totrinnsvurdering)

        overstyring.byggSubsumsjoner(kallKontekst.saksbehandler.epost).forEach { subsumsjon ->
            kallKontekst.outbox.leggTil(person.id, subsumsjon.byggEvent())
        }

        kallKontekst.outbox.leggTil(person.id, overstyring.byggEvent(), "overstyring av tidslinje")

        loggInfo("Overstyrte tidslinje for vedtaksperiode")

        return RestResponse.NoContent()
    }
}

enum class ApiOverstyrTidslinjeErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    VEDTAKSPERIODE_IKKE_FUNNET("Fant ikke vedtaksperiode", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
