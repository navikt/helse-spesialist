package no.nav.helse.spesialist.api.rest.vedtaksperioder

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.melding.AnmodningOmForkastingEvent
import no.nav.helse.spesialist.api.rest.ApiAnmodOmForkastingRequest
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Vedtaksperioder
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.VedtaksperiodeId

class PostAnmodOmForkastingBehandler : PostBehandler<Vedtaksperioder.VedtaksperiodeId.AnmodOmForkasting, ApiAnmodOmForkastingRequest, Unit, ApiPostAnmodOmForkastingErrorCode> {
    override val tag = Tags.VEDTAKSPERIODER

    override fun behandle(
        resource: Vedtaksperioder.VedtaksperiodeId.AnmodOmForkasting,
        request: ApiAnmodOmForkastingRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPostAnmodOmForkastingErrorCode> =
        kallKontekst.medVedtaksperiode(
            vedtaksperiodeId = VedtaksperiodeId(resource.parent.vedtaksperiodeId),
            vedtaksperiodeIkkeFunnet = { ApiPostAnmodOmForkastingErrorCode.VEDTAKSPERIODE_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiPostAnmodOmForkastingErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { vedtaksperiode, _ ->
            behandleForVedtaksperiode(vedtaksperiode, kallKontekst)
        }

    private fun behandleForVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPostAnmodOmForkastingErrorCode> {
        val behandling =
            kallKontekst.transaksjon.behandlingRepository.finnNyesteForVedtaksperiode(vedtaksperiode.id)
                ?: return RestResponse.Error(ApiPostAnmodOmForkastingErrorCode.BEHANDLING_IKKE_FUNNET)

        kallKontekst.outbox.leggTil(
            identitetsnummer = vedtaksperiode.identitetsnummer,
            hendelse =
                AnmodningOmForkastingEvent(
                    fødselsnummer = vedtaksperiode.identitetsnummer.value,
                    vedtaksperiodeId = vedtaksperiode.id.value.toString(),
                    organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                    yrkesaktivitetstype = behandling.yrkesaktivitetstype.toString(),
                ),
            årsak = "anmodning om forkasting av vedtaksperiode",
        )

        loggInfo("La anmodning om forkasting i outbox")

        return RestResponse.NoContent()
    }
}

enum class ApiPostAnmodOmForkastingErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    VEDTAKSPERIODE_IKKE_FUNNET("Fant ikke vedtaksperiode", HttpStatusCode.NotFound),
    BEHANDLING_IKKE_FUNNET("Fant ikke tilhørende behandling", HttpStatusCode.NotFound),
}
