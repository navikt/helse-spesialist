package no.nav.helse.spesialist.api.rest.behandlinger.notater

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiNotatRequest
import no.nav.helse.spesialist.api.rest.ApiOpprettetRessurs
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.harTilgangTilPerson
import no.nav.helse.spesialist.api.rest.resources.Behandlinger
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class PostBehandlingNotaterBehandler : PostBehandler<Behandlinger.BehandlingId.Notater, ApiNotatRequest, ApiOpprettetRessurs<Int>, ApiPostNotaterErrorCode> {
    override fun behandle(
        resource: Behandlinger.BehandlingId.Notater,
        request: ApiNotatRequest,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<ApiOpprettetRessurs<Int>, ApiPostNotaterErrorCode> {
        val behandling =
            transaksjon.behandlingRepository.finn(BehandlingUnikId(resource.parent.behandlingId))
                ?: return RestResponse.Error(ApiPostNotaterErrorCode.BEHANDLING_IKKE_FUNNET)

        val vedtaksperiode =
            transaksjon.vedtaksperiodeRepository.finn(behandling.vedtaksperiodeId)
                ?: error("Vedtaksperioden for behandlingen ble ikke funnet")

        if (!saksbehandler.harTilgangTilPerson(
                identitetsnummer = Identitetsnummer.fraString(vedtaksperiode.fødselsnummer),
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
            )
        ) {
            return RestResponse.Error(ApiPostNotaterErrorCode.MANGLER_TILGANG_TIL_PERSON)
        }

        val dialog = Dialog.Factory.ny()
        transaksjon.dialogRepository.lagre(dialog)

        val notat =
            Notat.Factory.ny(
                type = NotatType.Generelt,
                tekst = request.tekst,
                dialogRef = dialog.id(),
                vedtaksperiodeId = behandling.vedtaksperiodeId.value,
                saksbehandlerOid = saksbehandler.id,
            )
        transaksjon.notatRepository.lagre(notat)

        return RestResponse.Created(body = ApiOpprettetRessurs(notat.id().value))
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = listOf("Notater")
        }
    }
}

enum class ApiPostNotaterErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    BEHANDLING_IKKE_FUNNET("Behandlingen ble ikke funnet", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
