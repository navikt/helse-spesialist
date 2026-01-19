package no.nav.helse.spesialist.api.rest.vedtaksperioder.notater

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.PutBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.harTilgangTilPerson
import no.nav.helse.spesialist.api.rest.resources.Vedtaksperioder
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NotatId
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class PutFeilregistrerNotatBehandler : PutBehandler<Vedtaksperioder.VedtaksperiodeId.Notater.NotatId.Feilregistrer, Unit, Unit, ApiPostFeilregistrerNotatErrorCode> {
    override fun behandle(
        resource: Vedtaksperioder.VedtaksperiodeId.Notater.NotatId.Feilregistrer,
        request: Unit,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<Unit, ApiPostFeilregistrerNotatErrorCode> {
        val notatId = resource.parent.notatId
        val vedtaksperiodeId = resource.parent.parent.parent.vedtaksperiodeId
        val vedtaksperiode =
            transaksjon.vedtaksperiodeRepository.finn(VedtaksperiodeId(vedtaksperiodeId))
                ?: return RestResponse.Error(ApiPostFeilregistrerNotatErrorCode.VEDTAKSPERIODE_IKKE_FUNNET)

        if (!saksbehandler.harTilgangTilPerson(
                identitetsnummer = Identitetsnummer.fraString(identitetsnummer = vedtaksperiode.fødselsnummer),
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
            )
        ) {
            return RestResponse.Error(ApiPostFeilregistrerNotatErrorCode.MANGLER_TILGANG_TIL_PERSON)
        }

        val notat =
            transaksjon.notatRepository.finn(NotatId(notatId)) ?: return RestResponse.Error(
                ApiPostFeilregistrerNotatErrorCode.NOTAT_IKKE_FUNNET,
            )

        notat.feilregistrer()

        transaksjon.notatRepository.lagre(notat)

        return RestResponse.OK(Unit)
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Notater")
        }
    }
}

enum class ApiPostFeilregistrerNotatErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    VEDTAKSPERIODE_IKKE_FUNNET("Fant ikke vedtaksperiode", HttpStatusCode.NotFound),
    NOTAT_IKKE_FUNNET("Fant ikke notat", HttpStatusCode.NotFound),
}
