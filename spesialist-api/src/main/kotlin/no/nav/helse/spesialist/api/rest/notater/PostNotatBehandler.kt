package no.nav.helse.spesialist.api.rest.notater

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiNotatRequest
import no.nav.helse.spesialist.api.rest.ApiNotatResponse
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Notater
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class PostNotatBehandler : PostBehandler<Notater, ApiNotatRequest, ApiNotatResponse, ApiPostNotatErrorCode> {
    override val påkrevdTilgang = Tilgang.Skriv

    override fun behandle(
        resource: Notater,
        request: ApiNotatRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiNotatResponse, ApiPostNotatErrorCode> =
        kallKontekst.medVedtaksperiode(
            vedtaksperiodeId = VedtaksperiodeId(request.vedtaksperiodeId),
            vedtaksperiodeIkkeFunnet = { ApiPostNotatErrorCode.VEDTAKSPERIODE_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiPostNotatErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { vedtaksperiode, _ ->
            behandleForVedtaksperiode(request, vedtaksperiode, kallKontekst)
        }

    private fun behandleForVedtaksperiode(
        request: ApiNotatRequest,
        vedtaksperiode: Vedtaksperiode,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiNotatResponse, ApiPostNotatErrorCode> {
        val dialog = Dialog.Factory.ny()
        kallKontekst.transaksjon.dialogRepository.lagre(dialog)

        val notat =
            Notat.Factory.ny(
                type = NotatType.Generelt,
                tekst = request.tekst,
                dialogRef = dialog.id(),
                vedtaksperiodeId = vedtaksperiode.id.value,
                saksbehandlerOid = kallKontekst.saksbehandler.id,
            )
        kallKontekst.transaksjon.notatRepository.lagre(notat)

        val notatResponse = ApiNotatResponse(id = notat.id().value)

        loggInfo("Opprettet notat", "${notat.id()}")

        return RestResponse.Created(notatResponse)
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Notater")
        }
    }
}

enum class ApiPostNotatErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    VEDTAKSPERIODE_IKKE_FUNNET("Fant ikke vedtaksperiode", HttpStatusCode.NotFound),
}
