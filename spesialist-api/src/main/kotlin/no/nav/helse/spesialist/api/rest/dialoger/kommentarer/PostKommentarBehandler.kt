package no.nav.helse.spesialist.api.rest.dialoger.kommentarer

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiKommentarRequest
import no.nav.helse.spesialist.api.rest.ApiKommentarResponse
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Dialoger
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class PostKommentarBehandler : PostBehandler<Dialoger.DialogId.Kommentar, ApiKommentarRequest, ApiKommentarResponse, ApiPostKommentarErrorCode> {
    override val påkrevdTilgang = Tilgang.Skriv

    override fun behandle(
        resource: Dialoger.DialogId.Kommentar,
        request: ApiKommentarRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiKommentarResponse, ApiPostKommentarErrorCode> {
        // TODO: Mangler tilgangskontroll / relasjon til person
        val dialog =
            kallKontekst.transaksjon.dialogRepository.finn(DialogId(resource.parent.dialogId))
                ?: return RestResponse.Error(
                    ApiPostKommentarErrorCode.DIALOG_IKKE_FUNNET,
                )
        val kommentar =
            dialog.leggTilKommentar(
                tekst = request.tekst,
                saksbehandlerident = kallKontekst.saksbehandler.ident,
            )
        kallKontekst.transaksjon.dialogRepository.lagre(dialog)

        loggInfo("Opprettet kommentar", "${dialog.id()}, ${kommentar.id()}")

        return RestResponse.Created(ApiKommentarResponse(id = kommentar.id().value))
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Dialoger")
        }
    }
}

enum class ApiPostKommentarErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    DIALOG_IKKE_FUNNET("Fant ikke dialog", HttpStatusCode.NotFound),
}
