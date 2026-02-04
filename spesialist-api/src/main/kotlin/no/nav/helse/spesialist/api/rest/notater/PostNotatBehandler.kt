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
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle

class PostNotatBehandler : PostBehandler<Notater, ApiNotatRequest, ApiNotatResponse, ApiPostNotatErrorCode> {
    override val autoriserteBrukerroller: Set<Brukerrolle> = setOf(Brukerrolle.SAKSBEHANDLER)

    override fun behandle(
        resource: Notater,
        request: ApiNotatRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiNotatResponse, ApiPostNotatErrorCode> {
        val vedtaksperiode =
            kallKontekst.transaksjon.vedtaksperiodeRepository.finn(VedtaksperiodeId(request.vedtaksperiodeId))
                ?: return RestResponse.Error(ApiPostNotatErrorCode.VEDTAKSPERIODE_IKKE_FUNNET)

        val identitetsnummer = Identitetsnummer.fraString(identitetsnummer = vedtaksperiode.fødselsnummer)
        return kallKontekst.medPerson(
            identitetsnummer = identitetsnummer,
            personIkkeFunnet = ApiPostNotatErrorCode.PERSON_IKKE_FUNNET,
            manglerTilgangTilPerson = ApiPostNotatErrorCode.MANGLER_TILGANG_TIL_PERSON,
        ) {
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

            RestResponse.Created(ApiNotatResponse(id = notat.id().value))
        }
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
    PERSON_IKKE_FUNNET("Person ikke funnet", HttpStatusCode.InternalServerError),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    VEDTAKSPERIODE_IKKE_FUNNET("Fant ikke vedtaksperiode", HttpStatusCode.NotFound),
}
