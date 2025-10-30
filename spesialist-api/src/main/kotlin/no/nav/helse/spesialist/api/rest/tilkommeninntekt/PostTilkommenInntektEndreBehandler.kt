package no.nav.helse.spesialist.api.rest.tilkommeninntekt

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.graphql.mutation.InntektsendringerEventBygger
import no.nav.helse.spesialist.api.rest.ApiEndreTilkommenInntektRequest
import no.nav.helse.spesialist.api.rest.HttpForbidden
import no.nav.helse.spesialist.api.rest.HttpNotFound
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.TilkomneInntekter
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektId
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektPeriodeValidator

class PostTilkommenInntektEndreBehandler : PostBehandler<TilkomneInntekter.Id.Endre, ApiEndreTilkommenInntektRequest, Boolean> {
    override fun behandle(
        resource: TilkomneInntekter.Id.Endre,
        request: ApiEndreTilkommenInntektRequest,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<Boolean> {
        val tilkommenInntekt =
            transaksjon.tilkommenInntektRepository.finn(TilkommenInntektId(resource.parent.tilkommenInntektId))
                ?: throw HttpNotFound(
                    title = "Fant ikke tilkommen inntekt",
                    detail = "Tilkommen inntekt med id ${resource.parent.tilkommenInntektId} ble ikke funnet",
                )

        bekreftTilgangTilPerson(
            fødselsnummer = tilkommenInntekt.fødselsnummer,
            saksbehandler = saksbehandler,
            tilgangsgrupper = tilgangsgrupper,
            transaksjon = transaksjon,
            feilSupplier = ::HttpForbidden,
        )

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

        val arbeidsgiverFør = tilkommenInntekt.organisasjonsnummer
        val dagerFør = tilkommenInntekt.dagerTilGradering()
        val dagsbeløpFør = tilkommenInntekt.dagbeløp()

        tilkommenInntekt.endreTil(
            organisasjonsnummer = request.endretTil.organisasjonsnummer,
            periode = request.endretTil.periode.fom tilOgMed request.endretTil.periode.tom,
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
            outbox.leggTil(
                fødselsnummer = tilkommenInntekt.fødselsnummer,
                hendelse = it,
                årsak = "tilkommen inntekt endret",
            )
        }

        return RestResponse.ok(true)
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Tilkommen inntekt")
            operationId = operationIdBasertPåKlassenavn()
            request {
                body<ApiEndreTilkommenInntektRequest>()
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Alltid true - henger igjen fra at Apollo ikke tåler å ikke få noe i body"
                    body<Boolean>()
                }
            }
        }
    }
}
