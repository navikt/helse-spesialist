package no.nav.helse.spesialist.api.rest.dialoger.kommentarer

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiPatchKommentarRequest
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PatchBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Dialoger
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.KommentarId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class PatchKommentarBehandler : PatchBehandler<Dialoger.DialogId.Kommentar.KommentarId, ApiPatchKommentarRequest, Unit, ApiPatchKommentarErrorCode> {
    override val påkrevdTilgang = Tilgang.Skriv

    override fun behandle(
        resource: Dialoger.DialogId.Kommentar.KommentarId,
        request: ApiPatchKommentarRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPatchKommentarErrorCode> {
        // TODO: Mangler tilgangskontroll / relasjon til person
        if (!request.feilregistrert) return RestResponse.Error(ApiPatchKommentarErrorCode.KAN_IKKE_FJERNE_FEILREGISTRERING)

        val dialog =
            kallKontekst.transaksjon.dialogRepository.finn(DialogId(resource.parent.parent.dialogId))
                ?: return RestResponse.Error(ApiPatchKommentarErrorCode.DIALOG_IKKE_FUNNET)

        val kommentar =
            dialog.finnKommentar(KommentarId(resource.kommentarId))
                ?: return RestResponse.Error(ApiPatchKommentarErrorCode.KOMMENTAR_IKKE_FUNNET)

        if (kommentar.feilregistrertTidspunkt == null) {
            dialog.feilregistrerKommentar(kommentar.id())
            kallKontekst.transaksjon.dialogRepository.lagre(dialog)

            loggInfo("Markerte kommentar som feilregistrert", "${dialog.id()}, ${kommentar.id()}")
        }

        return RestResponse.OK(Unit)
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Dialoger")
        }
    }
}

enum class ApiPatchKommentarErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    KAN_IKKE_FJERNE_FEILREGISTRERING("Kan ikke fjerne feilregistrering", HttpStatusCode.BadRequest),
    DIALOG_IKKE_FUNNET("Fant ikke dialog", HttpStatusCode.NotFound),
    KOMMENTAR_IKKE_FUNNET("Fant ikke kommentar", HttpStatusCode.NotFound),
}
