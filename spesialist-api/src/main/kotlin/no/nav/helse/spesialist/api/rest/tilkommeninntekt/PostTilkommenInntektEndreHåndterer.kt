package no.nav.helse.spesialist.api.rest.tilkommeninntekt

import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.graphql.mutation.InntektsendringerEventBygger
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektInput
import no.nav.helse.spesialist.api.rest.HttpForbidden
import no.nav.helse.spesialist.api.rest.HttpNotFound
import no.nav.helse.spesialist.api.rest.PostHåndterer
import no.nav.helse.spesialist.application.KøetMeldingPubliserer
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektId
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektPeriodeValidator
import java.util.UUID

class PostTilkommenInntektEndreHåndterer : PostHåndterer<PostTilkommenInntektEndreHåndterer.URLParametre, PostTilkommenInntektEndreHåndterer.RequestBody, HttpStatusCode> {
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
        meldingsKø: KøetMeldingPubliserer,
    ): HttpStatusCode {
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

        val arbeidsgiverFør = tilkommenInntekt.organisasjonsnummer
        val dagerFør = tilkommenInntekt.dagerTilGradering()
        val dagsbeløpFør = tilkommenInntekt.dagbeløp()

        tilkommenInntekt.endreTil(
            organisasjonsnummer = requestBody.endretTil.organisasjonsnummer,
            periode = requestBody.endretTil.periode.fom tilOgMed requestBody.endretTil.periode.tom,
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
            meldingsKø.publiser(
                fødselsnummer = tilkommenInntekt.fødselsnummer,
                hendelse = it,
                årsak = "tilkommen inntekt endret",
            )
        }

        return HttpStatusCode.NoContent
    }
}
