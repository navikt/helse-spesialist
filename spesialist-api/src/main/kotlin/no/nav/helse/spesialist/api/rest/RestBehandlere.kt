package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

interface RestBehandler {
    fun openApi(config: RouteConfig)

    fun operationIdBasertPåKlassenavn(): String =
        this::class
            .simpleName!!
            .removeSuffix("Behandler")
            .let { it[0].lowercase() + it.substring(1) }
}

interface RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE> : RestBehandler {
    fun behandle(
        resource: RESOURCE,
        request: REQUEST,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<RESPONSE>
}

interface PostBehandler<RESOURCE, REQUEST, RESPONSE> : RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE>

interface PutBehandler<RESOURCE, REQUEST, RESPONSE> : RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE>

interface PatchBehandler<RESOURCE, REQUEST, RESPONSE> : RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE>

interface DeleteBehandler<RESOURCE, RESPONSE> : RestBehandler {
    fun behandle(
        resource: RESOURCE,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<RESPONSE>
}

interface GetBehandler<RESOURCE, RESPONSE> : RestBehandler {
    fun behandle(
        resource: RESOURCE,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): RestResponse<RESPONSE>
}
