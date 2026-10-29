package no.nav.helse.spesialist.api.rest.vedtaksperioder

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiRisikovurderingForVedtaksperiode
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Vedtaksperioder
import no.nav.helse.spesialist.domain.VedtaksperiodeId

class GetRisikovurderingForVedtaksperiodeBehandler : GetBehandler<Vedtaksperioder.VedtaksperiodeId.Risikovurdering, ApiRisikovurderingForVedtaksperiode, ApiGetRisikovurderingForVedtaksperiodeErrorCode> {
    override val tag = Tags.VEDTAKSPERIODER

    override fun behandle(
        resource: Vedtaksperioder.VedtaksperiodeId.Risikovurdering,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiRisikovurderingForVedtaksperiode, ApiGetRisikovurderingForVedtaksperiodeErrorCode> =
        kallKontekst.medVedtaksperiode(
            vedtaksperiodeId = VedtaksperiodeId(resource.parent.vedtaksperiodeId),
            vedtaksperiodeIkkeFunnet = { ApiGetRisikovurderingForVedtaksperiodeErrorCode.VEDTAKSPERIODE_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetRisikovurderingForVedtaksperiodeErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { vedtaksperiode, _ ->
            val risikovurdering =
                kallKontekst.transaksjon.risikovurderingDao
                    .hentRisikovurderingsdata(vedtaksperiode.id.value)
                    ?.tilApiRisikovurdering()
            RestResponse.OK(ApiRisikovurderingForVedtaksperiode(risikovurdering = risikovurdering))
        }
}

enum class ApiGetRisikovurderingForVedtaksperiodeErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    VEDTAKSPERIODE_IKKE_FUNNET("Vedtaksperiode ikke funnet", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
