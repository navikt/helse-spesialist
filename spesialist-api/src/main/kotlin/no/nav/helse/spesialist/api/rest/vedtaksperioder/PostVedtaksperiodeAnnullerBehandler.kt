package no.nav.helse.spesialist.api.rest.vedtaksperioder

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.Annullering
import no.nav.helse.modell.melding.AnnullertUtbetalingEvent
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiVedtaksperiodeAnnullerRequest
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.harTilgangTilPerson
import no.nav.helse.spesialist.api.rest.resources.Vedtaksperioder
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.VedtaksperiodeId

class PostVedtaksperiodeAnnullerBehandler : PostBehandler<Vedtaksperioder.VedtaksperiodeId.Annuller, ApiVedtaksperiodeAnnullerRequest, Unit, ApiPostVedtaksperiodeAnnullerErrorCode> {
    override fun behandle(
        resource: Vedtaksperioder.VedtaksperiodeId.Annuller,
        request: ApiVedtaksperiodeAnnullerRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPostVedtaksperiodeAnnullerErrorCode> {
        val vedtaksperiodeId = resource.parent.vedtaksperiodeId

        val vedtaksperiode =
            kallKontekst.transaksjon.vedtaksperiodeRepository.finn(VedtaksperiodeId(vedtaksperiodeId))
                ?: return RestResponse.Error(ApiPostVedtaksperiodeAnnullerErrorCode.VEDTAKSPERIODE_IKKE_FUNNET)

        if (!kallKontekst.saksbehandler.harTilgangTilPerson(
                identitetsnummer = Identitetsnummer.fraString(identitetsnummer = vedtaksperiode.fødselsnummer),
                tilgangsgrupper = kallKontekst.tilgangsgrupper,
                transaksjon = kallKontekst.transaksjon,
            )
        ) {
            return RestResponse.Error(ApiPostVedtaksperiodeAnnullerErrorCode.MANGLER_TILGANG_TIL_PERSON)
        }

        if (kallKontekst.transaksjon.annulleringRepository.finnAnnullering(vedtaksperiodeId = vedtaksperiodeId) != null) {
            return RestResponse.Error(ApiPostVedtaksperiodeAnnullerErrorCode.ALLEREDE_ANNULLERT)
        }

        kallKontekst.transaksjon.annulleringRepository.lagreAnnullering(
            annullering =
                Annullering.Factory.ny(
                    arbeidsgiverFagsystemId = request.arbeidsgiverFagsystemId,
                    personFagsystemId = request.personFagsystemId,
                    saksbehandlerOid = kallKontekst.saksbehandler.id,
                    vedtaksperiodeId = vedtaksperiodeId,
                    årsaker = request.årsaker.map { it.årsak },
                    kommentar = request.kommentar,
                ),
        )

        kallKontekst.outbox.leggTil(
            fødselsnummer = vedtaksperiode.fødselsnummer,
            hendelse =
                AnnullertUtbetalingEvent(
                    fødselsnummer = vedtaksperiode.fødselsnummer,
                    organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                    saksbehandlerOid = kallKontekst.saksbehandler.id.value,
                    saksbehandlerIdent = kallKontekst.saksbehandler.ident.value,
                    saksbehandlerEpost = kallKontekst.saksbehandler.epost,
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
