package no.nav.helse.spesialist.api.rest.vedtaksperioder.notater

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiKommentarRequest
import no.nav.helse.spesialist.api.rest.ApiKommentarResponse
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.harTilgangTilPerson
import no.nav.helse.spesialist.api.rest.resources.Vedtaksperioder
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NotatId
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class PostKommentarBehandler : PostBehandler<Vedtaksperioder.VedtaksperiodeId.Notater.NotatId.Kommentarer, ApiKommentarRequest, ApiKommentarResponse, ApiPostKommentarErrorCode> {
    override fun behandle(
        resource: Vedtaksperioder.VedtaksperiodeId.Notater.NotatId.Kommentarer,
        request: ApiKommentarRequest,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<ApiKommentarResponse, ApiPostKommentarErrorCode> {
        val vedtaksperiodeId = resource.parent.parent.parent.vedtaksperiodeId
        val vedtaksperiode =
            transaksjon.vedtaksperiodeRepository.finn(VedtaksperiodeId(vedtaksperiodeId))
                ?: return RestResponse.Error(ApiPostKommentarErrorCode.VEDTAKSPERIODE_IKKE_FUNNET)

        if (!saksbehandler.harTilgangTilPerson(
                identitetsnummer = Identitetsnummer.fraString(identitetsnummer = vedtaksperiode.fødselsnummer),
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
            )
        ) {
            return RestResponse.Error(ApiPostKommentarErrorCode.MANGLER_TILGANG_TIL_PERSON)
        }
        val notatId = resource.parent.notatId
        val notat =
            transaksjon.notatRepository.finn(NotatId(notatId)) ?: return RestResponse.Error(
                ApiPostKommentarErrorCode.NOTAT_IKKE_FUNNET,
            )
        val dialog =
            transaksjon.dialogRepository.finn(notat.dialogRef) ?: return RestResponse.Error(
                ApiPostKommentarErrorCode.DIALOG_IKKE_FUNNET,
            )

        val kommentar = dialog.leggTilKommentar(tekst = request.tekst, saksbehandlerident = saksbehandler.ident)
        transaksjon.dialogRepository.lagre(dialog)

        return RestResponse.Created(ApiKommentarResponse(id = kommentar.id().value))
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Notater")
        }
    }
}

enum class ApiPostKommentarErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    VEDTAKSPERIODE_IKKE_FUNNET("Fant ikke vedtaksperiode", HttpStatusCode.NotFound),
    NOTAT_IKKE_FUNNET("Fant ikke notat", HttpStatusCode.NotFound),
    DIALOG_IKKE_FUNNET("Fant ikke dialog", HttpStatusCode.NotFound),
}
