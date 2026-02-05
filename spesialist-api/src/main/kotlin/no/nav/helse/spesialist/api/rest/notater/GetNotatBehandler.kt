package no.nav.helse.spesialist.api.rest.notater

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiNotat
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.harTilgangTilPerson
import no.nav.helse.spesialist.api.rest.mapping.tilApiNotat
import no.nav.helse.spesialist.api.rest.resources.Notater
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NotatId
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class GetNotatBehandler : GetBehandler<Notater.NotatId, ApiNotat, GetNotatErrorCode> {
    override val påkrevdTilgang = Tilgang.Les

    override fun behandle(
        resource: Notater.NotatId,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiNotat, GetNotatErrorCode> {
        val notatId = NotatId(resource.notatId)
        val notat =
            kallKontekst.transaksjon.notatRepository.finn(notatId)
                ?: return RestResponse.Error(GetNotatErrorCode.NOTAT_IKKE_FUNNET)

        val vedtaksperiode =
            kallKontekst.transaksjon.vedtaksperiodeRepository.finn(VedtaksperiodeId(notat.vedtaksperiodeId))
                ?: error("Fant ikke vedtaksperiode")

        if (!kallKontekst.saksbehandler.harTilgangTilPerson(
                identitetsnummer = Identitetsnummer.fraString(identitetsnummer = vedtaksperiode.fødselsnummer),
                brukerroller = kallKontekst.brukerroller,
                transaksjon = kallKontekst.transaksjon,
            )
        ) {
            return RestResponse.Error(GetNotatErrorCode.MANGLER_TILGANG_TIL_PERSON)
        }

        val dialog =
            kallKontekst.transaksjon.dialogRepository.finn(notat.dialogRef)
                ?: error("Kunne ikke finne dialog med id ${notat.dialogRef}")

        return RestResponse.OK(notat.tilApiNotat(kallKontekst.saksbehandler, dialog))
    }

    override fun openApi(config: RouteConfig) {
        config.tags("Notater")
    }
}

enum class GetNotatErrorCode(
    override val statusCode: HttpStatusCode,
    override val title: String,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON(
        HttpStatusCode.Forbidden,
        "Mangler tilgang til person",
    ),
    NOTAT_IKKE_FUNNET(HttpStatusCode.NotFound, "Fant ikke notat"),
}
