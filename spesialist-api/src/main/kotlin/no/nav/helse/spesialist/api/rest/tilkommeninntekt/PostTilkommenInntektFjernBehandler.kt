package no.nav.helse.spesialist.api.rest.tilkommeninntekt

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiFjernTilkommenInntektRequest
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.TilkomneInntekter
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektId

class PostTilkommenInntektFjernBehandler : PostBehandler<TilkomneInntekter.Id.Fjern, ApiFjernTilkommenInntektRequest, Boolean, ApiPostTilkommenInntektFjernErrorCode> {
    override fun behandle(
        resource: TilkomneInntekter.Id.Fjern,
        request: ApiFjernTilkommenInntektRequest,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<Boolean, ApiPostTilkommenInntektFjernErrorCode> {
        val tilkommenInntekt =
            transaksjon.tilkommenInntektRepository.finn(TilkommenInntektId(resource.parent.tilkommenInntektId))
                ?: return RestResponse.Error(
                    errorCode = ApiPostTilkommenInntektFjernErrorCode.FANT_IKKE_TILKOMMEN_INNTEKT,
                    detail = "Tilkommen inntekt med id ${resource.parent.tilkommenInntektId} ble ikke funnet",
                )

        if (!harTilgangTilPerson(
                fødselsnummer = tilkommenInntekt.fødselsnummer,
                saksbehandler = saksbehandler,
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
            )
        ) {
            return RestResponse.Error(ApiPostTilkommenInntektFjernErrorCode.MANGLER_TILGANG_TIL_PERSON)
        }

        tilkommenInntekt.fjern(
            saksbehandlerIdent = saksbehandler.ident,
            notatTilBeslutter = request.notatTilBeslutter,
            totrinnsvurderingId =
                finnEllerOpprettTotrinnsvurdering(
                    fodselsnummer = tilkommenInntekt.fødselsnummer,
                    totrinnsvurderingRepository = transaksjon.totrinnsvurderingRepository,
                ).id(),
        )
        transaksjon.tilkommenInntektRepository.lagre(tilkommenInntekt)

        outbox.leggTil(
            fødselsnummer = tilkommenInntekt.fødselsnummer,
            hendelse = InntektsendringerEventBygger.forFjernet(tilkommenInntekt),
            årsak = "tilkommen inntekt fjernet",
        )

        return RestResponse.OK(true)
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Tilkommen inntekt")
        }
    }
}

enum class ApiPostTilkommenInntektFjernErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    FANT_IKKE_TILKOMMEN_INNTEKT("Fant ikke tilkommen inntekt", HttpStatusCode.NotFound),
}
