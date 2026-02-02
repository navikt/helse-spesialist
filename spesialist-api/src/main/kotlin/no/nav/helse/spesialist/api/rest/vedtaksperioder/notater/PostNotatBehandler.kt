package no.nav.helse.spesialist.api.rest.vedtaksperioder.notater

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiNotatRequest
import no.nav.helse.spesialist.api.rest.ApiNotatResponse
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.harTilgangTilPerson
import no.nav.helse.spesialist.api.rest.resources.Vedtaksperioder
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.VedtaksperiodeId

class PostNotatBehandler : PostBehandler<Vedtaksperioder.VedtaksperiodeId.Notater, ApiNotatRequest, ApiNotatResponse, ApiPostNotatErrorCode> {
    override fun behandle(
        resource: Vedtaksperioder.VedtaksperiodeId.Notater,
        request: ApiNotatRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiNotatResponse, ApiPostNotatErrorCode> {
        val vedtaksperiodeId = resource.parent.vedtaksperiodeId
        val vedtaksperiode =
            kallKontekst.transaksjon.vedtaksperiodeRepository.finn(VedtaksperiodeId(vedtaksperiodeId))
                ?: return RestResponse.Error(ApiPostNotatErrorCode.VEDTAKSPERIODE_IKKE_FUNNET)

        if (!kallKontekst.saksbehandler.harTilgangTilPerson(
                identitetsnummer = Identitetsnummer.fraString(identitetsnummer = vedtaksperiode.fødselsnummer),
                tilgangsgrupper = kallKontekst.tilgangsgrupper,
                transaksjon = kallKontekst.transaksjon,
            )
        ) {
            return RestResponse.Error(ApiPostNotatErrorCode.MANGLER_TILGANG_TIL_PERSON)
        }

        val dialog = Dialog.Factory.ny()
        kallKontekst.transaksjon.dialogRepository.lagre(dialog)

        val notat =
            Notat.Factory.ny(
                type = NotatType.Generelt,
                tekst = request.tekst,
                dialogRef = dialog.id(),
                vedtaksperiodeId = vedtaksperiodeId,
                saksbehandlerOid = kallKontekst.saksbehandler.id,
            )
        kallKontekst.transaksjon.notatRepository.lagre(notat)

        return RestResponse.Created(ApiNotatResponse(id = notat.id().value))
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
