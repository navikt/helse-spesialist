package no.nav.helse.spesialist.api.rest.tilkommeninntekt

import io.ktor.http.Parameters
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.graphql.mutation.InntektsendringerEventBygger
import no.nav.helse.spesialist.api.rest.ApiTilkommenInntektInput
import no.nav.helse.spesialist.api.rest.HttpForbidden
import no.nav.helse.spesialist.api.rest.HttpNotFound
import no.nav.helse.spesialist.api.rest.PostHåndterer
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.getRequiredUUID
import no.nav.helse.spesialist.application.KøetMeldingPubliserer
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektId
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektPeriodeValidator
import java.util.UUID
import kotlin.reflect.typeOf

class PostTilkommenInntektGjenopprettHåndterer : PostHåndterer<PostTilkommenInntektGjenopprettHåndterer.URLParametre, PostTilkommenInntektGjenopprettHåndterer.RequestBody, Boolean> {
    override val urlPath: String = "tilkomne-inntekter/{tilkommenInntektId}/gjenopprett"

    data class URLParametre(
        val tilkommenInntektId: UUID,
    )

    data class RequestBody(
        val endretTil: ApiTilkommenInntektInput,
        val notatTilBeslutter: String,
    )

    override fun extractParametre(parameters: Parameters) =
        URLParametre(
            tilkommenInntektId = parameters.getRequiredUUID("tilkommenInntektId"),
        )

    override fun håndter(
        urlParametre: URLParametre,
        requestBody: RequestBody,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        meldingsKø: KøetMeldingPubliserer,
    ): RestResponse<Boolean> {
        val tilkommenInntekt =
            transaksjon.tilkommenInntektRepository.finn(TilkommenInntektId(urlParametre.tilkommenInntektId))
                ?: throw HttpNotFound("Fant ikke tilkommen inntekt med tilkommentInntektId ${urlParametre.tilkommenInntektId}")

        bekreftTilgangTilPerson(
            fødselsnummer = tilkommenInntekt.fødselsnummer,
            saksbehandler = saksbehandler,
            tilgangsgrupper = tilgangsgrupper,
            transaksjon = transaksjon,
            feilSupplier = ::HttpForbidden,
        )

        val endretTilPeriode = requestBody.endretTil.periode.fom tilOgMed requestBody.endretTil.periode.tom
        TilkommenInntektPeriodeValidator.validerPeriode(
            periode = endretTilPeriode,
            organisasjonsnummer = requestBody.endretTil.organisasjonsnummer,
            andreTilkomneInntekter =
                transaksjon.tilkommenInntektRepository
                    .finnAlleForFødselsnummer(tilkommenInntekt.fødselsnummer)
                    .minus(tilkommenInntekt),
            vedtaksperioder = transaksjon.vedtaksperiodeRepository.finnVedtaksperioder(tilkommenInntekt.fødselsnummer),
        )

        tilkommenInntekt.gjenopprett(
            organisasjonsnummer = requestBody.endretTil.organisasjonsnummer,
            periode = endretTilPeriode,
            periodebeløp = requestBody.endretTil.periodebelop,
            ekskluderteUkedager = requestBody.endretTil.ekskluderteUkedager.toSet(),
            saksbehandlerIdent = saksbehandler.ident,
            notatTilBeslutter = requestBody.notatTilBeslutter,
            totrinnsvurderingId =
                finnEllerOpprettTotrinnsvurdering(
                    fodselsnummer = tilkommenInntekt.fødselsnummer,
                    totrinnsvurderingRepository = transaksjon.totrinnsvurderingRepository,
                ).id(),
        )
        transaksjon.tilkommenInntektRepository.lagre(tilkommenInntekt)

        meldingsKø.publiser(
            fødselsnummer = tilkommenInntekt.fødselsnummer,
            hendelse = InntektsendringerEventBygger.forNy(tilkommenInntekt),
            årsak = "tilkommen inntekt gjenopprettet",
        )

        return RestResponse.ok(true)
    }

    override val urlParametersClass = URLParametre::class

    override val requestBodyType = typeOf<RequestBody>()

    override val responseBodyType = typeOf<Boolean>()
}
