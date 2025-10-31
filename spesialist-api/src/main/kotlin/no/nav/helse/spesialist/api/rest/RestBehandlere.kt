package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
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

inline fun <reified REQUEST, reified RESPONSE, reified ERROR : ApiErrorCode> RestBehandler.openApiMedRequestBody(config: RouteConfig) {
    config.request {
        body<REQUEST>()
    }
    openApiUtenRequestBody<RESPONSE, ERROR>(config)
}

inline fun <reified RESPONSE, reified ERROR : ApiErrorCode> RestBehandler.openApiUtenRequestBody(config: RouteConfig) {
    config.operationId = operationIdBasertPåKlassenavn()
    config.response {
        val hasResponseBody = RESPONSE::class != Unit::class
        code(if (hasResponseBody) HttpStatusCode.OK else HttpStatusCode.NoContent) {
            description = "Vellykket svar"
            if (hasResponseBody) {
                body<RESPONSE>()
            }
        }
        default {
            description = "Svar ved feil"
            body<ApiHttpProblemDetails<ERROR>> {
                mediaTypes = setOf(io.ktor.http.ContentType.Application.ProblemJson)
            }
        }
    }
    openApi(config)
}

interface RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE, ERROR : ApiErrorCode> : RestBehandler {
    fun behandle(
        resource: RESOURCE,
        request: REQUEST,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<RESPONSE, ERROR>
}

interface PostBehandler<RESOURCE, REQUEST, RESPONSE, ERROR : ApiErrorCode> : RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE, ERROR>

interface PutBehandler<RESOURCE, REQUEST, RESPONSE, ERROR : ApiErrorCode> : RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE, ERROR>

interface PatchBehandler<RESOURCE, REQUEST, RESPONSE, ERROR : ApiErrorCode> : RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE, ERROR>

interface DeleteBehandler<RESOURCE, RESPONSE, ERROR : ApiErrorCode> : RestBehandler {
    fun behandle(
        resource: RESOURCE,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<RESPONSE, ERROR>
}

interface GetBehandler<RESOURCE, RESPONSE, ERROR : ApiErrorCode> : RestBehandler {
    fun behandle(
        resource: RESOURCE,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
    ): RestResponse<RESPONSE, ERROR>
}
