package no.nav.helse.spesialist.api.rest.vedtaksperioder.notater

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PutBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.harTilgangTilPerson
import no.nav.helse.spesialist.api.rest.resources.Vedtaksperioder
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.KommentarId
import no.nav.helse.spesialist.domain.VedtaksperiodeId

class PutFeilregistrerKommentarBehandler : PutBehandler<Vedtaksperioder.VedtaksperiodeId.Notater.NotatId.Kommentarer.KommentarId.Feilregistrer, Unit, Unit, ApiPostFeilregistrerKommentarErrorCode> {
    override fun behandle(
        resource: Vedtaksperioder.VedtaksperiodeId.Notater.NotatId.Kommentarer.KommentarId.Feilregistrer,
        request: Unit,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPostFeilregistrerKommentarErrorCode> {
        val vedtaksperiodeId = resource.parent.parent.parent.parent.parent.vedtaksperiodeId
        val vedtaksperiode =
            kallKontekst.transaksjon.vedtaksperiodeRepository.finn(VedtaksperiodeId(vedtaksperiodeId))
                ?: return RestResponse.Error(ApiPostFeilregistrerKommentarErrorCode.VEDTAKSPERIODE_IKKE_FUNNET)

        if (!kallKontekst.saksbehandler.harTilgangTilPerson(
                identitetsnummer = Identitetsnummer.fraString(identitetsnummer = vedtaksperiode.fødselsnummer),
                tilgangsgrupper = kallKontekst.tilgangsgrupper,
                transaksjon = kallKontekst.transaksjon,
            )
        ) {
            return RestResponse.Error(ApiPostFeilregistrerKommentarErrorCode.MANGLER_TILGANG_TIL_PERSON)
        }

        val kommentarId = KommentarId(resource.parent.kommentarId)
        val dialog =
            kallKontekst.transaksjon.dialogRepository.finnForKommentar(kommentarId) ?: return RestResponse.Error(
                ApiPostFeilregistrerKommentarErrorCode.DIALOG_IKKE_FUNNET,
            )

        dialog.feilregistrerKommentar(kommentarId)

        kallKontekst.transaksjon.dialogRepository.lagre(dialog)

        return RestResponse.OK(Unit)
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Notater")
        }
    }
}

enum class ApiPostFeilregistrerKommentarErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    VEDTAKSPERIODE_IKKE_FUNNET("Fant ikke vedtaksperiode", HttpStatusCode.NotFound),
    DIALOG_IKKE_FUNNET("Fant ikke dialog", HttpStatusCode.NotFound),
}
