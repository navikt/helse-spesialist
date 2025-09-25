package no.nav.helse.spesialist.api.rest.tilkommeninntekt

import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.graphql.mutation.InntektsendringerEventBygger
import no.nav.helse.spesialist.api.graphql.mutation.LeggTilTilkommenInntektResponse
import no.nav.helse.spesialist.api.graphql.mutation.TilkommenInntektMutationHandler
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektInput
import no.nav.helse.spesialist.api.rest.HttpForbidden
import no.nav.helse.spesialist.api.rest.PostHåndterer
import no.nav.helse.spesialist.api.rest.RestHandler
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektPeriodeValidator

class PostTilkommenInntektLeggTilHåndterer(
    private val handler: RestHandler,
    tilkommenInntektMutationHandler: TilkommenInntektMutationHandler,
) : PostHåndterer<Unit, PostTilkommenInntektLeggTilHåndterer.RequestBody, LeggTilTilkommenInntektResponse> {
    private val meldingPubliserer = tilkommenInntektMutationHandler.meldingPubliserer

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
    ): LeggTilTilkommenInntektResponse {
        handler.kontrollerTilgangTilPerson(
            fødselsnummer = requestBody.fodselsnummer,
            saksbehandler = saksbehandler,
            tilgangsgrupper = tilgangsgrupper,
            transaksjon = transaksjon,
            feilSupplier = ::HttpForbidden,
        )

        return leggTilTilkommenInntekt(
            fodselsnummer = requestBody.fodselsnummer,
            verdier = requestBody.verdier,
            notatTilBeslutter = requestBody.notatTilBeslutter,
            saksbehandler = saksbehandler,
            session = transaksjon,
        )
    }

    private fun leggTilTilkommenInntekt(
        fodselsnummer: String,
        verdier: ApiTilkommenInntektInput,
        notatTilBeslutter: String,
        saksbehandler: Saksbehandler,
        session: SessionContext,
    ): LeggTilTilkommenInntektResponse {
        val periode = verdier.periode.fom tilOgMed verdier.periode.tom
        TilkommenInntektPeriodeValidator.validerPeriode(
            periode = periode,
            organisasjonsnummer = verdier.organisasjonsnummer,
            andreTilkomneInntekter = session.tilkommenInntektRepository.finnAlleForFødselsnummer(fodselsnummer),
            vedtaksperioder = session.vedtaksperiodeRepository.finnVedtaksperioder(fodselsnummer),
        )

        val tilkommenInntekt =
            TilkommenInntekt.ny(
                fødselsnummer = fodselsnummer,
                saksbehandlerIdent = saksbehandler.ident,
                notatTilBeslutter = notatTilBeslutter,
                totrinnsvurderingId =
                    finnEllerOpprettTotrinnsvurdering(
                        fodselsnummer = fodselsnummer,
                        totrinnsvurderingRepository = session.totrinnsvurderingRepository,
                    ).id(),
                organisasjonsnummer = verdier.organisasjonsnummer,
                periode = periode,
                periodebeløp = verdier.periodebelop,
                ekskluderteUkedager = verdier.ekskluderteUkedager.toSet(),
            )
        session.tilkommenInntektRepository.lagre(tilkommenInntekt)

        meldingPubliserer.publiser(
            fødselsnummer = tilkommenInntekt.fødselsnummer,
            hendelse = InntektsendringerEventBygger.forNy(tilkommenInntekt),
            årsak = "tilkommen inntekt lagt til",
        )

        return LeggTilTilkommenInntektResponse(tilkommenInntekt.id().value)
    }
}
