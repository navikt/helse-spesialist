package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.modell.Annullering
import no.nav.helse.modell.melding.AnnullertUtbetalingEvent
import no.nav.helse.spesialist.api.rest.resources.Vedtaksperioder
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class PostVedtaksperiodeAnnullerBehandler : PostBehandler<Vedtaksperioder.Id.Annuller, ApiVedtaksperiodeAnnullerRequest, Unit, ApiPostVedtaksperiodeAnnullerErrorCode> {
    override fun behandle(
        resource: Vedtaksperioder.Id.Annuller,
        request: ApiVedtaksperiodeAnnullerRequest,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<Unit, ApiPostVedtaksperiodeAnnullerErrorCode> {
        val vedtaksperiodeId = resource.parent.vedtaksperiodeId

        val vedtaksperiode =
            transaksjon.vedtaksperiodeRepository.finn(VedtaksperiodeId(vedtaksperiodeId))
                ?: return RestResponse.Error(ApiPostVedtaksperiodeAnnullerErrorCode.VEDTAKSPERIODE_IKKE_FUNNET)

        if (!saksbehandler.harTilgangTilPerson(
                identitetsnummer = Identitetsnummer.fraString(identitetsnummer = vedtaksperiode.fødselsnummer),
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
            )
        ) {
            return RestResponse.Error(ApiPostVedtaksperiodeAnnullerErrorCode.MANGLER_TILGANG_TIL_PERSON)
        }

        if (transaksjon.annulleringRepository.finnAnnullering(vedtaksperiodeId = vedtaksperiodeId) != null) {
            return RestResponse.Error(ApiPostVedtaksperiodeAnnullerErrorCode.ALLEREDE_ANNULLERT)
        }

        transaksjon.annulleringRepository.lagreAnnullering(
            annullering =
                Annullering.Factory.ny(
                    arbeidsgiverFagsystemId = request.arbeidsgiverFagsystemId,
                    personFagsystemId = request.personFagsystemId,
                    saksbehandlerOid = saksbehandler.id,
                    vedtaksperiodeId = vedtaksperiodeId,
                    årsaker = request.årsaker.map { it.årsak },
                    kommentar = request.kommentar,
                ),
        )

        outbox.leggTil(
            fødselsnummer = vedtaksperiode.fødselsnummer,
            hendelse =
                AnnullertUtbetalingEvent(
                    fødselsnummer = vedtaksperiode.fødselsnummer,
                    organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                    saksbehandlerOid = saksbehandler.id.value,
                    saksbehandlerIdent = saksbehandler.ident,
                    saksbehandlerEpost = saksbehandler.epost,
                    vedtaksperiodeId = vedtaksperiodeId,
                    begrunnelser = request.årsaker.map { arsak -> arsak.årsak },
                    arsaker =
                        request.årsaker.map { arsak ->
                            AnnullertUtbetalingEvent.Årsak(
                                key = arsak.key,
                                arsak = arsak.årsak,
                            )
                        },
                    kommentar = request.kommentar,
                ),
            årsak = "annullering av utbetaling",
        )

        return RestResponse.NoContent()
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Vedtaksperiode")
        }
    }
}

enum class ApiPostVedtaksperiodeAnnullerErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    VEDTAKSPERIODE_IKKE_FUNNET("Fant ikke vedtaksperiode", HttpStatusCode.NotFound),
    ALLEREDE_ANNULLERT("Perioden er allerede annullert", HttpStatusCode.Conflict),
}
