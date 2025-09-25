package no.nav.helse.spesialist.api.rest.tilkommeninntekt

import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.graphql.mutation.InntektsendringerEventBygger
import no.nav.helse.spesialist.api.graphql.mutation.LeggTilTilkommenInntektResponse
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektInput
import no.nav.helse.spesialist.api.rest.HttpForbidden
import no.nav.helse.spesialist.api.rest.PostHåndterer
import no.nav.helse.spesialist.application.KøetMeldingPubliserer
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektPeriodeValidator

class PostTilkommenInntektLeggTilHåndterer : PostHåndterer<Unit, PostTilkommenInntektLeggTilHåndterer.RequestBody, LeggTilTilkommenInntektResponse> {
    data class RequestBody(
        val fodselsnummer: String,
        val verdier: ApiTilkommenInntektInput,
        val notatTilBeslutter: String,
    )

    override fun håndter(
        urlParametre: Unit,
        requestBody: RequestBody,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        meldingsKø: KøetMeldingPubliserer,
    ): LeggTilTilkommenInntektResponse {
        bekreftTilgangTilPerson(
            fødselsnummer = requestBody.fodselsnummer,
            saksbehandler = saksbehandler,
            tilgangsgrupper = tilgangsgrupper,
            transaksjon = transaksjon,
            feilSupplier = ::HttpForbidden,
        )

        val periode = requestBody.verdier.periode.fom tilOgMed requestBody.verdier.periode.tom
        TilkommenInntektPeriodeValidator.validerPeriode(
            periode = periode,
            organisasjonsnummer = requestBody.verdier.organisasjonsnummer,
            andreTilkomneInntekter = transaksjon.tilkommenInntektRepository.finnAlleForFødselsnummer(requestBody.fodselsnummer),
            vedtaksperioder = transaksjon.vedtaksperiodeRepository.finnVedtaksperioder(requestBody.fodselsnummer),
        )

        val tilkommenInntekt =
            TilkommenInntekt.ny(
                fødselsnummer = requestBody.fodselsnummer,
                saksbehandlerIdent = saksbehandler.ident,
                notatTilBeslutter = requestBody.notatTilBeslutter,
                totrinnsvurderingId =
                    finnEllerOpprettTotrinnsvurdering(
                        fodselsnummer = requestBody.fodselsnummer,
                        totrinnsvurderingRepository = transaksjon.totrinnsvurderingRepository,
                    ).id(),
                organisasjonsnummer = requestBody.verdier.organisasjonsnummer,
                periode = periode,
                periodebeløp = requestBody.verdier.periodebelop,
                ekskluderteUkedager = requestBody.verdier.ekskluderteUkedager.toSet(),
            )
        transaksjon.tilkommenInntektRepository.lagre(tilkommenInntekt)

        meldingsKø.publiser(
            fødselsnummer = tilkommenInntekt.fødselsnummer,
            hendelse = InntektsendringerEventBygger.forNy(tilkommenInntekt),
            årsak = "tilkommen inntekt lagt til",
        )

        return LeggTilTilkommenInntektResponse(tilkommenInntekt.id().value)
    }
}
