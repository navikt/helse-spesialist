package no.nav.helse.spesialist.api.rest.tilkommeninntekt

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiGjenopprettTilkommenInntektRequest
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.TilkomneInntekter
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektId
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektPeriodeValidator

class PostTilkommenInntektGjenopprettBehandler : PostBehandler<TilkomneInntekter.Id.Gjenopprett, ApiGjenopprettTilkommenInntektRequest, Unit, ApiPostTilkommenInntektGjenopprettErrorCode> {
    override fun behandle(
        resource: TilkomneInntekter.Id.Gjenopprett,
        request: ApiGjenopprettTilkommenInntektRequest,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<Unit, ApiPostTilkommenInntektGjenopprettErrorCode> {
        val tilkommenInntekt =
            transaksjon.tilkommenInntektRepository.finn(TilkommenInntektId(resource.parent.tilkommenInntektId))
                ?: return RestResponse.Error(
                    errorCode = ApiPostTilkommenInntektGjenopprettErrorCode.FANT_IKKE_TILKOMMEN_INNTEKT,
                    detail = "Tilkommen inntekt med id ${resource.parent.tilkommenInntektId} ble ikke funnet",
                )

        if (!harTilgangTilPerson(
                fødselsnummer = tilkommenInntekt.fødselsnummer,
                saksbehandler = saksbehandler,
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
            )
        ) {
            return RestResponse.Error(ApiPostTilkommenInntektGjenopprettErrorCode.MANGLER_TILGANG_TIL_PERSON)
        }

        val endretTilPeriode = request.endretTil.periode.fom tilOgMed request.endretTil.periode.tom
        TilkommenInntektPeriodeValidator.validerPeriode(
            periode = endretTilPeriode,
            organisasjonsnummer = request.endretTil.organisasjonsnummer,
            andreTilkomneInntekter =
                transaksjon.tilkommenInntektRepository
                    .finnAlleForFødselsnummer(tilkommenInntekt.fødselsnummer)
                    .minus(tilkommenInntekt),
            vedtaksperioder = transaksjon.legacyVedtaksperiodeRepository.finnVedtaksperioder(tilkommenInntekt.fødselsnummer),
        )

        tilkommenInntekt.gjenopprett(
            organisasjonsnummer = request.endretTil.organisasjonsnummer,
            periode = endretTilPeriode,
            periodebeløp = request.endretTil.periodebelop,
            ekskluderteUkedager = request.endretTil.ekskluderteUkedager.toSet(),
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
            hendelse = InntektsendringerEventBygger.forNy(tilkommenInntekt),
            årsak = "tilkommen inntekt gjenopprettet",
        )

        return RestResponse.NoContent()
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Tilkommen inntekt")
        }
    }
}

enum class ApiPostTilkommenInntektGjenopprettErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    FANT_IKKE_TILKOMMEN_INNTEKT("Fant ikke tilkommen inntekt", HttpStatusCode.NotFound),
}
