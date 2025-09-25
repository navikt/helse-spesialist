package no.nav.helse.spesialist.api.rest.tilkommeninntekt

import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.graphql.mutation.InntektsendringerEventBygger
import no.nav.helse.spesialist.api.graphql.mutation.TilkommenInntektMutationHandler
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektInput
import no.nav.helse.spesialist.api.rest.HttpForbidden
import no.nav.helse.spesialist.api.rest.HttpNotFound
import no.nav.helse.spesialist.api.rest.PostHåndterer
import no.nav.helse.spesialist.api.rest.RestHandler
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektId
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektPeriodeValidator
import java.util.UUID

class PostTilkommenInntektEndreHåndterer(
    private val handler: RestHandler,
    tilkommenInntektMutationHandler: TilkommenInntektMutationHandler,
) : PostHåndterer<PostTilkommenInntektEndreHåndterer.URLParametre, PostTilkommenInntektEndreHåndterer.RequestBody, HttpStatusCode> {
    private val meldingPubliserer = tilkommenInntektMutationHandler.meldingPubliserer

    data class URLParametre(
        val tilkommenInntektId: UUID,
    )

    data class RequestBody(
        val endretTil: ApiTilkommenInntektInput,
        val notatTilBeslutter: String,
    )

    override fun håndter(
        urlParametre: URLParametre,
        requestBody: RequestBody,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): HttpStatusCode {
        val tilkommenInntekt =
            transaksjon.tilkommenInntektRepository.finn(TilkommenInntektId(urlParametre.tilkommenInntektId))
                ?: throw HttpNotFound("Fant ikke tilkommen inntekt med tilkommentInntektId ${urlParametre.tilkommenInntektId}")

        handler.kontrollerTilgangTilPerson(
            fødselsnummer = tilkommenInntekt.fødselsnummer,
            saksbehandler = saksbehandler,
            tilgangsgrupper = tilgangsgrupper,
            transaksjon = transaksjon,
            feilSupplier = ::HttpForbidden,
        )

        endreTilkommenInntekt(
            tilkommenInntekt = tilkommenInntekt,
            endretTil = requestBody.endretTil,
            notatTilBeslutter = requestBody.notatTilBeslutter,
            saksbehandler = saksbehandler,
            session = transaksjon,
        )

        return HttpStatusCode.NoContent
    }

    private fun endreTilkommenInntekt(
        tilkommenInntekt: TilkommenInntekt,
        endretTil: ApiTilkommenInntektInput,
        notatTilBeslutter: String,
        saksbehandler: Saksbehandler,
        session: SessionContext,
    ) {
        val endretTilPeriode = endretTil.periode.fom tilOgMed endretTil.periode.tom
        TilkommenInntektPeriodeValidator.validerPeriode(
            periode = endretTilPeriode,
            organisasjonsnummer = endretTil.organisasjonsnummer,
            andreTilkomneInntekter =
                session.tilkommenInntektRepository
                    .finnAlleForFødselsnummer(tilkommenInntekt.fødselsnummer)
                    .minus(tilkommenInntekt),
            vedtaksperioder = session.vedtaksperiodeRepository.finnVedtaksperioder(tilkommenInntekt.fødselsnummer),
        )

        val arbeidsgiverFør = tilkommenInntekt.organisasjonsnummer
        val dagerFør = tilkommenInntekt.dagerTilGradering()
        val dagsbeløpFør = tilkommenInntekt.dagbeløp()

        tilkommenInntekt.endreTil(
            organisasjonsnummer = endretTil.organisasjonsnummer,
            periode = endretTil.periode.fom tilOgMed endretTil.periode.tom,
            periodebeløp = endretTil.periodebelop,
            ekskluderteUkedager = endretTil.ekskluderteUkedager.toSet(),
            saksbehandlerIdent = saksbehandler.ident,
            notatTilBeslutter = notatTilBeslutter,
            totrinnsvurderingId =
                finnEllerOpprettTotrinnsvurdering(
                    fodselsnummer = tilkommenInntekt.fødselsnummer,
                    totrinnsvurderingRepository = session.totrinnsvurderingRepository,
                ).id(),
        )
        session.tilkommenInntektRepository.lagre(tilkommenInntekt)

        val arbeidsgiverEtter = tilkommenInntekt.organisasjonsnummer
        val dagerEtter = tilkommenInntekt.dagerTilGradering()
        val dagsbeløpEtter = tilkommenInntekt.dagbeløp()

        val event =
            InntektsendringerEventBygger.forEndring(
                arbeidsgiverFør = arbeidsgiverFør,
                arbeidsgiverEtter = arbeidsgiverEtter,
                dagerFør = dagerFør,
                dagerEtter = dagerEtter,
                dagsbeløpFør = dagsbeløpFør,
                dagsbeløpEtter = dagsbeløpEtter,
            )

        event?.let {
            meldingPubliserer.publiser(
                fødselsnummer = tilkommenInntekt.fødselsnummer,
                hendelse = it,
                årsak = "tilkommen inntekt endret",
            )
        }
    }
}
