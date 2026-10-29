package no.nav.helse.spesialist.api.rest.vedtaksperioder

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiAnnulleringForVedtaksperiode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Vedtaksperioder
import no.nav.helse.spesialist.domain.VedtaksperiodeId

class GetAnnulleringForVedtaksperiodeBehandler : GetBehandler<Vedtaksperioder.VedtaksperiodeId.Annullering, ApiAnnulleringForVedtaksperiode, ApiGetAnnulleringForVedtaksperiodeErrorCode> {
    override val tag = Tags.VEDTAKSPERIODER

    override fun behandle(
        resource: Vedtaksperioder.VedtaksperiodeId.Annullering,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiAnnulleringForVedtaksperiode, ApiGetAnnulleringForVedtaksperiodeErrorCode> =
        kallKontekst.medVedtaksperiode(
            vedtaksperiodeId = VedtaksperiodeId(resource.parent.vedtaksperiodeId),
            vedtaksperiodeIkkeFunnet = { ApiGetAnnulleringForVedtaksperiodeErrorCode.VEDTAKSPERIODE_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetAnnulleringForVedtaksperiodeErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { vedtaksperiode, _ ->
            val annullering =
                kallKontekst.transaksjon.annulleringRepository
                    .finnOrNull(vedtaksperiode.id.value)
                    ?.let {
                        val saksbehandler =
                            kallKontekst.transaksjon.saksbehandlerRepository.finnOrNull(it.saksbehandlerOid)
                                ?: error("Fant ikke saksbehandleren som annullerte")
                        it.tilApiAnnullering(saksbehandler)
                    }
            RestResponse.OK(ApiAnnulleringForVedtaksperiode(annullering = annullering))
        }
}

enum class ApiGetAnnulleringForVedtaksperiodeErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    VEDTAKSPERIODE_IKKE_FUNNET("Vedtaksperiode ikke funnet", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
