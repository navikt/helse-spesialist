package no.nav.helse.spesialist.api.rest.behandlinger

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiForsikring
import no.nav.helse.spesialist.api.rest.ForsikringInnhold
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Behandlinger
import no.nav.helse.spesialist.application.ForsikringHenter
import no.nav.helse.spesialist.domain.Forsikring
import no.nav.helse.spesialist.domain.ResultatAvForsikring
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class GetForsikringForPersonBehandler(
    private val forsikringHenter: ForsikringHenter,
) : GetBehandler<Behandlinger.BehandlingId.Forsikring, ApiForsikring, ApiForsikringErrorCode> {
    override val påkrevdTilgang = Tilgang.Les

    override fun behandle(
        resource: Behandlinger.BehandlingId.Forsikring,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiForsikring, ApiForsikringErrorCode> {
        val spleisBehandlingId = SpleisBehandlingId(resource.parent.behandlingId)

        return kallKontekst.medBehandling(
            spleisBehandlingId = spleisBehandlingId,
            behandlingIkkeFunnet = { ApiForsikringErrorCode.BEHANDLING_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiForsikringErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { _, _, _ ->
            hentForsikringForBehandling(spleisBehandlingId)
        }
    }

    private fun hentForsikringForBehandling(
        spleisBehandlingId: SpleisBehandlingId,
    ): RestResponse<ApiForsikring, ApiForsikringErrorCode> {
        val forsikring =
            runCatching {
                forsikringHenter.hentForsikringsinformasjon(spleisBehandlingId)
            }.getOrElse {
                return RestResponse.Error(ApiForsikringErrorCode.FEIL_VED_VIDERE_KALL)
            }

        return when (forsikring) {
            is ResultatAvForsikring.MottattForsikring ->
                RestResponse.OK(forsikring.forsikring.tilApiForsikring())

            is ResultatAvForsikring.IngenForsikring ->
                RestResponse.OK(ApiForsikring(false, null))
        }
    }

    override fun openApi(config: RouteConfig) {
        config.tags("Forsikringer")
    }
}

private fun Forsikring.tilApiForsikring(): ApiForsikring =
    ApiForsikring(
        eksisterer = true,
        forsikringInnhold =
            ForsikringInnhold(
                gjelderFraDag = gjelderFraDag,
                dekningsgrad = dekningsgrad,
            ),
    )

enum class ApiForsikringErrorCode(
    override val statusCode: HttpStatusCode,
    override val title: String,
) : ApiErrorCode {
    BEHANDLING_IKKE_FUNNET(HttpStatusCode.NotFound, "Fant ikke behandling"),
    MANGLER_TILGANG_TIL_PERSON(HttpStatusCode.Forbidden, "Mangler tilgang til person"),
    FEIL_VED_VIDERE_KALL(
        HttpStatusCode.InternalServerError,
        "Klarte ikke hente status fra Spiskammerset",
    ),
}
