package no.nav.helse.spesialist.api.rest.notater

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiPatchNotatRequest
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PatchBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Notater
import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatId
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class PatchNotatBehandler : PatchBehandler<Notater.NotatId, ApiPatchNotatRequest, Unit, ApiPatchNotatErrorCode> {
    override val påkrevdTilgang = Tilgang.Skriv

    override fun behandle(
        resource: Notater.NotatId,
        request: ApiPatchNotatRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPatchNotatErrorCode> {
        if (!request.feilregistrert) return RestResponse.Error(ApiPatchNotatErrorCode.KAN_IKKE_FJERNE_FEILREGISTRERING)

        val notat =
            kallKontekst.transaksjon.notatRepository.finn(NotatId(resource.notatId))
                ?: return RestResponse.Error(ApiPatchNotatErrorCode.NOTAT_IKKE_FUNNET)

        return kallKontekst.medVedtaksperiode(
            vedtaksperiodeId = VedtaksperiodeId(notat.vedtaksperiodeId),
            vedtaksperiodeIkkeFunnet = { error("Vedtaksperioden ble ikke funnet") },
            manglerTilgangTilPerson = { ApiPatchNotatErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { _, _ ->
            behandleForVedtaksperiode(notat, kallKontekst)
        }
    }

    private fun behandleForVedtaksperiode(
        notat: Notat,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPatchNotatErrorCode> {
        if (!notat.feilregistrert) {
            notat.feilregistrer()
            kallKontekst.transaksjon.notatRepository.lagre(notat)
        }

        return RestResponse.OK(Unit)
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Notater")
        }
    }
}

enum class ApiPatchNotatErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    NOTAT_IKKE_FUNNET("Fant ikke notat", HttpStatusCode.NotFound),
    KAN_IKKE_FJERNE_FEILREGISTRERING("Kan ikke fjerne feilregistrering", HttpStatusCode.BadRequest),
}
