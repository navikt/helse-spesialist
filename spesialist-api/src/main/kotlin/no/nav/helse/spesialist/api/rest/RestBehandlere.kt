package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

interface RestBehandler {
    fun openApi(config: RouteConfig)

    val påkrevdTilgang: Tilgang
    val påkrevdeBrukerroller: Set<Brukerrolle> get() = emptySet()

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

interface RestBehandlerUtenBody<RESOURCE, RESPONSE, ERROR : ApiErrorCode> : RestBehandler {
    fun behandle(
        resource: RESOURCE,
        kallKontekst: KallKontekst,
    ): RestResponse<RESPONSE, ERROR>
}

interface RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE, ERROR : ApiErrorCode> : RestBehandler {
    fun behandle(
        resource: RESOURCE,
        request: REQUEST,
        kallKontekst: KallKontekst,
    ): RestResponse<RESPONSE, ERROR>
}

interface GetBehandler<RESOURCE, RESPONSE, ERROR : ApiErrorCode> : RestBehandlerUtenBody<RESOURCE, RESPONSE, ERROR>

interface DeleteBehandler<RESOURCE, RESPONSE, ERROR : ApiErrorCode> : RestBehandlerUtenBody<RESOURCE, RESPONSE, ERROR>

interface PatchBehandler<RESOURCE, REQUEST, RESPONSE, ERROR : ApiErrorCode> : RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE, ERROR>

interface PostBehandler<RESOURCE, REQUEST, RESPONSE, ERROR : ApiErrorCode> : RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE, ERROR>

interface PutBehandler<RESOURCE, REQUEST, RESPONSE, ERROR : ApiErrorCode> : RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE, ERROR>

abstract class ForPersonBehandlerUtenBody<RESOURCE, RESPONSE, ERROR : ApiErrorCode>(
    private val personPseudoId: (RESOURCE) -> Personer.PersonPseudoId,
    private val personIkkeFunnet: ERROR,
    private val manglerTilgangTilPerson: ERROR,
) : RestBehandlerUtenBody<RESOURCE, RESPONSE, ERROR> {
    abstract fun behandle(
        resource: RESOURCE,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<RESPONSE, ERROR>

    final override fun behandle(
        resource: RESOURCE,
        kallKontekst: KallKontekst,
    ): RestResponse<RESPONSE, ERROR> =
        kallKontekst.medPerson(
            personPseudoIdResource = personPseudoId(resource),
            personIkkeFunnet = personIkkeFunnet,
            manglerTilgangTilPerson = manglerTilgangTilPerson,
        ) { person -> behandle(resource, person, kallKontekst) }
}

abstract class ForPersonBehandlerMedBody<RESOURCE, REQUEST, RESPONSE, ERROR : ApiErrorCode>(
    private val personPseudoId: (RESOURCE) -> Personer.PersonPseudoId,
    private val personIkkeFunnet: ERROR,
    private val manglerTilgangTilPerson: ERROR,
) : RestBehandlerMedBody<RESOURCE, REQUEST, RESPONSE, ERROR> {
    abstract fun behandle(
        resource: RESOURCE,
        request: REQUEST,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<RESPONSE, ERROR>

    final override fun behandle(
        resource: RESOURCE,
        request: REQUEST,
        kallKontekst: KallKontekst,
    ): RestResponse<RESPONSE, ERROR> =
        kallKontekst.medPerson(
            personPseudoIdResource = personPseudoId(resource),
            personIkkeFunnet = personIkkeFunnet,
            manglerTilgangTilPerson = manglerTilgangTilPerson,
        ) { person -> behandle(resource, request, person, kallKontekst) }
}

abstract class GetForPersonBehandler<RESOURCE, RESPONSE, ERROR : ApiErrorCode>(
    personPseudoId: (RESOURCE) -> Personer.PersonPseudoId,
    personIkkeFunnet: ERROR,
    manglerTilgangTilPerson: ERROR,
) : ForPersonBehandlerUtenBody<RESOURCE, RESPONSE, ERROR>(
        personPseudoId = personPseudoId,
        personIkkeFunnet = personIkkeFunnet,
        manglerTilgangTilPerson = manglerTilgangTilPerson,
    ),
    GetBehandler<RESOURCE, RESPONSE, ERROR>

abstract class DeleteForPersonBehandler<RESOURCE, RESPONSE, ERROR : ApiErrorCode>(
    personPseudoId: (RESOURCE) -> Personer.PersonPseudoId,
    personIkkeFunnet: ERROR,
    manglerTilgangTilPerson: ERROR,
) : ForPersonBehandlerUtenBody<RESOURCE, RESPONSE, ERROR>(
        personPseudoId = personPseudoId,
        personIkkeFunnet = personIkkeFunnet,
        manglerTilgangTilPerson = manglerTilgangTilPerson,
    ),
    DeleteBehandler<RESOURCE, RESPONSE, ERROR>

abstract class PatchForPersonBehandler<RESOURCE, REQUEST, RESPONSE, ERROR : ApiErrorCode>(
    personPseudoId: (RESOURCE) -> Personer.PersonPseudoId,
    personIkkeFunnet: ERROR,
    manglerTilgangTilPerson: ERROR,
) : ForPersonBehandlerMedBody<RESOURCE, REQUEST, RESPONSE, ERROR>(
        personPseudoId = personPseudoId,
        personIkkeFunnet = personIkkeFunnet,
        manglerTilgangTilPerson = manglerTilgangTilPerson,
    ),
    PatchBehandler<RESOURCE, REQUEST, RESPONSE, ERROR>

abstract class PostForPersonBehandler<RESOURCE, REQUEST, RESPONSE, ERROR : ApiErrorCode>(
    personPseudoId: (RESOURCE) -> Personer.PersonPseudoId,
    personIkkeFunnet: ERROR,
    manglerTilgangTilPerson: ERROR,
) : ForPersonBehandlerMedBody<RESOURCE, REQUEST, RESPONSE, ERROR>(
        personPseudoId = personPseudoId,
        personIkkeFunnet = personIkkeFunnet,
        manglerTilgangTilPerson = manglerTilgangTilPerson,
    ),
    PostBehandler<RESOURCE, REQUEST, RESPONSE, ERROR>

abstract class PutForPersonBehandler<RESOURCE, REQUEST, RESPONSE, ERROR : ApiErrorCode>(
    personPseudoId: (RESOURCE) -> Personer.PersonPseudoId,
    personIkkeFunnet: ERROR,
    manglerTilgangTilPerson: ERROR,
) : ForPersonBehandlerMedBody<RESOURCE, REQUEST, RESPONSE, ERROR>(
        personPseudoId = personPseudoId,
        personIkkeFunnet = personIkkeFunnet,
        manglerTilgangTilPerson = manglerTilgangTilPerson,
    ),
    PutBehandler<RESOURCE, REQUEST, RESPONSE, ERROR>
